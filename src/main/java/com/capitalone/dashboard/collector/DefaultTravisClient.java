package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.BuildStatus;
import com.capitalone.dashboard.model.RepoBranch;
import com.capitalone.dashboard.model.SCM;
import com.capitalone.dashboard.model.TravisJob;
import com.capitalone.dashboard.model.TravisParsed;
import com.capitalone.dashboard.util.Encryption;
import com.capitalone.dashboard.util.EncryptionException;
import com.capitalone.dashboard.util.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.capitalone.dashboard.utils.Utilities.getLong;
import static com.capitalone.dashboard.utils.Utilities.getString;
@Component
public class DefaultTravisClient implements TravisClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTravisClient.class);
    private static final String BUILDS_BY_PROJECTID_REST_SUFFIX_BY_DATE = "/repo/%s/builds?branch.name=%s&event_type=push&include=build.commit&offset=%s&limit=%s";
    private static final String BUILD_JOB_URL = "builds/%s";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final int TRAVIS_BOARDS_PAGING = 50;

    private final RestOperations rest;
    private final TravisBuildSettings settings;
    private static JSONParser parser = new JSONParser();
    @Autowired
    public DefaultTravisClient( Supplier<RestOperations> restOperationsSupplier, TravisBuildSettings settings ) {
        this.rest = restOperationsSupplier.get();
        this.settings = settings;
    }
    @Override
    public List<Build> getBuilds( TravisJob job ) throws RestClientException, MalformedURLException, HygieiaException, URISyntaxException {
        List<Build> builds = new ArrayList<>();
        try {

            boolean isLast = false;
            int offset = 0;
            while(!isLast){
                TravisParsed travisParsed = new TravisParsed(job.getJobUrl());
                String url = String.format(travisParsed.getApiUrl() +
                        BUILDS_BY_PROJECTID_REST_SUFFIX_BY_DATE, travisParsed.getRepoSlug(), job.getRepoBranch(), offset, TRAVIS_BOARDS_PAGING);

                URI uri = new URI(url);

                String personalAccessToken = decryptString( job.getPersonalAccessToken(), settings.getKey() );
                ResponseEntity< String > responseEntity = makeRestCall( uri, personalAccessToken );
                JSONObject buildsJson = ( JSONObject ) parser.parse( responseEntity.getBody() );

                if ( buildsJson == null ) break;

                JSONArray buildsArray = ( JSONArray ) buildsJson.get( "builds" );
                if ( !CollectionUtils.isEmpty( buildsArray ) ) {
                    for ( Object obj : buildsArray ) {
                        JSONObject jo = ( JSONObject ) obj;
                        builds.add(objectToBuildJob( jo, job.getJobUrl() ) );
                    }
                }

                JSONObject pagination = ( JSONObject ) buildsJson.get( "@pagination" );
                offset = TRAVIS_BOARDS_PAGING + asInt( pagination, "offset" );
                String isLastPage = getString( pagination, "is_last" );
                int total = asInt( pagination, "count" );
                if( isLastPage.equals( "true" ) || builds.size() >= total) isLast = true;
            }


        } catch ( ParseException pe ) {
            LOGGER.error( "Parser exception when parsing builds", pe );
        }
        return builds;
    }

    /**
     *  Makes rest call for build date
     * @param url
     * @param decryptedPersonalAccessToken
     * @return response string
     */
    protected ResponseEntity<String> makeRestCall( URI url , String decryptedPersonalAccessToken ) {
        String token = settings.getToken();

        if( StringUtils.isNotEmpty( decryptedPersonalAccessToken ) ) {
            return rest.exchange(url, HttpMethod.GET,
                    new HttpEntity<>( createHeaders( decryptedPersonalAccessToken ) ),
                    String.class);
        } else if (StringUtils.isNotEmpty( token )) {
            return rest.exchange( url, HttpMethod.GET,
                    new HttpEntity<>( createHeaders( token ) ),
                    String.class );
        } else {
            return rest.exchange( url, HttpMethod.GET, null,
                    String.class );
        }
    }

    /**
     *  Takes a token to create headers for Travis API
     * @param token
     * @return
     */
    protected HttpHeaders createHeaders( final String token ) {
        String authHeader = "token " + token;

        HttpHeaders headers = new HttpHeaders();
        headers.set( HttpHeaders.AUTHORIZATION, authHeader );
        headers.set( "Travis-API-Version", "3" );
        return headers;
    }

    /**
     * Converts a build json object to a build Object
     * @param jo
     * @param buildUrl
     * @return returns build Object
     */
    protected Build objectToBuildJob( JSONObject jo, String buildUrl ){
        Build build = new Build();

        build.setBuildStatus( getBuildStatus( getString(jo, "state") ) );
        build.setBuildUrl( String.format( buildUrl + BUILD_JOB_URL, getLong(jo,"id") ) );

        build.setNumber( getString( jo, "number" ) );
        build.setStartTime( timestamp( jo, "started_at" ) );
        build.setEndTime( timestamp( jo, "finished_at" ) );
        //Duration from API is not reliable because issues happen if builds have been restarted. see travis docs
        long duration = timestamp( jo, "finished_at" ) - timestamp( jo, "started_at" );
        build.setDuration( duration );
        build.setTimestamp( System.currentTimeMillis() );

        addChangeSet( build, jo );
        return build;
    }

    /**
     *  Grabs changeset information for the given build.
     * @param build
     * @param changeSet
     */
    private void addChangeSet( Build build, JSONObject changeSet ){
        JSONObject commit = ( JSONObject ) changeSet.get( "commit" );
        SCM scm = new SCM();
        scm.setScmRevisionNumber( getString( commit, "sha" ) );
        scm.setScmCommitLog( getString( commit,"message" ) );
        scm.setScmCommitTimestamp( timestamp( commit, "committed_at" ));
        scm.setScmAuthor( getCommitAuthor( commit ) );
        scm.setNumberOfChanges( 1 );
        build.getSourceChangeSet().add( scm );

        RepoBranch rb = new RepoBranch();
        JSONObject branch = ( JSONObject ) changeSet.get( "branch" );
        rb.setBranch( getString( branch, "name" ) );
        rb.setUrl( parseRepoUrl( getString( commit, "compare_url") ) );
        rb.setType( RepoBranch.RepoType.GIT );
        build.getCodeRepos().add( rb );
    }

    /**
     * Gets timestamp from json object by key
     * @param json
     * @param key
     * @return returns time in milliseconds
     */
    private long timestamp( JSONObject json, String key ) {
        Object obj = json.get( key );
        if ( obj != null ) {
            try {
                return new SimpleDateFormat( DATE_FORMAT )
                        .parse( obj.toString() ).getTime();
            } catch ( java.text.ParseException e ) {
                LOGGER.warn( obj + " is not in expected format " + DATE_FORMAT + e );
            }
        }
        return 0;
    }

    /**
     * gets int from json object by key
     * @param json
     * @param key
     * @return int
     */
    private int asInt(JSONObject json, String key) {
        String val = getString(json, key);
        try {
            if (val != null) {
                return Integer.parseInt(val);
            }
        } catch (NumberFormatException ex) {
            LOGGER.error(ex.getMessage());
        }
        return 0;
    }

    /**
     * Parses Github repo URL
     * @param repoUrl
     * @return
     */
    private String parseRepoUrl( String repoUrl ) {
        try {
            String url;
            if ( repoUrl.endsWith( ".git" ) ) {
                url = repoUrl.substring( 0, repoUrl.lastIndexOf( ".git" ) );
            }else{
                url = repoUrl;
            }
            URL u = new URL( url );
            String host = u.getHost();
            String protocol = u.getProtocol();
            String path = u.getPath();
            String[] parts = path.split("/");
            if ( ( parts == null ) || ( parts.length < 3) ) {
                throw new HygieiaException( "Bad github repo URL: " + repoUrl, HygieiaException.BAD_DATA );
            }
            String orgName = parts[1];
            String repoName = parts[2];

            return protocol+ "://" +host+"/"+orgName+"/"+repoName;

        } catch ( MalformedURLException e ) {
            LOGGER.warn(repoUrl + " is not in expected format " + e);
        } catch ( HygieiaException e ){
            LOGGER.warn(e.toString());
        }
        return "";
    }

    /**
     * Pulls commit Author from json
     * @param jsonItem
     * @return returns commit Author
     */
    private String getCommitAuthor( JSONObject jsonItem ) {
        JSONObject author = ( JSONObject ) jsonItem.get( "author" );
        return author != null ? getString( author, "name" ) : "noreply";
    }

    /**
     * Converts Travis build status to Hygieia build status
     * @param status
     * @return
     */
    private BuildStatus getBuildStatus( String status ) {
        switch ( status ) {
            case "passed":
                return BuildStatus.Success;
            case "UNSTABLE":
                return BuildStatus.Unstable;
            case "failed":
                return BuildStatus.Failure;
            case "canceled":
                return BuildStatus.Aborted;
            default:
                return BuildStatus.Unknown;
        }
    }
    /**
     * Decrypt string
     * @param string
     * @param key
     * @return String
     */
    public static String decryptString( String string, String key ) {
        if ( !StringUtils.isEmpty( string ) ) {
            try {
                return Encryption.decryptString(
                        string, key );
            } catch (EncryptionException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return "";
    }
}

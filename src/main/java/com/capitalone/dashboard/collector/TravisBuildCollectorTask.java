package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.CollectionError;
import com.capitalone.dashboard.repository.TravisJobRepository;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.TravisBuildCollector;
import com.capitalone.dashboard.model.TravisJob;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.BuildRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TravisBuildCollectorTask extends CollectorTask < TravisBuildCollector > {
    private static final Logger LOGGER = LoggerFactory.getLogger(TravisBuildCollectorTask.class);
    private final BaseCollectorRepository<TravisBuildCollector> collectorRepository;
    private final BuildRepository buildRepository;
    private final TravisBuildSettings travisBuildSettings;
    private final TravisClient travisClient;
    private final TravisJobRepository travisJobRepository;

    @Autowired
    public TravisBuildCollectorTask( TaskScheduler taskScheduler,
                                     BaseCollectorRepository<TravisBuildCollector> collectorRepository,
                                     TravisBuildSettings travisBuildSettings,
                                     TravisClient travisClient,
                                     TravisJobRepository travisJobRepository,
                                     BuildRepository buildRepository) {

        super(taskScheduler, "TravisCi");
        this.collectorRepository    = collectorRepository;
        this.travisBuildSettings    = travisBuildSettings;
        this.travisClient           = travisClient;
        this.travisJobRepository    = travisJobRepository;
        this.buildRepository        = buildRepository;
    }

    @Override
    public TravisBuildCollector getCollector() {
        return TravisBuildCollector.prototype();
    }

    @Override
    public BaseCollectorRepository< TravisBuildCollector > getCollectorRepository() {
        return collectorRepository;
    }

    /**
     * Getting Cron Schedule from settings
     * @return
     */
    @Override
    public String getCron() {
        return travisBuildSettings.getCron();
    }

    @Override
    public void collect( TravisBuildCollector collector ) {
        //TODO: add errors to job
        proxySetup();
        String instanceUrl = travisBuildSettings.getHost();
        for (TravisJob job : enabledJobs(collector, instanceUrl)) {
            try{
                addNewBuilds(job, travisClient.getBuilds(job));
            }catch (RestClientException | URISyntaxException | MalformedURLException ex) {
                LOGGER.info("Error fetching Builds for:" + job.getJobUrl(), ex);
                 CollectionError error = new CollectionError(CollectionError.UNKNOWN_HOST, job.getJobUrl());
                 job.getErrors().add(error);
            } catch (HygieiaException he) {
                LOGGER.info("Error fetching Builds for:" + job.getJobUrl(), he);
                CollectionError error = new CollectionError("Bad repo url", job.getJobUrl());
                job.getErrors().add(error);
            }
            travisJobRepository.save(job);
        }

    }

    /**
     * Adds new builds to mongo
     * @param job
     * @param builds
     */
    private void addNewBuilds(TravisJob job, List<Build> builds){
        long start = System.currentTimeMillis();
        int count = 0;

        for(Build build : builds){
            if(isNewBuild(job, build)) {
                job.setLastUpdated(System.currentTimeMillis());
                travisJobRepository.save(job);
                if ( build != null ) {
                    build.setCollectorItemId(job.getId());
                    buildRepository.save(build);
                    count++;
                }
            }
        }
        log("New builds", start, count);
    }

    /**
     * Returns list of enabled Travis build collector Items
     * @param collector
     * @param instanceUrl
     * @return
     */
    protected List< TravisJob > enabledJobs( Collector collector, String instanceUrl ) {
        List<TravisJob> jobs = travisJobRepository.findEnabledJobs( collector.getId(), instanceUrl );
        List<TravisJob> pulledJobs
                = Optional.ofNullable( jobs )
                .orElseGet( Collections::emptyList ).stream()
                .filter( pulledJob -> !pulledJob.isPushed() && pulledJob.isEnabled() )
                .collect( Collectors.toList() );

        if ( CollectionUtils.isEmpty( pulledJobs ) ) { return new ArrayList<>(); }
        return pulledJobs;
    }

    /**
     * Defines proxy values
     */
    protected void proxySetup(){
        String proxyUrl         = travisBuildSettings.getProxy();
        String proxyPort        = travisBuildSettings.getProxyPort();
        String proxyUser        = travisBuildSettings.getProxyUser();
        String proxyPassword    = travisBuildSettings.getProxyPassword();

        if (!StringUtils.isEmpty(proxyUrl) && !StringUtils.isEmpty(proxyPort)) {
            System.setProperty("http.proxyHost", proxyUrl);
            System.setProperty("https.proxyHost", proxyUrl);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("https.proxyPort", proxyPort);

            if (!StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword)) {
                System.setProperty("http.proxyUser", proxyUser);
                System.setProperty("https.proxyUser", proxyUser);
                System.setProperty("http.proxyPassword", proxyPassword);
                System.setProperty("https.proxyPassword", proxyPassword);
            }
        }
    }

    /**
     * Checks to see if build exist in mongo already
     * @param job
     * @param build
     * @return
     */
    protected boolean isNewBuild(TravisJob job, Build build) {
        return buildRepository.findByCollectorItemIdAndNumber(job.getId(),
                build.getNumber()) == null;
    }
}

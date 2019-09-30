package com.capitalone.dashboard.model;

import com.capitalone.dashboard.misc.HygieiaException;

import java.net.MalformedURLException;
import java.net.URL;

public class TravisParsed {
    private String url;
    private String host;
    private String apiUrl;
    private String orgName;
    private String repoName;
    private String repoSlug;
    private static final String PUBLIC_TRAVIS_REPO_HOST = "api.travis-ci.com";
    private static final String PUBLIC_TRAVIS_HOST_NAME = "travis-ci.com";
    public TravisParsed(String url) throws MalformedURLException, HygieiaException {
        this.url = url;
        parse();
    }

    private void parse() throws MalformedURLException, HygieiaException {
        URL u = new URL(url);
        host = u.getHost();
        String protocol = u.getProtocol();
        String path = u.getPath();
        String[] parts = path.split("/");
        if ((parts == null) || (parts.length < 3)) {
            throw new HygieiaException("Bad Travis URL: " + url, HygieiaException.BAD_DATA);
        }
        orgName = parts[1];
        repoName = parts[2];
        repoSlug = orgName + "%2F" + repoName;
        if (host.startsWith(PUBLIC_TRAVIS_HOST_NAME)) {
            apiUrl = protocol + "://" + PUBLIC_TRAVIS_REPO_HOST;
        } else {
            apiUrl = protocol + "://" + host;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getRepoSlug() {
        return repoSlug;
    }

    public void setRepoSlug(String repoSlug) {
        this.repoSlug = repoSlug;
    }
}

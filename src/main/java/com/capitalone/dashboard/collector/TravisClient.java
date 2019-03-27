package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.TravisJob;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

public interface TravisClient {
    /**
     * Fetch full populated build information for a build.
     *
     * @param  job the the travis job object
     * @return a Build instance or null
     */
    List<Build> getBuilds(TravisJob job) throws MalformedURLException, HygieiaException, URISyntaxException;
}

package com.capitalone.dashboard.model;

import java.util.HashMap;
import java.util.Map;

public class TravisBuildCollector extends Collector {
    public static TravisBuildCollector prototype() {
        TravisBuildCollector protoType = new TravisBuildCollector();
        protoType.setName( "TravisCi" );
        protoType.setCollectorType( CollectorType.Build );
        protoType.setOnline( true );
        protoType.setEnabled( true );

        Map<String, Object> options = new HashMap<>();
        options.put(TravisJob.INSTANCE_URL,"");
        options.put(TravisJob.JOB_URL,"");
        options.put(TravisJob.JOB_NAME,"");
        options.put(TravisJob.REPO_BRANCH,"");
        options.put(TravisJob.PERSONAL_ACCESS_TOKEN,"");

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(TravisJob.JOB_URL,"");
        uniqueOptions.put(TravisJob.JOB_NAME,"");

        protoType.setAllFields(options);
        protoType.setUniqueFields(uniqueOptions);
        return protoType;
    }
}

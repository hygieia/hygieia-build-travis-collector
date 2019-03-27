package com.capitalone.dashboard.testUtils;

import com.capitalone.dashboard.repository.TravisJobRepository;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.TravisBuildCollector;
import com.capitalone.dashboard.model.TravisJob;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.testutil.GsonUtil;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

public class LoadTestData {
    public static void loadCollector(BaseCollectorRepository<TravisBuildCollector> collectorRepository) throws IOException {
        Gson gson = GsonUtil.getGson();
        String json = IOUtils.toString(Resources.getResource("./repository/collector.json"));
        TravisBuildCollector builds = gson.fromJson(json, new TypeToken<TravisBuildCollector>(){}.getType());
        collectorRepository.save(builds);
    }
    public static void loadJobRepository(TravisJobRepository travisJobRepository) throws IOException {
        Gson gson = GsonUtil.getGson();
        String json = IOUtils.toString(Resources.getResource("./repository/travisJobRepository.json"));
        List<TravisJob> jobs = gson.fromJson(json, new TypeToken<List<TravisJob>>(){}.getType());
        travisJobRepository.save(jobs);
    }
}

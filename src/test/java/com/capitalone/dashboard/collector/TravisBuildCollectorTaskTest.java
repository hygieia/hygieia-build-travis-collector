package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.TravisJobRepository;
import com.capitalone.dashboard.config.MongoServerConfig;
import com.capitalone.dashboard.config.TestConfig;
import com.capitalone.dashboard.model.TravisBuildCollector;
import com.capitalone.dashboard.model.TravisJob;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.testUtils.LoadTestData;
import com.capitalone.dashboard.util.Supplier;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class, MongoServerConfig.class})
@DirtiesContext
public class TravisBuildCollectorTaskTest {

    @Autowired
    private TravisBuildCollectorTask travisBuildCollectorTask;

    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private TravisJobRepository travisJobRepository;

    @Mock
    private Supplier<RestOperations> restOperationsSupplier = mock(Supplier.class);
    @Mock
    private RestOperations rest = mock(RestOperations.class);
    @Autowired
    private BaseCollectorRepository<TravisBuildCollector> collectorRepository;
    @Autowired
    private TravisBuildSettings travisBuildSettings;
    @Mock
    private DefaultTravisClient defaultTravisClient;

    private TravisBuildCollector travisBuildCollector;
    @Before
    public void loadTestData() throws IOException {
        LoadTestData.loadCollector(collectorRepository);
        LoadTestData.loadJobRepository(travisJobRepository);
        when(restOperationsSupplier.get()).thenReturn(rest);

        defaultTravisClient = new DefaultTravisClient( restOperationsSupplier, travisBuildSettings);
        travisBuildCollectorTask = new TravisBuildCollectorTask(
                null,
                collectorRepository,
                travisBuildSettings,
                defaultTravisClient,
                travisJobRepository,
                buildRepository);
        travisBuildCollector = travisBuildCollectorTask.getCollector();
        travisBuildCollector.setId(new ObjectId("5c943ab6cd6ae6641aa410d3"));
    }
    @Test
    public void shouldCollect() throws IOException {
        doReturn(new ResponseEntity<>(getExpectedJSON("response/buildResponse.json"),
               HttpStatus.OK)).when(rest).exchange(any(), eq(HttpMethod.GET), Matchers.any(HttpEntity.class), eq(String.class));
        travisBuildCollectorTask.collect(travisBuildCollector);
        Iterable builds = buildRepository.findAll();
        assertThat( Lists.newArrayList(builds).size()).isEqualTo(25);

    }
    @Test
    public void enabledJobs() {
        List< TravisJob > jobs = travisBuildCollectorTask.enabledJobs(travisBuildCollector, "travis-ci.com");
        assertThat(jobs.size()).isEqualTo(1);
    }
    @Test
    public void enabledJobsInvalidCollectorId() {
        List< TravisJob > jobs = travisBuildCollectorTask.enabledJobs(new Collector(), "travis-ci.com");
        assertThat(jobs.size()).isEqualTo(0);
    }
    private String getExpectedJSON(String fileName) throws IOException {
        String path = "./" + fileName;
        URL fileUrl = Resources.getResource(path);
        return IOUtils.toString(fileUrl);
    }
}

package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.TravisJob;
import com.capitalone.dashboard.util.Supplier;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTravisClientTest {
    @Mock private Supplier<RestOperations> restOperationsSupplier;
    @Mock private RestOperations rest;
    @Mock private DefaultTravisClient defaultTravisClient;

    private TravisBuildSettings settings;

    @Before
    public void init() {
        when(restOperationsSupplier.get()).thenReturn(rest);
        settings = new TravisBuildSettings();
        defaultTravisClient  = new DefaultTravisClient(restOperationsSupplier,
                settings);
    }
    @Test
    public void getBuildsTest() throws IOException {
        doReturn(new ResponseEntity<>(getExpectedJSON("response/buildResponse.json"),
                HttpStatus.OK)).when(rest).exchange(any(),
              eq(HttpMethod.GET), Matchers.any(HttpEntity.class), eq(String.class));
        try {
            TravisJob job = new TravisJob();
            job.setRepoBranch("master");
            job.setJobUrl("https://travis-ci.com/Hygieia/Hygieia/");
            job.setJobName("Hygieia/Hygieia");
            List< Build > list = defaultTravisClient.getBuilds(job);
            assertThat(list.size()).isGreaterThan(1);
        } catch (HygieiaException | URISyntaxException ex){

        }
    }
    private String getExpectedJSON(String fileName) throws IOException {
        String path = "./" + fileName;
        URL fileUrl = Resources.getResource(path);
        return IOUtils.toString(fileUrl);
    }
}

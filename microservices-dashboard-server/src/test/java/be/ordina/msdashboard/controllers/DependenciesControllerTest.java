package be.ordina.msdashboard.controllers;

import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.services.DependenciesResourceService;
import be.ordina.msdashboard.services.RedisService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DependenciesControllerTest {

    @InjectMocks
    private DependenciesController dependenciesController;

    @Mock
    private DependenciesResourceService dependenciesResourceService;

    @Mock
    private RedisService redisService;

    @Test
    public void getDependenciesGraphJson() {
        doReturn(Collections.emptyMap()).when(dependenciesResourceService).getDependenciesGraphResourceJson();

        HttpEntity<Map<String, Object>> httpEntity = dependenciesController.getDependenciesGraphJson();

        assertThat(httpEntity.getBody()).isEmpty();
    }

    @Test
    public void getDependenciesJson() {
        doReturn(new Node()).when(dependenciesResourceService).getDependenciesResourceJson();

        HttpEntity<Node> httpEntity = dependenciesController.getDependenciesJson();

        assertThat(httpEntity.getBody()).isNotNull();
    }

    @Test
    public void saveNode() {
        dependenciesController.saveNode("nodeAsJson");

        verify(redisService).saveNode("nodeAsJson");
    }

    @Test
    public void getAllNodes() {
        doReturn(Collections.emptyList()).when(redisService).getAllNodes();

        List<Node> nodes = dependenciesController.getAllNodes();

        assertThat(nodes).isEmpty();
    }

    @Test
    public void deleteNode() {
        dependenciesController.deleteNode("nodeId");

        verify(redisService).deleteNode("nodeId");
    }

    @Test
    public void deleteAllNodes() {
        dependenciesController.deleteAllNodes();

        verify(redisService).deleteAllNodes();
    }

    @Test
    public void flushAll() {
        dependenciesController.flushAll();

        verify(redisService).flushDB();
    }

    @Test
    public void evictCache() {
        dependenciesController.evictCache();

        verify(redisService).evictHealthsCache();
    }

}

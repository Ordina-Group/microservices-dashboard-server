package be.ordina.msdashboard.services;

import be.ordina.msdashboard.aggregator.DependenciesGraphResourceJsonBuilder;
import be.ordina.msdashboard.aggregator.DependenciesResourceJsonBuilder;
import be.ordina.msdashboard.model.Node;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DependenciesResourceServiceTest {

	@InjectMocks
	private DependenciesResourceService dependenciesResourceService;

	@Mock
	private DependenciesResourceJsonBuilder dependenciesResourceJsonBuilder;

	@Mock
	private DependenciesGraphResourceJsonBuilder dependenciesGraphResourceJsonBuilder;

	@Test
	public void getDependenciesResourceJson() {
		Node node = new Node();

		doReturn(node).when(dependenciesResourceJsonBuilder).build();

		Node returnedNode = dependenciesResourceService.getDependenciesResourceJson();

		verify(dependenciesResourceJsonBuilder).build();

		assertThat(returnedNode).isEqualTo(node);
	}

	@Test
	public void getDependenciesGraphResourceJson() {
		Map<String, Object> graph = Collections.emptyMap();

		doReturn(graph).when(dependenciesGraphResourceJsonBuilder).build();

		Map<String, Object> returnedGraph = dependenciesResourceService.getDependenciesGraphResourceJson();

		verify(dependenciesGraphResourceJsonBuilder).build();

		assertThat(returnedGraph).isEqualTo(graph);
	}
}
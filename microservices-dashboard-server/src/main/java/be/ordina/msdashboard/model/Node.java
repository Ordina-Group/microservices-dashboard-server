
package be.ordina.msdashboard.model;

import static be.ordina.msdashboard.constants.Constants.DETAILS;
import static be.ordina.msdashboard.constants.Constants.ID;
import static be.ordina.msdashboard.constants.Constants.LANE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node {
	@JsonProperty(ID)
	private String id;

	@JsonProperty(DETAILS)
	private Map<String, Object> details;

	@JsonProperty(LANE)
	private Integer lane;

	@JsonProperty("linkedNodes")
	private List<Node> linkedNodes;

	public void setLane(Integer lane) {
		this.lane = lane;
	}

	public Integer getLane() {
		return lane;
	}

	public void setId(java.lang.String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public List<Node> getLinkedNodes() {
		if(linkedNodes ==null){
			linkedNodes = new ArrayList<>();
		}
		return linkedNodes;
	}

	public void setLinkedNodes(List<Node> linkedNodes) {
		this.linkedNodes = linkedNodes;
	}

	public Map<String, Object> getDetails() {
		if(details==null){
			details = new HashMap<>();
		}
		return details;
	}

	public void setDetails(Map<String, Object> details) {
		this.details = details;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Node node = (Node) o;

		return id.equals(node.id);

	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Node{" +
				"details=" + details +
				", id='" + id + '\'' +
				", lane=" + lane +
				", linkedNodes=" + linkedNodes +
				'}';
	}
}
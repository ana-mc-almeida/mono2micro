package pt.ist.socialsoftware.mono2micro.operation.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.operation.Operation;

import java.util.stream.Collectors;

public class MergeOperation extends Operation {
	public static final String MERGE_OPERATION = "MergeOperation";
	protected String cluster1Name;
	protected String cluster2Name;
	protected String newName;
	/** Undo state, captured by {@link #execute}; read-only in JSON, so a request cannot set it. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String cluster1Entities;
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	protected String cluster2Entities;

	public MergeOperation() {}

	public MergeOperation(String cluster1Name, String cluster2Name, String newName) {
		this.cluster1Name = cluster1Name;
		this.cluster2Name = cluster2Name;
		this.newName = newName;
	}

	@Override
	public String getOperationType() {
		return MERGE_OPERATION;
	}

	@Override
	public void execute(Decomposition decomposition) {
		storeState(decomposition); // needed so that when doing undo, the original entities are restored
		super.execute(decomposition);
	}

	protected void storeState(Decomposition decomposition) {
		if (cluster1Entities == null) { // First time doing the operation sets up the original cluster entities
			Cluster cluster1 = decomposition.getCluster(this.cluster1Name);
			Cluster cluster2 = decomposition.getCluster(this.cluster2Name);
			cluster1Entities = cluster1.getElementsIDs().stream().map(Object::toString).collect(Collectors.joining(","));
			cluster2Entities = cluster2.getElementsIDs().stream().map(Object::toString).collect(Collectors.joining(","));
		}
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getCluster1Name() {
		return cluster1Name;
	}

	public void setCluster1Name(String cluster1Name) {
		this.cluster1Name = cluster1Name;
	}

	public String getCluster2Name() {
		return cluster2Name;
	}

	public void setCluster2Name(String cluster2Name) {
		this.cluster2Name = cluster2Name;
	}

	public String getCluster1Entities() {
		return cluster1Entities;
	}

	public void setCluster1Entities(String cluster1Entities) {
		this.cluster1Entities = cluster1Entities;
	}

	public String getCluster2Entities() {
		return cluster2Entities;
	}

	public void setCluster2Entities(String cluster2Entities) {
		this.cluster2Entities = cluster2Entities;
	}
}

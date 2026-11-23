package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition.DecompositionMetricCalculator;

import java.util.List;
import java.util.Set;

public abstract class RepresentationInformation {
	String decompositionId;

	public abstract String getType();

	public abstract void deleteProperties();

	public abstract void setup(ClusterRepresentation decomposition) throws Exception;

	public abstract void update(ClusterRepresentation decomposition) throws Exception;

	public abstract void snapshot(ClusterRepresentation snapshotDecomposition, ClusterRepresentation decomposition) throws Exception;

	public abstract List<DecompositionMetricCalculator> getDecompositionMetrics();

	public abstract List<String> getParameters();

	public void renameClusterInFunctionalities(String clusterName, String newName) {}

	public void removeFunctionalitiesWithEntityIDs(ClusterRepresentation decomposition, Set<Short> elements) {}

	public abstract String getEdgeWeights(ClusterRepresentation decomposition) throws Exception;

	public abstract String getSearchItems(ClusterRepresentation decomposition) throws Exception;

	public String getDecompositionId() {
		return decompositionId;
	}

	public void setDecompositionId(String decompositionId) {
		this.decompositionId = decompositionId;
	}
}

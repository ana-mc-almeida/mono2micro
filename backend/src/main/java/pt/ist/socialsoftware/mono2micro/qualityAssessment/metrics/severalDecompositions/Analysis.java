package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;

public abstract class Analysis {
	public abstract String getType();

	public abstract void analyse(ClusterRepresentation decomposition1, ClusterRepresentation decomposition2) throws Exception;
}

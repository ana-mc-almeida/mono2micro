package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.dto;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;

public class ComparisonToolResponseFactory {
	public static ComparisonToolResponse getComparisonToolResponse(
			ClusterRepresentation decomposition1,
			ClusterRepresentation decomposition2
	) throws Exception {
		switch (decomposition1.getType() + decomposition2.getType()) { // Compare two decompositions based on their type
		default:
			return new DefaultComparisonToolResponse(decomposition1, decomposition2);
		}
	}
}

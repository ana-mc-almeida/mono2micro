package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.dto;

import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.MoJoFM;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.Purity;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.ClusterRepresentationDtoFactory;

public class DefaultComparisonToolResponse extends ComparisonToolResponse {
	public DefaultComparisonToolResponse(ClusterRepresentation decomposition1, ClusterRepresentation decomposition2) throws Exception {
		this.decomposition1 = ClusterRepresentationDtoFactory.getDecompositionDto(decomposition1);
		this.decomposition2 = ClusterRepresentationDtoFactory.getDecompositionDto(decomposition2);

		this.addAnalysis(new MoJoFM(decomposition1, decomposition2));
		this.addAnalysis(new Purity(decomposition1, decomposition2));
	}
}

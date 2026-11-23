package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition.functionality;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.MetricCalculator;

public abstract class FunctionalityMetricCalculator extends MetricCalculator {
	public abstract Object calculateMetric(
			AccessesInformation accessesInformation,
			ClusterRepresentation decomposition,
			Functionality functionality
	) throws Exception;
}

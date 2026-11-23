package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition.functionalityRedesign;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.FunctionalityRedesign;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.MetricCalculator;

public abstract class FunctionalityRedesignMetricCalculator extends MetricCalculator {
	public abstract Object calculateMetric(
			ClusterRepresentation decomposition,
			AccessesInformation info,
			Functionality functionality,
			FunctionalityRedesign functionalityRedesign) throws Exception;
}

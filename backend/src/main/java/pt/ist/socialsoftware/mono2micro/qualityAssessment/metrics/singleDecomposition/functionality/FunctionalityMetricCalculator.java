package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition.functionality;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.MetricCalculator;

public abstract class FunctionalityMetricCalculator extends MetricCalculator {
	public abstract Object calculateMetric(AccessesInformation accessesInformation, Decomposition decomposition, Functionality functionality) throws Exception;
}

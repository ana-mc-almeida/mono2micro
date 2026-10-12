package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.MetricCalculator;

public abstract class DecompositionMetricCalculator extends MetricCalculator {
	public abstract Object calculateMetric(Decomposition decomposition);
}

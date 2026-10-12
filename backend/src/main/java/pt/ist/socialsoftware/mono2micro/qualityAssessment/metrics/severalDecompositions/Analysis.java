package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;

public abstract class Analysis {
	public abstract String getType();

	public abstract void analyse(Decomposition decomposition1, Decomposition decomposition2) throws Exception;
}

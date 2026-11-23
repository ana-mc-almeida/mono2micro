package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import pt.ist.socialsoftware.mono2micro.feature.FeatureFamily;

/**
 * The algorithm operations, as a family so the boot check finds every one claimed (D-025 point 7). No algorithm operation is ever present: it is
 * offered, never collected or given as input, so nothing requires one.
 */
public class AlgorithmOperationFamily extends FeatureFamily<AlgorithmOperation> {

	public AlgorithmOperationFamily() {
		super("algorithm operation", AlgorithmOperation.class);
	}
}

package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import pt.ist.socialsoftware.mono2micro.feature.FeatureFamily;

/**
 * The clustering algorithms, as a family so the boot check finds every algorithm claimed (D-025 point 7). No algorithm is ever present: an
 * algorithm is offered, never collected or given as input, so nothing requires one.
 */
public class ClusteringAlgorithmFamily extends FeatureFamily<Clustering> {

	public ClusteringAlgorithmFamily() {
		super("clustering algorithm", Clustering.class);
	}
}

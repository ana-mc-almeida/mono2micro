package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import static pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.SciPyClustering.SCIPY;

public class ClusteringFactory {
	public static Clustering getClustering(String type) {
		switch (type) {
		case SCIPY:
			return new SciPyClustering();
		default:
			throw new RuntimeException("Algorithm type " + type + "not recognized.");
		}
	}
}

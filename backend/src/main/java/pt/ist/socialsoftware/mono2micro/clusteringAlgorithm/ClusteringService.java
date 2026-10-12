package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.List;

@Service
public class ClusteringService {
	public List<String> getAlgorithmSupportedStrategyTypes(String algorithmType) {
		Clustering algorithm = SelfRegistration.prototypes(Clustering.class).get(algorithmType);
		if (algorithm == null)
			throw new RuntimeException("Algorithm type " + algorithmType + " not recognized.");
		return algorithm.getAlgorithmSupportedStrategyTypes();
	}
}

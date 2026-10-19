package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.request.DecompositionRequest;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Clustering implements VariableFeature {
	public abstract Decomposition generateDecomposition(Similarity similarity, DecompositionRequest request, Set<String> decompositionNames) throws Exception;

	public abstract String getType();

	/** The Similarity kinds this algorithm accepts as input, as the shared check derives them from its requirements. */
	public List<String> getAlgorithmSupportedStrategyTypes() {
		return SelfRegistration.prototypes(SimilarityKind.class).entrySet().stream()
				.filter(kind -> AvailabilityCheck.derive(Collections.singletonList(this), AvailabilityContext.forInput(kind.getValue()))
						.getFeatures().get(0).isAvailable())
				.map(kind -> kind.getKey())
				.collect(Collectors.toList());
	}
}

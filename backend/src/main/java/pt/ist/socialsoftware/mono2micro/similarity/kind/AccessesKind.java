package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;

import java.util.Collections;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY;

/** Needs only what every kind needs, which {@link SimilarityKindFamily} shares. */
@JsonTypeName(AccessesKind.ACCESSES)
public class AccessesKind extends SimilarityKind {
	public static final String ACCESSES = "Accesses";

	@Override
	public String similarityType() {
		return SIMILARITY_SCIPY_ACCESSES_REPOSITORY;
	}

	@Override
	public List<RepresentationInformation> informationBlocks() {
		return Collections.singletonList(new AccessesInformation());
	}

	@Override
	public List<String> weightsTypes() {
		return Collections.singletonList(AccessesWeights.ACCESSES_WEIGHTS);
	}

	@Override
	public String scopedRepresentationType() {
		return AccessesRepresentation.ACCESSES;
	}
}

package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.FunctionalityVectorizationSequenceOfAccessesWeights;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses.SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES;

@JsonTypeName(FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES)
public class FunctionalityVectorizationSequenceOfAccessesKind extends SimilarityKind {
	public static final String FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES = "Functionality Vectorization Sequence Accesses";

	@Override
	public List<Requirement> requirements() {
		// FIXME (ticket 19): the four vectorization kinds repeat this pair of requirements; declare it once
		return Arrays.asList(
				Requires.of(EntityToIDRepresentation.class),
				Requires.of(CodeEmbeddingsRepresentation.class));
	}

	@Override
	public String similarityType() {
		return SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES;
	}

	@Override
	public List<RepresentationInformation> informationBlocks() {
		return Collections.singletonList(new AccessesInformation());
	}

	@Override
	public List<String> weightsTypes() {
		return Collections.singletonList(FunctionalityVectorizationSequenceOfAccessesWeights.FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS);
	}

	@Override
	public boolean requiresClusterTypeConversion() {
		return true;
	}
}

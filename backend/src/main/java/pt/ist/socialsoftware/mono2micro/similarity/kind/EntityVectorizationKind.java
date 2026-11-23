package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyEntityVectorization.SIMILARITY_SCIPY_ENTITY_VECTORIZATION;

@JsonTypeName(EntityVectorizationKind.ENTITY_VECTORIZATION)
public class EntityVectorizationKind extends SimilarityKind {
	public static final String ENTITY_VECTORIZATION = "Entity Vectorization";

	@Override
	public List<Requirement> requirements() {
		// FIXME (ticket 19): the four vectorization kinds repeat this pair of requirements; declare it once
		return Arrays.asList(
				Requires.of(EntityToIDRepresentation.class),
				Requires.of(CodeEmbeddingsRepresentation.class));
	}

	@Override
	public String similarityType() {
		return SIMILARITY_SCIPY_ENTITY_VECTORIZATION;
	}

	@Override
	public List<RepresentationInformation> informationBlocks() {
		return Collections.singletonList(new AccessesInformation());
	}

	@Override
	public List<Weights> defaultWeights() {
		return Collections.emptyList();
	}
}

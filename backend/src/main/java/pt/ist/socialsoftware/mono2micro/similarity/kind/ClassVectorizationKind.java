package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.HasRepresentation;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.domain.CodeEmbeddingsRepresentation.CODE_EMBEDDINGS;
import static pt.ist.socialsoftware.mono2micro.representation.domain.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.CLASS_VECTORIZATION_STRATEGY;

@JsonTypeName(CLASS_VECTORIZATION_STRATEGY)
public class ClassVectorizationKind extends SimilarityKind {

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				new HasRepresentation(ID_TO_ENTITY),
				new HasRepresentation(ENTITY_TO_ID),
				new HasRepresentation(ACCESSES),
				new HasRepresentation(CODE_EMBEDDINGS));
	}
}

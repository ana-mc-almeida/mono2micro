package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY;

@JsonTypeName(FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY)
public class FunctionalityVectorizationSequenceOfAccessesKind extends SimilarityKind {

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				Requires.of(EntityToIDRepresentation.class),
				Requires.of(CodeEmbeddingsRepresentation.class));
	}
}

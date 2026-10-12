package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.representation.HasRepresentation;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.ACCESSES_STRATEGY;

@JsonTypeName(ACCESSES_STRATEGY)
public class AccessesKind extends SimilarityKind {

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				new HasRepresentation(ID_TO_ENTITY),
				new HasRepresentation(ACCESSES));
	}
}

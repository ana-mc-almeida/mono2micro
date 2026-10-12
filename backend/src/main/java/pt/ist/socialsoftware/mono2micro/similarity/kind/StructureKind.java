package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.representation.HasRepresentation;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.domain.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.representation.domain.StructureRepresentation.STRUCTURE;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.STRUCTURE_STRATEGY;

/**
 * Declares Entity-to-ID, which {@code SimilarityScipyStructure.fillElements} reads, although the
 * strategy map this replaces left it out (D-019).
 */
@JsonTypeName(STRUCTURE_STRATEGY)
public class StructureKind extends SimilarityKind {

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				new HasRepresentation(ID_TO_ENTITY),
				new HasRepresentation(ENTITY_TO_ID),
				new HasRepresentation(ACCESSES),
				new HasRepresentation(STRUCTURE));
	}
}

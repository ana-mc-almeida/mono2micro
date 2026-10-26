package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;

import java.util.Arrays;
import java.util.List;

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
				Requires.of(EntityToIDRepresentation.class),
				Requires.of(StructureRepresentation.class));
	}
}

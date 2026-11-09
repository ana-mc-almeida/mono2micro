package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.StructureInformation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.StructureWeights;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure.SIMILARITY_SCIPY_STRUCTURE;

/**
 * Declares Entity-to-ID, which {@code SimilarityScipyStructure.fillElements} reads, although the
 * strategy map this replaces left it out (D-019).
 */
@JsonTypeName(StructureKind.STRUCTURE)
public class StructureKind extends SimilarityKind {
	public static final String STRUCTURE = "Structure";

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				Requires.of(EntityToIDRepresentation.class),
				Requires.of(StructureRepresentation.class));
	}

	@Override
	public String similarityType() {
		return SIMILARITY_SCIPY_STRUCTURE;
	}

	@Override
	public List<RepresentationInformation> informationBlocks() {
		return Arrays.asList(new AccessesInformation(), new StructureInformation());
	}

	@Override
	public List<String> weightsTypes() {
		return Collections.singletonList(StructureWeights.STRUCTURE_WEIGHTS);
	}

	@Override
	public String scopedRepresentationType() {
		return StructureRepresentation.STRUCTURE;
	}
}

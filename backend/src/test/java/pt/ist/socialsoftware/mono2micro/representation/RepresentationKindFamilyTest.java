package pt.ist.socialsoftware.mono2micro.representation;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The representation kinds are a family (D-025). Each kind has an id but is not a variable feature, and a kind is present when the software
 * system collected a representation of it.
 */
public class RepresentationKindFamilyTest {

	private final FeatureFamilies families = FeatureFamilies.scanned();

	@Test
	public void everyRepresentationKindIsAFeatureOfTheFamily() {
		assertThat(families.familyWithMemberType(Representation.class)).hasValueSatisfying(family -> assertThat(family.features())
				.extracting(FeatureId::getName)
				.containsExactlyInAnyOrder(
						IDToEntityRepresentation.ID_TO_ENTITY,
						EntityToIDRepresentation.ENTITY_TO_ID,
						AccessesRepresentation.ACCESSES,
						AuthorRepresentation.AUTHOR,
						CommitRepresentation.COMMIT,
						CodeEmbeddingsRepresentation.CODE_EMBEDDINGS,
						StructureRepresentation.STRUCTURE));
	}

	@Test
	public void aKindIsPresentExactlyWhenTheSystemCollectedIt() {
		AvailabilityContext context = families.contextFor(Arrays.asList(new IDToEntityRepresentation(), new AccessesRepresentation()));

		assertThat(context.isPresent(AccessesRepresentation.class)).isTrue();
		assertThat(context.isPresent(IDToEntityRepresentation.class)).isTrue();
		assertThat(context.isPresent(StructureRepresentation.class)).isFalse();
	}

	@Test
	public void aSystemWithSomeRepresentationHasTheFamily() {
		assertThat(families.contextFor(Collections.singletonList(new CommitRepresentation())).isPresent(Representation.class)).isTrue();
		assertThat(families.contextFor(Collections.emptyList()).isPresent(Representation.class)).isFalse();
	}
}

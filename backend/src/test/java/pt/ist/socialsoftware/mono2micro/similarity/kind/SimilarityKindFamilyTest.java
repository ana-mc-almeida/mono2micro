package pt.ist.socialsoftware.mono2micro.similarity.kind;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;

import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The 8 Similarity kinds are one family (D-025, D-026). No kind is present in a software system; for an input, the family answers with the input's
 * kind: a Similarity's, or the kind itself.
 */
public class SimilarityKindFamilyTest {

	private final FeatureFamilies families = FeatureFamilies.scanned();
	private final AvailabilityContext nothingCollected = families.contextFor(Collections.emptyList());

	@Test
	public void everySimilarityKindIsAFeatureOfTheFamily() {
		assertThat(families.familyWithMemberType(SimilarityKind.class)).hasValueSatisfying(family -> assertThat(family.features())
				.extracting(FeatureId::getName)
				.containsExactlyInAnyOrder(
						AccessesKind.ACCESSES,
						RepositoryKind.REPOSITORY,
						StructureKind.STRUCTURE,
						ClassVectorizationKind.CLASS_VECTORIZATION,
						EntityVectorizationKind.ENTITY_VECTORIZATION,
						FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH,
						FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES,
						// Accesses and Repository is its own kind beside Repository alone (D-026)
						AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY));
	}

	/** An algorithm operation's input is a Similarity, whose kind is read from its kind name. */
	@Test
	public void aSimilarityInputMakesItsKindPresentAndNoOther() {
		Similarity similarity = mock(Similarity.class);
		when(similarity.getKindName()).thenReturn(StructureKind.STRUCTURE);

		AvailabilityContext context = nothingCollected.withInput(similarity);

		assertThat(context.isPresent(StructureKind.class)).isTrue();
		assertThat(context.isPresent(AccessesKind.class)).isFalse();
		assertThat(context.isPresent(SimilarityKind.class)).isTrue();
	}

	/** A profile's input is the kind it would scope, before any Similarity of it exists. */
	@Test
	public void theInputsKindIsPresentAndNoOther() {
		AvailabilityContext context = nothingCollected.withInput(new StructureKind());

		assertThat(context.isPresent(StructureKind.class)).isTrue();
		assertThat(context.isPresent(AccessesKind.class)).isFalse();
		assertThat(context.isPresent(SimilarityKind.class)).isTrue();
	}

	/** The shared requirements are named with the representation family's label, which keeps them apart from the Accesses kind. */
	@Test
	public void aKindOnASystemWithNothingCollectedFailsWhatTheFamilyShares() {
		DerivationRecord record = AvailabilityCheck.derive(Collections.singletonList(new AccessesKind()), nothingCollected);

		assertThat(record.getFeatures().get(0).getFailedRequirements())
				.containsExactly("requires an IDToEntity representation", "requires an Accesses representation");
	}

	@Test
	public void withoutAnInputNoKindIsPresent() {
		assertThat(nothingCollected.isPresent(SimilarityKind.class)).isFalse();
	}
}

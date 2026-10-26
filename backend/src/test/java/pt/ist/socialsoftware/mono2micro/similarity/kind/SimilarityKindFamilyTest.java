package pt.ist.socialsoftware.mono2micro.similarity.kind;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.ACCESSES_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.CLASS_VECTORIZATION_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.ENTITY_VECTORIZATION_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.REPOSITORY_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.STRUCTURE_STRATEGY;

/**
 * The 7 Similarity kinds are one family (D-025). No kind is present in a software system; for an input, the family answers with the input's
 * kind.
 */
public class SimilarityKindFamilyTest {

	private final FeatureFamilies families = FeatureFamilies.scanned();
	private final AvailabilityContext nothingCollected = families.contextFor(Collections.emptyList());

	@Test
	public void everySimilarityKindIsAFeatureOfTheFamily() {
		assertThat(families.familyWithMemberType(SimilarityKind.class)).hasValueSatisfying(family -> assertThat(family.features())
				.extracting(FeatureId::getName)
				.containsExactlyInAnyOrder(
						ACCESSES_STRATEGY,
						REPOSITORY_STRATEGY,
						STRUCTURE_STRATEGY,
						CLASS_VECTORIZATION_STRATEGY,
						ENTITY_VECTORIZATION_STRATEGY,
						FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY,
						FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY));
	}

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

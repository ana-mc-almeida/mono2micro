package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.ClusteringFactory;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

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
 * One test per family scan: every feature the tool has appears in its family's derivation record,
 * withheld or not, so a feature that fails to register is caught here rather than silently never
 * offered.
 */
public class FamilyScanTest {

	private static final AvailabilityContext NOTHING_COLLECTED = AvailabilityContext.ofRepresentationKinds(Collections.emptySet());

	@Test
	public void everySimilarityKindAppearsInTheRecord() {
		DerivationRecord record = AvailabilityCheck.derive(
				SelfRegistration.prototypes(SimilarityKind.class).values(), NOTHING_COLLECTED);

		assertThat(record.getFeatures()).extracting(FeatureAvailability::getName).containsExactlyInAnyOrder(
				ACCESSES_STRATEGY,
				REPOSITORY_STRATEGY,
				STRUCTURE_STRATEGY,
				CLASS_VECTORIZATION_STRATEGY,
				ENTITY_VECTORIZATION_STRATEGY,
				FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY,
				FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY);
	}

	@Test
	public void everyAlgorithmAppearsInTheRecord() {
		DerivationRecord record = AvailabilityCheck.derive(
				SelfRegistration.prototypes(Clustering.class).values(), NOTHING_COLLECTED);

		assertThat(record.getFeatures()).extracting(FeatureAvailability::getName)
				.containsExactlyInAnyOrderElementsOf(ClusteringFactory.algorithmTypes);
	}

	/** Without an input there is no Similarity kind to accept, and the record says which requirement failed. */
	@Test
	public void anAlgorithmWithoutAnInputIsWithheldNamingTheKindsItAccepts() {
		DerivationRecord record = AvailabilityCheck.derive(
				SelfRegistration.prototypes(Clustering.class).values(), NOTHING_COLLECTED);

		assertThat(record.getFeatures()).allSatisfy(algorithm -> {
			assertThat(algorithm.isAvailable()).isFalse();
			assertThat(algorithm.getFailedRequirements()).hasSize(1);
			assertThat(algorithm.getFailedRequirements().get(0)).startsWith("Similarity kind in {");
		});
	}
}

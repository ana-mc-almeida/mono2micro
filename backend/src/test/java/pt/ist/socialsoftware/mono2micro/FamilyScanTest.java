package pt.ist.socialsoftware.mono2micro;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.ClusteringFactory;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.SciPyClustering;
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

	private static final AvailabilityContext NOTHING_COLLECTED = FeatureFamilies.scanned().contextFor(Collections.emptyList());

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

	/**
	 * Without an input there is no Similarity kind to accept, and the record names the family SciPy requires, not a list of kinds, so an 8th
	 * kind does not edit the algorithm (D-025).
	 */
	@Test
	public void sciPyWithoutAnInputIsWithheldNamingTheSimilarityKindFamily() {
		DerivationRecord record = AvailabilityCheck.derive(
				Collections.singletonList(SelfRegistration.prototypes(Clustering.class).get(SciPyClustering.SCIPY)), NOTHING_COLLECTED);

		FeatureAvailability sciPy = record.getFeatures().get(0);
		assertThat(sciPy.isAvailable()).isFalse();
		assertThat(sciPy.getFailedRequirements()).containsExactly("requires a Similarity kind as input");
	}

	@Test
	public void sciPyAcceptsEveryKindTheFamilyFinds() {
		assertThat(SelfRegistration.prototypes(Clustering.class).get(SciPyClustering.SCIPY).getAlgorithmSupportedStrategyTypes())
				.containsExactlyElementsOf(SelfRegistration.prototypes(SimilarityKind.class).keySet());
	}
}

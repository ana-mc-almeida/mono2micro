package pt.ist.socialsoftware.mono2micro;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.AlgorithmOperation;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms.SciPyOperation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.ExpertKnowledge;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesAndRepositoryKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.RepositoryKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.StructureKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.ClassVectorizationKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.EntityVectorizationKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.FunctionalityVectorizationCallGraphKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.FunctionalityVectorizationSequenceOfAccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatCode;

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
				AccessesKind.ACCESSES,
				RepositoryKind.REPOSITORY,
				StructureKind.STRUCTURE,
				ClassVectorizationKind.CLASS_VECTORIZATION,
				EntityVectorizationKind.ENTITY_VECTORIZATION,
				FunctionalityVectorizationCallGraphKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH,
				FunctionalityVectorizationSequenceOfAccessesKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES,
				// Accesses and Repository is its own kind beside Repository alone (D-026)
				AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY);
	}

	@Test
	public void everyAlgorithmOperationAppearsInTheRecord() {
		DerivationRecord record = AvailabilityCheck.derive(
				SelfRegistration.prototypes(AlgorithmOperation.class).values(), NOTHING_COLLECTED);

		assertThat(record.getFeatures()).extracting(FeatureAvailability::getName)
				.containsExactly(SciPyOperation.SCIPY);
	}

	/**
	 * Without an input there is no Similarity kind to accept, and the record names the family SciPy requires, not a list of kinds, so an 8th
	 * kind does not edit the algorithm (D-025).
	 */
	@Test
	public void sciPyWithoutAnInputIsWithheldNamingTheSimilarityKindFamily() {
		DerivationRecord record = AvailabilityCheck.derive(
				Collections.singletonList(SelfRegistration.prototypes(AlgorithmOperation.class).get(SciPyOperation.SCIPY)), NOTHING_COLLECTED);

		FeatureAvailability sciPy = record.getFeatures().get(0);
		assertThat(sciPy.isAvailable()).isFalse();
		assertThat(sciPy.getFailedRequirements()).containsExactly("requires a Similarity kind as input");
	}

	/** A Similarity of every kind the family finds is an input the SciPy operation accepts, as the check derives it from its kind name. */
	@Test
	public void sciPyAcceptsASimilarityOfEveryKindTheFamilyFinds() {
		AlgorithmOperation sciPy = SelfRegistration.prototypes(AlgorithmOperation.class).get(SciPyOperation.SCIPY);

		for (String kindName : SelfRegistration.prototypes(SimilarityKind.class).keySet())
			assertThat(AvailabilityCheck.derive(Collections.singletonList(sciPy), NOTHING_COLLECTED.withInput(similarityOf(kindName))).availableNames())
					.as(kindName).containsExactly(SciPyOperation.SCIPY);
	}

	private static Similarity similarityOf(String kindName) {
		Similarity similarity = mock(Similarity.class);
		when(similarity.getKindName()).thenReturn(kindName);
		return similarity;
	}

	/**
	 * The expert knowledge family finds the profile. Without an input there is no kind to scope, so the record names the kinds a profile can
	 * scope, all of them alternatives (D-017).
	 */
	@Test
	public void theProfileAppearsInTheExpertKnowledgeRecordWithheldWithoutAKind() {
		DerivationRecord record = AvailabilityCheck.derive(SelfRegistration.prototypes(ExpertKnowledge.class).values(), NOTHING_COLLECTED);

		assertThat(record.getFeatures()).extracting(FeatureAvailability::getName).containsExactly(Profile.PROFILE);
		assertThat(record.getFeatures().get(0).getFailedRequirements()).containsExactly("requires any of {an " + AccessesKind.ACCESSES
				+ " Similarity kind as input, an " + AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY + " Similarity kind as input, a "
				+ RepositoryKind.REPOSITORY + " Similarity kind as input, a " + StructureKind.STRUCTURE + " Similarity kind as input}");
	}

	/** What the boot runs: every family, every variable feature on the classpath, and every requirement add up (D-025 point 7). */
	@Test
	public void theToolsFamiliesPassTheBootCheck() {
		assertThatCode(() -> FeatureFamilies.scanned().verify(SelfRegistration.types(VariableFeature.class))).doesNotThrowAnyException();
	}
}

package pt.ist.socialsoftware.mono2micro.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

/**
 * What saving a recommendation's candidates does today, for the kinds the tool offers but cannot save.
 *
 * <p><b>Known defect, inherited from upstream: a vectorization recommendation's candidates cannot be saved.</b>
 * The tool offers recommendations of the four vectorization kinds and computes their candidates
 * ({@code requiresClusterTypeConversion} exists for them), but saving a candidate asks
 * {@code getSimilarityInformationDto} for the Similarity to attach it to, and only the Structure and the Accesses and Repository types
 * can describe one. Upstream ({@code ../mono2micro} at {@code 570b61d9}, {@code RecommendMatrixSciPy.java:241-249}) refused the same
 * types, by a switch on the recommendation type; ticket 09 moved the test to kind names and kept the defect.
 *
 * <p>The tests whose display name starts with "today" pin the defect. The {@code @Disabled} twin asserts
 * the fix: Phase 2 enables it and deletes the "today" test. See {@code ../implementation/docs/gap-analysis.md},
 * Gap 9, and {@code docs/decisions.md}, D-027.
 */
@DisplayName("Recommendation candidates, as saved today")
class RecommendationCharacterizationTest {

	private static final String VECTORIZATION_DEFECT = "Known defect, inherited from upstream: a vectorization "
			+ "recommendation's candidates cannot be saved. Phase 2 fixes it. See ../implementation/docs/gap-analysis.md, Gap 9";

	/** Pins the known defect; see the class javadoc. */
	@ParameterizedTest(name = "today, a {0} candidate has no Similarity to be saved with")
	@ValueSource(strings = { "Class Vectorization", "Entity Vectorization", "Functionality Vectorization Call Graph",
			"Functionality Vectorization Sequence Accesses" })
	void vectorizationCandidateHasNoSimilarity(String kindName) {
		RecommendMatrixSciPy recommendation = recommendation(kindName);

		assertThatThrownBy(() -> recommendation.getSimilarityInformationDto(Collections.emptyList()))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("A " + kindName + " recommendation's candidates cannot be saved (Gap 9, D-027).");
	}

	@Disabled(VECTORIZATION_DEFECT)
	@ParameterizedTest(name = "a {0} candidate is saved with a Similarity")
	@ValueSource(strings = { "Class Vectorization", "Entity Vectorization", "Functionality Vectorization Call Graph",
			"Functionality Vectorization Sequence Accesses" })
	void vectorizationCandidateHasASimilarity(String kindName) {
		RecommendMatrixSciPy recommendation = recommendation(kindName);

		assertThat(recommendation.getSimilarityInformationDto(Collections.emptyList())).isNotNull();
	}

	/** The kinds whose candidates can be saved: each asks for a Similarity of its own kind, of the type that computes it. */
	@ParameterizedTest(name = "a {0} candidate is saved with a Similarity of its kind")
	@ValueSource(strings = { "Accesses", "Repository", "Accesses and Repository", "Structure" })
	void candidateAsksForASimilarityOfItsKind(String kindName) {
		RecommendMatrixSciPy recommendation = recommendation(kindName);

		SimilarityDto candidate = recommendation.getSimilarityInformationDto(Collections.emptyList());

		assertThat(candidate.getType()).isEqualTo(SimilarityKind.getByName(kindName).similarityType())
				.isIn(SimilarityScipyStructure.SIMILARITY_SCIPY_STRUCTURE, SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY);
		assertThat(candidate.getKindName()).isEqualTo(kindName);
		assertThat(candidate.getRepresentationIds()).isEqualTo(recommendation.getRepresentationIds());
	}

	private static RecommendMatrixSciPy recommendation(String kindName) {
		RecommendMatrixSciPy recommendation = new RecommendMatrixSciPy();
		recommendation.setKindName(kindName);
		recommendation.setSoftwareSystem(new SoftwareSystem("system"));
		recommendation.setRepresentations(Arrays.asList(new IDToEntityRepresentation(), new AccessesRepresentation()));
		return recommendation;
	}
}

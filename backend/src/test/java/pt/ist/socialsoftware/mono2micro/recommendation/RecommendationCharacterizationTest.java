package pt.ist.socialsoftware.mono2micro.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_CLASS_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_ENTITY_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_CALLGRAPH;
import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES;

import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;

/**
 * What saving a recommendation's candidates does today, for the kinds the tool offers but cannot save.
 *
 * <p><b>Known defect, inherited from upstream: a vectorization recommendation's candidates cannot be saved.</b>
 * The tool offers the four vectorization recommendation types and computes their candidates
 * ({@code requiresClusterTypeConversion} exists for them), but saving a candidate asks
 * {@code getSimilarityInformationDto} for the Similarity to attach it to, and that method only knows
 * the Structure and the Accesses and Repository types. For every other type it throws. Upstream
 * ({@code ../mono2micro} at {@code 570b61d9}, {@code RecommendMatrixSciPy.java:241-249}) has the same switch.
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
	@ValueSource(strings = { RECOMMEND_MATRIX_CLASS_VECTORIZATION, RECOMMEND_MATRIX_ENTITY_VECTORIZATION,
			RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_CALLGRAPH, RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES })
	void vectorizationCandidateHasNoSimilarity(String type) {
		RecommendMatrixSciPy recommendation = recommendation(type);

		assertThatThrownBy(() -> recommendation.getSimilarityInformationDto(type, Collections.emptyList()))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("The type \"" + type + "\" is not a valid recommendation type.");
	}

	@Disabled(VECTORIZATION_DEFECT)
	@ParameterizedTest(name = "a {0} candidate is saved with a Similarity")
	@ValueSource(strings = { RECOMMEND_MATRIX_CLASS_VECTORIZATION, RECOMMEND_MATRIX_ENTITY_VECTORIZATION,
			RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_CALLGRAPH, RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES })
	void vectorizationCandidateHasASimilarity(String type) {
		RecommendMatrixSciPy recommendation = recommendation(type);

		assertThat(recommendation.getSimilarityInformationDto(type, Collections.emptyList())).isNotNull();
	}

	private static RecommendMatrixSciPy recommendation(String type) {
		RecommendMatrixSciPy recommendation = new RecommendMatrixSciPy();
		recommendation.type = type;
		return recommendation;
	}
}

package pt.ist.socialsoftware.mono2micro.recommendation.domain;

import pt.ist.socialsoftware.mono2micro.recommendation.dto.*;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.*;

public class RecommendationFactory {
	public static Recommendation getRecommendation(RecommendationDto recommendationDto) {
		switch (recommendationDto.getType()) {
		case RECOMMEND_MATRIX_SCIPY_STRUCTURE:
		case RECOMMEND_MATRIX_SCIPY:
		case RECOMMEND_MATRIX_CLASS_VECTORIZATION:
		case RECOMMEND_MATRIX_ENTITY_VECTORIZATION:
		case RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_CALLGRAPH:
		case RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES:
			return new RecommendMatrixSciPy((RecommendMatrixSciPyDto) recommendationDto);
		default:
			throw new RuntimeException("The type \"" + recommendationDto.getType() + "\" is not a valid decomposition type for recommendation creation.");
		}
	}

	public static Recommendation getRecommendation(Strategy strategy, List<Recommendation> strategyRecommendations, RecommendationDto recommendationDto) {
		Recommendation recommendation = getRecommendation(recommendationDto);
		recommendation.setCompleted(false);

		// Get new name
		Set<String> recommendationNames = strategyRecommendations.stream().map(Recommendation::getName).collect(Collectors.toSet());
		int i = 0;
		String recommendationName;
		do {
			recommendationName = strategy.getName() + " | " + " Recommendation " + ++i;
		} while (recommendationNames.contains(recommendationName));
		recommendation.setName(recommendationName);
		recommendation.setRecommendationResultName(recommendation.getId() + "_recommendationResult");
		recommendation.setStrategy(strategy);

		return recommendation;
	}
}

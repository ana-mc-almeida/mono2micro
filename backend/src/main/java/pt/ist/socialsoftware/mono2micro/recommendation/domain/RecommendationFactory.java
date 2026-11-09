package pt.ist.socialsoftware.mono2micro.recommendation.domain;

import pt.ist.socialsoftware.mono2micro.recommendation.dto.*;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.*;

public class RecommendationFactory {
	public static Recommendation getRecommendation(RecommendationDto recommendationDto) {
		switch (recommendationDto.getType()) {
		case RECOMMEND_MATRIX_SCIPY:
			return new RecommendMatrixSciPy((RecommendMatrixSciPyDto) recommendationDto);
		default:
			throw new RuntimeException("The type \"" + recommendationDto.getType() + "\" is not a valid decomposition type for recommendation creation.");
		}
	}

	/** A new recommendation of the request's kind, over the representations its kind reads, named apart from the software system's others. */
	public static Recommendation getRecommendation(List<Representation> representations, List<Recommendation> softwareSystemRecommendations,
			RecommendationDto recommendationDto) {
		Recommendation recommendation = getRecommendation(recommendationDto);
		recommendation.setCompleted(false);

		// Get new name
		Set<String> recommendationNames = softwareSystemRecommendations.stream().map(Recommendation::getName).collect(Collectors.toSet());
		int i = 0;
		String recommendationName;
		do {
			recommendationName = recommendationDto.getKindName() + " | " + " Recommendation " + ++i;
		} while (recommendationNames.contains(recommendationName));
		recommendation.setName(recommendationName);
		recommendation.setRecommendationResultName(recommendation.getId() + "_recommendationResult");
		recommendation.setSoftwareSystem(Representation.softwareSystemOf(representations));
		recommendation.setKindName(recommendationDto.getKindName());
		recommendation.setRepresentations(representations);

		return recommendation;
	}
}

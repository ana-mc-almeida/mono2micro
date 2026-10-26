package pt.ist.socialsoftware.mono2micro.recommendation.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationFactory;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDto;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDtoFactory;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.StrategyRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RecommendationService {
	@Autowired
	StrategyRepository strategyRepository;

	@Autowired
	RecommendationRepository recommendationRepository;

	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	GridFsService gridFsService;

	public RecommendationDto createRecommendation(RecommendationDto recommendationDto) {
		Strategy strategy = strategyRepository.getById(recommendationDto.getStrategyId());
		List<Recommendation> strategyRecommendations = recommendationRepository.findByStrategyId(strategy.getId());
		RecommendMatrixSciPy existingRecommendation = (RecommendMatrixSciPy) strategyRecommendations.stream()
				.filter(recommendation -> recommendation.equalsDto(recommendationDto)).findFirst().orElse(null);

		Recommendation recommendation;
		// Create from scratch
		if (existingRecommendation == null) {
			recommendation = RecommendationFactory.getRecommendation(strategy, strategyRecommendations, recommendationDto);
			recommendationRepository.save(recommendation);
		} else return RecommendationDtoFactory.getRecommendationDto(existingRecommendation);

		recommendation.generateRecommendation(recommendationRepository);
		return RecommendationDtoFactory.getRecommendationDto(recommendation);
	}

	public void createDecompositions(String recommendationId, List<String> decompositionNames) throws Exception {
		Recommendation recommendation = getRecommendationById(recommendationId);
		recommendation.createDecompositions(similarityRepository.findByStrategyId(recommendation.getStrategy().getId()), decompositionNames);
	}

	public void deleteRecommendation(Recommendation recommendation) {
		recommendation.deleteProperties();
		gridFsService.deleteFile(recommendation.getRecommendationResultName());
		recommendationRepository.delete(recommendation);
	}

	public String getRecommendationResult(String recommendationId) throws IOException {
		Recommendation recommendation = getRecommendationById(recommendationId);
		return IOUtils.toString(gridFsService.getFile(recommendation.getRecommendationResultName()), StandardCharsets.UTF_8);
	}

	private Recommendation getRecommendationById(String recommendationId) {
		return recommendationRepository.getById(recommendationId);
	}
}
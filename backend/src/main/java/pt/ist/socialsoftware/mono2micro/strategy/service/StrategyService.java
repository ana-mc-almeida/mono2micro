package pt.ist.socialsoftware.mono2micro.strategy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.repository.SoftwareSystemRepository;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;
import pt.ist.socialsoftware.mono2micro.recommendation.service.RecommendationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.StrategyRepository;

import java.util.List;

@Service
public class StrategyService {
	@Autowired
	SoftwareSystemRepository softwareSystemRepository;

	@Autowired
	StrategyRepository strategyRepository;

	@Autowired
	DecompositionService decompositionService;

	@Autowired
	SimilarityService similarityService;

	@Autowired
	RecommendationService recommendationService;

	/** Returns the new strategy, or the existing one with the same algorithm and types. */
	public Strategy createStrategy(String softwareSystemId, String algorithmType, List<String> strategyTypes) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findById(softwareSystemId)
				.orElseThrow(() -> new RuntimeException("No software system " + softwareSystemId + " found."));
		for (Strategy strategy : softwareSystem.getStrategies())
			if (strategy.getAlgorithmType().equals(algorithmType) &&
					strategy.getStrategyTypes().size() == strategyTypes.size() &&
					strategy.getStrategyTypes().containsAll(strategyTypes)) // strategy already exists
				return strategy;
		Strategy strategy = new Strategy(softwareSystem, algorithmType, strategyTypes);
		strategy.setSoftwareSystem(softwareSystem);
		softwareSystem.addStrategy(strategy);
		strategyRepository.save(strategy);
		softwareSystemRepository.save(softwareSystem);
		return strategy;
	}

	public void removeSpecificStrategyProperties(Strategy strategy) {
		deleteStrategySimilarities(strategy);
		deleteStrategyRecommendations(strategy);
	}

	public List<Similarity> getStrategySimilarities(String strategyName) {
		Strategy strategy = strategyRepository.findByName(strategyName);
		return strategy.getSimilarities();
	}

	public void deleteSingleStrategy(String strategyName) {
		Strategy strategy = strategyRepository.findByName(strategyName);
		strategy.getSoftwareSystem().removeStrategy(strategyName);
		for (Decomposition decomposition : strategy.getDecompositions())
			decompositionService.deleteDecomposition(decomposition);
		removeSpecificStrategyProperties(strategy);
		strategyRepository.delete(strategy);
		softwareSystemRepository.save(strategy.getSoftwareSystem());
	}

	public void deleteStrategy(Strategy strategy) {
		for (Decomposition decomposition : strategy.getDecompositions())
			decompositionService.deleteDecomposition(decomposition);
		removeSpecificStrategyProperties(strategy);
		strategyRepository.delete(strategy);
	}

	public Strategy getStrategy(String strategyName) {
		return strategyRepository.findByName(strategyName);
	}

	public List<Decomposition> getStrategyDecompositions(String strategyName) {
		Strategy strategy = strategyRepository.findByName(strategyName);
		return strategy.getDecompositions();
	}

	private void deleteStrategySimilarities(Strategy strategy) {
		for (Similarity similarity : strategy.getSimilarities())
			similarityService.deleteSimilarity(similarity);
	}

	private void deleteStrategyRecommendations(Strategy strategy) {
		for (Recommendation recommendation : strategy.getRecommendations())
			recommendationService.deleteRecommendation(recommendation);
	}
}
package pt.ist.socialsoftware.mono2micro.similarity.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityFactory;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.StrategyRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class SimilarityService {
	@Autowired
	StrategyRepository strategyRepository;

	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	DecompositionService decompositionService;

	@Autowired
	GridFsService gridFsService;

	/** Returns the new similarity, or the existing one equal to the request. */
	public Similarity createSimilarity(SimilarityDto similarityDto) throws Exception {
		Strategy strategy = strategyRepository.findById(similarityDto.getStrategyId())
				.orElseThrow(() -> new RuntimeException("No strategy " + similarityDto.getStrategyId() + " found."));
		similarityDto.setStrategyName(strategy.getName()); // the generated name starts with it
		for (Similarity existing : strategy.getSimilarities())
			if (existing.equalsDto(similarityDto))
				return existing;
		Similarity similarity = SimilarityFactory.getSimilarity(strategy, similarityDto);

		similarity.generate();

		similarityRepository.save(similarity);
		strategyRepository.save(strategy);
		return similarity;
	}

	public void deleteSingleSimilarity(String similarityId) {
		Similarity similarity = getSimilarity(similarityId);

		for (Decomposition decomposition : similarity.getDecompositions())
			decompositionService.deleteDecomposition(decomposition);

		similarity.removeProperties();

		Strategy strategy = similarity.getStrategy();
		strategy.removeSimilarity(similarity.getName());
		strategyRepository.save(strategy);

		similarityRepository.deleteById(similarityId);
	}

	public void deleteSimilarity(Similarity similarity) { // Used when strategy is deleted
		similarity.removeProperties();
		similarityRepository.deleteById(similarity.getId());
	}

	public List<Decomposition> getDecompositions(String similarityId) {
		Similarity similarity = getSimilarity(similarityId);
		return similarity.getDecompositions();
	}

	public Similarity getSimilarity(String similarityId) {
		return similarityRepository.findById(similarityId).orElseThrow(() -> new RuntimeException("No similarity " + similarityId + " found."));
	}

	public byte[] getDendrogramImage(String similarityId) throws IOException {
		Similarity similarity = getSimilarity(similarityId);
		InputStream inputStream = gridFsService.getFile(similarity.getId() + "_image");
		return IOUtils.toByteArray(inputStream);
	}
}
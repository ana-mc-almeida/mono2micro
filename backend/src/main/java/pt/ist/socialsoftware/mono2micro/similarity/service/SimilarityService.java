package pt.ist.socialsoftware.mono2micro.similarity.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.DecompositionRepository;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.ProfileService;
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
	DecompositionRepository decompositionRepository;

	@Autowired
	DecompositionService decompositionService;

	@Autowired
	ProfileService profileService;

	@Autowired
	GridFsService gridFsService;

	/** Returns the new similarity, or the existing one equal to the request. */
	public Similarity createSimilarity(SimilarityDto similarityDto) throws Exception {
		Strategy strategy = strategyRepository.getById(similarityDto.getStrategyId());
		similarityDto.setStrategyName(strategy.getName()); // the generated name starts with it
		Profile profile = profileService.getScopingProfile(similarityDto.getProfileId(), strategy.getSoftwareSystem().getId());
		similarityDto.setProfileName(Profile.nameOf(profile)); // and shows the profile
		for (Similarity existing : similarityRepository.findByStrategyId(strategy.getId()))
			if (existing.equalsDto(similarityDto))
				return existing;
		Similarity similarity = SimilarityFactory.createSimilarity(strategy, similarityDto);
		similarity.setProfile(profile);

		similarity.generate();

		similarityRepository.save(similarity);
		return similarity;
	}

	public void deleteSingleSimilarity(String similarityId) {
		deleteSimilarity(getSimilarityById(similarityId));
	}

	public void deleteSimilarity(Similarity similarity) {
		for (Decomposition decomposition : decompositionRepository.findBySimilarityId(similarity.getId()))
			decompositionService.deleteDecomposition(decomposition);

		similarity.removeProperties();
		similarityRepository.deleteById(similarity.getId());
	}

	public List<Decomposition> getDecompositions(String similarityId) {
		return decompositionRepository.findBySimilarityId(similarityId);
	}

	public Similarity getSimilarityById(String similarityId) {
		return similarityRepository.getById(similarityId);
	}

	public byte[] getDendrogramImage(String similarityId) throws IOException {
		Similarity similarity = getSimilarityById(similarityId);
		InputStream inputStream = gridFsService.getFile(similarity.getId() + "_image");
		return IOUtils.toByteArray(inputStream);
	}
}
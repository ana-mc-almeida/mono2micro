package pt.ist.socialsoftware.mono2micro.recommendation.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationFactory;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDto;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDtoFactory;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.ProfileService;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RecommendationService {
	@Autowired
	RecommendationRepository recommendationRepository;

	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	SimilarityService similarityService;

	@Autowired
	ProfileService profileService;

	@Autowired
	GridFsService gridFsService;

	/**
	 * Returns the new recommendation of the requested kind on the software system, or the existing one equal to the request.
	 *
	 * @throws IllegalArgumentException
	 * if no kind has the requested name, or the request's weights are not the ones its kind is computed with
	 * @throws pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException
	 * if the software system's representations do not offer the requested kind
	 */
	public RecommendationDto createRecommendation(RecommendationDto recommendationDto) {
		String softwareSystemId = recommendationDto.getSoftwareSystemId();
		SimilarityKind kind = SimilarityKind.getByName(recommendationDto.getKindName());
		if (!kind.weightsTypes().equals(recommendationDto.weightsTypes()))
			throw new IllegalArgumentException("The " + recommendationDto.getKindName() + " kind is computed with the weights "
					+ kind.weightsTypes() + ", not " + recommendationDto.weightsTypes() + ".");
		List<Representation> representations = similarityService.resolveRepresentations(kind, softwareSystemId);

		List<Recommendation> softwareSystemRecommendations = recommendationRepository.findBySoftwareSystemId(softwareSystemId);
		Recommendation existingRecommendation = softwareSystemRecommendations.stream()
				.filter(recommendation -> recommendation.equalsDto(recommendationDto)).findFirst().orElse(null);
		if (existingRecommendation != null)
			return RecommendationDtoFactory.getRecommendationDto(existingRecommendation);

		Recommendation recommendation = RecommendationFactory.getRecommendation(representations, softwareSystemRecommendations, recommendationDto);
		recommendation.setProfile(profileService.getScopingProfile(recommendationDto.getProfileId(), softwareSystemId,
				recommendationDto.scopedRepresentationType()));
		recommendationRepository.save(recommendation);

		recommendation.generateRecommendation(recommendationRepository);
		return RecommendationDtoFactory.getRecommendationDto(recommendation);
	}

	/**
	 * Saves the chosen candidates, each with the Similarity of its weights, found among those of the same kind or created (D-016). A found
	 * one reads the same representations, which {@code equalsDto} compares, so it is the software system's.
	 */
	public void createDecompositions(String recommendationId, List<String> decompositionNames) throws Exception {
		Recommendation recommendation = getRecommendationById(recommendationId);
		recommendation.createDecompositions(similarityRepository.findByKindName(recommendation.getKindName()), decompositionNames);
	}

	public List<Recommendation> getRecommendations(String softwareSystemId) {
		return recommendationRepository.findBySoftwareSystemId(softwareSystemId);
	}

	/** Deletes the recommendations whose Similarities read the representation, as deleting a representation must. */
	public void deleteRecommendationsReading(String representationId) {
		for (Recommendation recommendation : recommendationRepository.findByRepresentations_Id(representationId))
			deleteRecommendation(recommendation);
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
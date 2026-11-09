package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.DecompositionRepository;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;
import pt.ist.socialsoftware.mono2micro.recommendation.service.RecommendationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.repository.SoftwareSystemRepository;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDto;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDtoFactory;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.StrategyRepository;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SoftwareSystemService {

	@Autowired
	RepresentationService representationService;

	@Autowired
	StrategyService strategyService;

	@Autowired
	SoftwareSystemRepository softwareSystemRepository;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	StrategyRepository strategyRepository;

	@Autowired
	SimilarityService similarityService;

	@Autowired
	RecommendationService recommendationService;

	@Autowired
	DecompositionRepository decompositionRepository;

	public SoftwareSystem createSoftwareSystem(String softwareSystemName) {
		if (softwareSystemRepository.existsByName(softwareSystemName))
			throw new KeyAlreadyExistsException();
		return softwareSystemRepository.save(new SoftwareSystem(softwareSystemName));
	}

	public Map<String, List<String>> getRepresentationGroups() {
		return Representation.representationGroupToRepresentations;
	}

	public List<String> getSoftwareSystemRepresentationGroups(String softwareSystemId) {
		Set<String> representationTypes = representationKinds(softwareSystemId);

		return Representation.representationGroupToRepresentations.entrySet().stream()
				.filter(entry -> representationTypes.containsAll(entry.getValue()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	public List<SoftwareSystem> getSoftwareSystems() {
		return softwareSystemRepository.getSoftwareSystems();
	}

	public SoftwareSystem getSoftwareSystemById(String softwareSystemId) {
		return softwareSystemRepository.getById(softwareSystemId);
	}

	public List<RepresentationDto> getRepresentationTypes(String softwareSystemId) {
		return RepresentationDtoFactory.getFactory().getRepresentationDtos(getRepresentations(softwareSystemId));
	}

	public List<Representation> getRepresentations(String softwareSystemId) {
		return representationRepository.findBySoftwareSystemId(softwareSystemId);
	}

	public List<Strategy> getSoftwareSystemStrategies(String softwareSystemId) {
		return strategyRepository.findBySoftwareSystemId(softwareSystemId);
	}

	public List<String> getAllowableSoftwareSystemStrategyTypes(String softwareSystemId) {
		// FIXME (ticket 09): offers only the kinds a strategy can hold, until the strategy screen goes
		return AvailabilityCheck.derive(SelfRegistration.prototypes(SimilarityKind.class).values(), availabilityContext(softwareSystemId))
				.availableNames().stream().filter(Strategy::isStrategyType).collect(Collectors.toList());
	}

	/** The software system's context: each family is asked once which of its features the system's collected representations make present (D-025). */
	private AvailabilityContext availabilityContext(String softwareSystemId) {
		return FeatureFamilies.scanned().contextFor(representationRepository.findBySoftwareSystemId(softwareSystemId));
	}

	/** The decompositions of the software system: those of its Similarities, and those of its strategies'. */
	public List<Decomposition> getDecompositions(String softwareSystemId) {
		List<Decomposition> decompositions = similarityService.getSimilarities(softwareSystemId).stream()
				.flatMap(similarity -> decompositionRepository.findBySimilarityId(similarity.getId()).stream())
				.collect(Collectors.toList());
		// FIXME (ticket 09): the strategies' decompositions go with Strategy
		for (Strategy strategy : strategyRepository.findBySoftwareSystemId(softwareSystemId))
			decompositions.addAll(decompositionRepository.findByStrategyId(strategy.getId()));
		return decompositions;
	}

	public void deleteSoftwareSystem(String softwareSystemId) {
		SoftwareSystem softwareSystem = getSoftwareSystemById(softwareSystemId);
		for (Similarity similarity : similarityService.getSimilarities(softwareSystemId))
			similarityService.deleteSimilarity(similarity);
		for (Recommendation recommendation : recommendationService.getRecommendations(softwareSystemId))
			recommendationService.deleteRecommendation(recommendation);
		for (Strategy strategy : strategyRepository.findBySoftwareSystemId(softwareSystemId))
			strategyService.deleteStrategy(strategy);
		for (Representation representation : representationRepository.findBySoftwareSystemId(softwareSystemId))
			representationService.deleteRepresentation(representation.getId());
		softwareSystemRepository.delete(softwareSystem);
	}

	private Set<String> representationKinds(String softwareSystemId) {
		return representationRepository.findBySoftwareSystemId(softwareSystemId).stream()
				.map(Representation::getType)
				.collect(Collectors.toSet());
	}
}

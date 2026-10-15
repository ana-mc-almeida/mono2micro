package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.repository.SoftwareSystemRepository;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDto;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDtoFactory;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SoftwareSystemService {

	@Autowired
	RepresentationService representationService;

	@Autowired
	StrategyService strategyService;

	@Autowired
	SoftwareSystemRepository softwareSystemRepository;

	public void createSoftwareSystem(String softwareSystemName) {
		if (softwareSystemRepository.existsByName(softwareSystemName))
			throw new KeyAlreadyExistsException();
		softwareSystemRepository.save(new SoftwareSystem(softwareSystemName));
	}

	public Map<String, List<String>> getRepresentationGroups() {
		return Representation.representationGroupToRepresentations;
	}

	public List<String> getSoftwareSystemRepresentationGroups(String softwareSystemName) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findByName(softwareSystemName);
		return softwareSystem.getRepresentationGroups();
	}

	public List<SoftwareSystem> getSoftwareSystems() {
		return softwareSystemRepository.getSoftwareSystems();
	}

	public SoftwareSystem getSoftwareSystem(String softwareSystemName) {
		return softwareSystemRepository.findByName(softwareSystemName);
	}

	public List<RepresentationDto> getRepresentationTypes(String softwareSystemName) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findByName(softwareSystemName);
		return RepresentationDtoFactory.getFactory().getRepresentationDtos(softwareSystem.getRepresentations());
	}

	public List<Strategy> getSoftwareSystemStrategies(String softwareSystemName) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findByName(softwareSystemName);
		return softwareSystem.getStrategies();
	}

	public List<String> getAllowableSoftwareSystemStrategyTypes(String softwareSystemName) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findByName(softwareSystemName);
		return AvailabilityCheck.derive(
				SelfRegistration.prototypes(SimilarityKind.class).values(),
				availabilityContextOf(softwareSystem)).availableNames();
	}

	public void deleteSoftwareSystem(String softwareSystemName) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findByName(softwareSystemName);
		for (Strategy strategy : softwareSystem.getStrategies())
			strategyService.deleteStrategy(strategy);
		for (Representation representation : softwareSystem.getRepresentations())
			representationService.deleteRepresentation(representation.getName());
		softwareSystemRepository.deleteById(softwareSystemName);
	}

	/** The context the shared check reads: the kinds of the representations this software system has collected (D-022). */
	private AvailabilityContext availabilityContextOf(SoftwareSystem softwareSystem) {
		return AvailabilityContext.ofRepresentationKinds(softwareSystem.getRepresentations().stream()
				.map(Representation::getType)
				.collect(Collectors.toSet()));
	}
}

package pt.ist.socialsoftware.mono2micro.representation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.repository.SoftwareSystemRepository;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.domain.RepresentationFactory;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepresentationService {
	@Autowired
	SoftwareSystemRepository softwareSystemRepository;

	@Autowired
	StrategyService strategyService;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	GridFsService gridFsService;

	public void addRepresentations(String softwareSystemId, String representationInfoType, List<String> representationTypes, List<Object> representations)
			throws Exception {
		SoftwareSystem softwareSystem = softwareSystemRepository.findById(softwareSystemId)
				.orElseThrow(() -> new RuntimeException("No software system " + softwareSystemId + " found."));

		if (representationTypes == null && representations == null) { // representations already added
			return;
		}
		if (representationTypes == null || representations == null || representationTypes.size() != representations.size())
			throw new RuntimeException("Number of representations is different from the number of representation types.");

		List<String> availableRepresentationsTypes = softwareSystem.getRepresentations().stream().map(Representation::getType).collect(Collectors.toList());

		for (String representationType : representationTypes)
			if (availableRepresentationsTypes.contains(representationType))
				throw new RuntimeException("Re-sending representations is not allowed.");

		for (int i = 0; i < representationTypes.size(); i++) {
			String representationType = representationTypes.get(i);
			byte[] representationFileStream = ((MultipartFile) representations.get(i)).getBytes();
			Representation representation = RepresentationFactory.getRepresentation(representationType);
			String fileName = representation.init(softwareSystem, representationFileStream);
			softwareSystem.addRepresentation(representation);
			gridFsService.saveFile(new ByteArrayInputStream(representationFileStream), fileName);
			representationRepository.save(representation);
		}
		softwareSystemRepository.save(softwareSystem);
	}

	public Representation getSoftwareSystemRepresentation(String softwareSystemId, String representationType) {
		SoftwareSystem softwareSystem = softwareSystemRepository.findById(softwareSystemId)
				.orElseThrow(() -> new RuntimeException("No software system " + softwareSystemId + " found."));
		return softwareSystem.getRepresentations().stream().filter(representation -> representation.getType().equals(representationType)).findFirst()
				.orElseThrow(() -> new RuntimeException("No representation with type" + representationType));
	}

	public Representation getRepresentation(String representationId) {
		return representationRepository.findById(representationId)
				.orElseThrow(() -> new RuntimeException("No representation " + representationId + " found."));
	}

	public void deleteRepresentation(String representationId) {
		Representation representation = representationRepository.findById(representationId)
				.orElseThrow(() -> new RuntimeException("No representation " + representationId));
		gridFsService.deleteFile(representation.getId());
		representationRepository.deleteById(representationId);
	}

	public void deleteSingleRepresentation(String representationId) {
		Representation representation = representationRepository.findById(representationId)
				.orElseThrow(() -> new RuntimeException("No representation " + representationId));
		SoftwareSystem softwareSystem = representation.getSoftwareSystem();
		softwareSystem.removeRepresentation(representation.getName());
		ArrayList<Strategy> strategies = new ArrayList<>();
		for (Strategy strategy : softwareSystem.getStrategies()) {
			boolean deleted = false;
			for (String strategyType : strategy.getStrategyTypes()) {
				if (Strategy.strategiesToRepresentations.get(strategyType).contains(representation.getType())) {
					strategyService.deleteStrategy(strategy);
					deleted = true;
					break;
				}
			}
			if (!deleted)
				strategies.add(strategy);
		}

		softwareSystem.setStrategies(strategies);
		gridFsService.deleteFile(representation.getId());
		softwareSystemRepository.save(softwareSystem);
		representationRepository.deleteById(representationId);
	}
}
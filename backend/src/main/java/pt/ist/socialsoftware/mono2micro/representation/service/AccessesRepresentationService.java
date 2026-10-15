package pt.ist.socialsoftware.mono2micro.representation.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.repository.SoftwareSystemRepository;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;

@Service
public class AccessesRepresentationService {

	@Autowired
	SoftwareSystemRepository softwareSystemRepository;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	GridFsService gridFsService;

	public void addAccessesProfile(String representationId, String profile) {
		AccessesRepresentation representation = (AccessesRepresentation) representationRepository.findById(representationId)
				.orElseThrow(() -> new RuntimeException("No representation " + representationId + " found."));
		representation.addProfile(profile, new HashSet<>());
		representationRepository.save(representation);
	}

	public void moveAccessesFunctionalities(String representationId, String[] functionalities, String targetProfile) {
		AccessesRepresentation representation = (AccessesRepresentation) representationRepository.findById(representationId)
				.orElseThrow(() -> new RuntimeException("No representation " + representationId + " found."));
		representation.moveFunctionalities(functionalities, targetProfile);
		representationRepository.save(representation);
	}

	public void deleteAccessesProfile(String representationId, String profile) {
		AccessesRepresentation representation = (AccessesRepresentation) representationRepository.findById(representationId)
				.orElseThrow(() -> new RuntimeException("No representation " + representationId + " found."));
		representation.deleteProfile(profile);
		representationRepository.save(representation);
	}

	public String getIdToEntity(String softwareSystemName) throws IOException {
		SoftwareSystem softwareSystem = softwareSystemRepository.findByName(softwareSystemName);
		Representation representation = softwareSystem.getRepresentationByFileType(ID_TO_ENTITY);
		return IOUtils.toString(gridFsService.getFile(representation.getName()), StandardCharsets.UTF_8);
	}
}

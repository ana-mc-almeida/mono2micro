package pt.ist.socialsoftware.mono2micro.representation.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;

@Service
public class AccessesRepresentationService {

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	GridFsService gridFsService;

	public String getIdToEntity(String softwareSystemId) throws IOException {
		Representation representation = representationRepository.findBySoftwareSystemIdAndType(softwareSystemId, ID_TO_ENTITY)
				.orElseThrow(() -> new RuntimeException("No " + ID_TO_ENTITY + " representation in software system " + softwareSystemId + "."));
		return IOUtils.toString(gridFsService.getFile(representation.getId()), StandardCharsets.UTF_8);
	}
}

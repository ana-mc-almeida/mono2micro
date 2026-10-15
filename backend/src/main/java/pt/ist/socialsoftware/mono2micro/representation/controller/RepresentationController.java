package pt.ist.socialsoftware.mono2micro.representation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
// FIXME (ticket 07): unused import, left over from a copied logger; remove
import pt.ist.socialsoftware.mono2micro.softwareSystem.SoftwareSystemController;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDto;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDtoFactory;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;

import java.util.List;

@RestController
@RequestMapping(value = "/mono2micro")
public class RepresentationController {

	private static final Logger logger = LoggerFactory.getLogger(RepresentationController.class);

	@Autowired
	RepresentationService representationService;

	@GetMapping(value = "/softwareSystems/{softwareSystemName}/representation/{representationType}/getSoftwareSystemRepresentation")
	public ResponseEntity<RepresentationDto> getSoftwareSystemRepresentation(
			@PathVariable String softwareSystemName,
			@PathVariable String representationType
	) {
		logger.debug("getSoftwareSystemRepresentation");

		try {
			return new ResponseEntity<>(
					RepresentationDtoFactory.getFactory()
							.getRepresentationDto(representationService.getSoftwareSystemRepresentation(softwareSystemName, representationType)),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/representation/{representationName}/delete")
	public ResponseEntity<HttpStatus> deleteRepresentation(@PathVariable String representationName) {
		// FIXME (ticket 07): copied from deleteSoftwareSystem; should be "deleteRepresentation"
		logger.debug("deleteSoftwareSystem");

		try {
			representationService.deleteSingleRepresentation(representationName);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/representation/{representationName}/getRepresentation")
	public ResponseEntity<RepresentationDto> getRepresentation(@PathVariable String representationName) {
		logger.debug("getRepresentation");

		try {
			return new ResponseEntity<>(RepresentationDtoFactory.getFactory().getRepresentationDto(representationService.getRepresentation(representationName)),
					HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/softwareSystems/{softwareSystemName}/addRepresentations/{representationInfoType}")
	public ResponseEntity<HttpStatus> createStrategy(
			@PathVariable String softwareSystemName,
			@PathVariable String representationInfoType,
			@Nullable @RequestParam List<String> representationTypes,
			@Nullable @RequestParam List<Object> representations
	) {
		logger.debug("createStrategy");

		try {
			representationService.addRepresentations(softwareSystemName, representationInfoType, representationTypes, representations);
			return new ResponseEntity<>(HttpStatus.CREATED);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}
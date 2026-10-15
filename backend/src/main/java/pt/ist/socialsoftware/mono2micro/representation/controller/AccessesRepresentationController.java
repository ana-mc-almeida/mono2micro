package pt.ist.socialsoftware.mono2micro.representation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.softwareSystem.SoftwareSystemController;
import pt.ist.socialsoftware.mono2micro.representation.service.AccessesRepresentationService;

import javax.management.openmbean.KeyAlreadyExistsException;

@RestController
@RequestMapping(value = "/mono2micro")
public class AccessesRepresentationController {
	// FIXME (ticket 07): logger copied from SoftwareSystemController; use AccessesRepresentationController.class
	private static final Logger logger = LoggerFactory.getLogger(SoftwareSystemController.class);

	@Autowired
	AccessesRepresentationService accessesRepresentationService;

	@PostMapping(value = "/representation/{representationName}/addAccessesProfile")
	public ResponseEntity<HttpStatus> addAccessesProfile(
			@PathVariable String representationName,
			@RequestParam String profile
	) {
		logger.debug("addAccessesProfile");

		try {
			accessesRepresentationService.addAccessesProfile(representationName, profile);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (KeyAlreadyExistsException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/representation/{representationName}/moveAccessesFunctionalities")
	public ResponseEntity<HttpStatus> moveAccessesFunctionalities(
			@PathVariable String representationName,
			@RequestBody String[] functionalities,
			@RequestParam String targetProfile
	) {
		logger.debug("moveAccessesFunctionalities");

		try {
			accessesRepresentationService.moveAccessesFunctionalities(representationName, functionalities, targetProfile);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/representation/{representationName}/deleteAccessesProfile")
	public ResponseEntity<HttpStatus> deleteAccessesProfile(
			@PathVariable String representationName,
			@RequestParam String profile
	) {
		logger.debug("deleteAccessesProfile");

		try {
			accessesRepresentationService.deleteAccessesProfile(representationName, profile);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	// FIXME (ticket 07): the path variable is a software system name, not a representation name;
	// move to /softwareSystems/{softwareSystemName}/getIdToEntity
	@GetMapping(value = "/representation/{softwareSystemName}/getIdToEntity")
	public ResponseEntity<String> getIdToEntity(@PathVariable String softwareSystemName) {
		logger.debug("getIdToEntity");

		try {
			return new ResponseEntity<>(accessesRepresentationService.getIdToEntity(softwareSystemName), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}

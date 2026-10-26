package pt.ist.socialsoftware.mono2micro.representation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.softwareSystem.SoftwareSystemController;
import pt.ist.socialsoftware.mono2micro.representation.service.AccessesRepresentationService;

@RestController
@RequestMapping(value = "/mono2micro")
public class AccessesRepresentationController {
	// FIXME (ticket 07): logger copied from SoftwareSystemController; use AccessesRepresentationController.class
	private static final Logger logger = LoggerFactory.getLogger(SoftwareSystemController.class);

	@Autowired
	AccessesRepresentationService accessesRepresentationService;

	// FIXME (ticket 07): the path variable is a software system name, not a representation name;
	// move to /softwareSystems/{softwareSystemName}/getIdToEntity
	@GetMapping(value = "/representation/{softwareSystemId}/getIdToEntity")
	public ResponseEntity<String> getIdToEntity(@PathVariable String softwareSystemId) {
		logger.debug("getIdToEntity");

		try {
			return new ResponseEntity<>(accessesRepresentationService.getIdToEntity(softwareSystemId), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}

package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Arrays;
import java.util.HashSet;

@RestController
@RequestMapping(value = "/mono2micro")
public class ProfileController {
	private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

	@Autowired
	ProfileService profileService;

	@GetMapping(value = "/representation/{representationId}/getProfiles")
	public ResponseEntity<ProfilesDto> getProfiles(@PathVariable String representationId) {
		logger.debug("getProfiles");

		try {
			return new ResponseEntity<>(profileService.getProfiles(representationId), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/representation/{representationId}/addProfile")
	public ResponseEntity<ProfileDto> addProfile(
			@PathVariable String representationId,
			@RequestParam String profile
	) {
		logger.debug("addProfile");

		try {
			return new ResponseEntity<>(new ProfileDto(profileService.createProfile(representationId, profile)), HttpStatus.CREATED);

		} catch (KeyAlreadyExistsException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/** Without a target profile, the units go to Unassigned. */
	@PostMapping(value = "/representation/{representationId}/moveUnits")
	public ResponseEntity<HttpStatus> moveUnits(
			@PathVariable String representationId,
			@RequestBody String[] units,
			@RequestParam(required = false) String targetProfileId
	) {
		logger.debug("moveUnits");

		try {
			profileService.moveUnits(representationId, new HashSet<>(Arrays.asList(units)), targetProfileId);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/profile/{profileId}/delete")
	public ResponseEntity<HttpStatus> deleteProfile(@PathVariable String profileId) {
		logger.debug("deleteProfile");

		try {
			profileService.deleteProfile(profileId);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}

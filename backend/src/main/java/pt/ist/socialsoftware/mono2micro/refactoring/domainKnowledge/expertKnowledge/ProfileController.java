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
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/mono2micro")
public class ProfileController {
	private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

	@Autowired
	ProfileService profileService;

	/** Whether a profile is offered for a Similarity of the kind: the {@link Profile} feature's derivation record (D-013, D-017). */
	@GetMapping(value = "/softwareSystems/{softwareSystemId}/profiles/available")
	public ResponseEntity<DerivationRecord> getAvailability(@PathVariable String softwareSystemId, @RequestParam String kindName) {
		logger.debug("getProfileAvailability");

		try {
			return new ResponseEntity<>(profileService.getAvailability(softwareSystemId, kindName), HttpStatus.OK);
		} catch (IllegalArgumentException e) { // an unknown kind
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/** The profiles the selector lists for a Similarity of the kind; 422 when no profile is offered for it. */
	@GetMapping(value = "/softwareSystems/{softwareSystemId}/profiles")
	public ResponseEntity<List<ProfileDto>> getOfferedProfiles(@PathVariable String softwareSystemId, @RequestParam String kindName) {
		logger.debug("getOfferedProfiles");

		try {
			return new ResponseEntity<>(profileService.getOfferedProfiles(softwareSystemId, kindName).stream().map(ProfileDto::new)
					.collect(Collectors.toList()), HttpStatus.OK);
		} catch (UnavailableFeatureException e) {
			return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
		} catch (IllegalArgumentException e) { // an unknown kind
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/** The types of the software system's representations whose cards link to their profiles. */
	@GetMapping(value = "/softwareSystems/{softwareSystemId}/profiles/representationTypes")
	public ResponseEntity<Set<String>> getProfiledRepresentationTypes(@PathVariable String softwareSystemId) {
		logger.debug("getProfiledRepresentationTypes");

		return new ResponseEntity<>(profileService.getProfiledRepresentationTypes(softwareSystemId), HttpStatus.OK);
	}

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

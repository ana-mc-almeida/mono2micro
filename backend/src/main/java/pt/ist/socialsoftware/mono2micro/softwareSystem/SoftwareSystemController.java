package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.softwareSystem.dto.SoftwareSystemDto;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.decomposition.DecompositionDto;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.decomposition.DecompositionDtoFactory;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDto;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.dto.StrategyDto;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/mono2micro")
public class SoftwareSystemController {

	private static final Logger logger = LoggerFactory.getLogger(SoftwareSystemController.class);

	@Autowired
	SoftwareSystemService softwareSystemService;

	@GetMapping(value = "/softwareSystems")
	public ResponseEntity<List<SoftwareSystemDto>> getSoftwareSystems() {
		logger.debug("getSoftwareSystems");

		try {
			return new ResponseEntity<>(
					softwareSystemService.getSoftwareSystems().stream().map(SoftwareSystemDto::new).collect(Collectors.toList()),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}")
	public ResponseEntity<SoftwareSystemDto> getSoftwareSystem(
			@PathVariable String softwareSystemId
	) {
		logger.debug("getSoftwareSystem");

		try {
			return new ResponseEntity<>(
					new SoftwareSystemDto(softwareSystemService.getSoftwareSystem(softwareSystemId)),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}/getRepresentations")
	public ResponseEntity<List<RepresentationDto>> getRepresentations(
			@PathVariable String softwareSystemId
	) {
		logger.debug("getRepresentations");

		try {
			return new ResponseEntity<>(
					softwareSystemService.getRepresentationTypes(softwareSystemId),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}/getSoftwareSystemStrategies")
	public ResponseEntity<List<StrategyDto>> getSoftwareSystemStrategies(@PathVariable String softwareSystemId) {
		logger.debug("getSoftwareSystemStrategies");

		try {
			List<StrategyDto> strategies = softwareSystemService.getSoftwareSystemStrategies(softwareSystemId).stream().map(StrategyDto::new)
					.collect(Collectors.toList());
			return new ResponseEntity<>(strategies, HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}/getAllowableSoftwareSystemStrategyTypes")
	public ResponseEntity<List<String>> getAllowableSoftwareSystemStrategyTypes(@PathVariable String softwareSystemId) {
		logger.debug("getAllowableSoftwareSystemStrategyTypes");

		try {
			return new ResponseEntity<>(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(softwareSystemId), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}/getSoftwareSystemDecompositions")
	public ResponseEntity<List<DecompositionDto>> getSoftwareSystemDecompositions(
			@PathVariable String softwareSystemId
	) {
		logger.debug("getSoftwareSystemDecompositions");

		try {
			List<DecompositionDto> decompositionDtos = DecompositionDtoFactory.getDecompositionDtos(
					softwareSystemService.getSoftwareSystemStrategies(softwareSystemId).stream()
							.map(Strategy::getDecompositions)
							.flatMap(Collection::stream)
							.collect(Collectors.toList())
			);
			return new ResponseEntity<>(decompositionDtos, HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/softwareSystems/{softwareSystemId}/delete")
	public ResponseEntity<HttpStatus> deleteSoftwareSystem(@PathVariable String softwareSystemId) {
		logger.debug("deleteSoftwareSystem");

		try {
			softwareSystemService.deleteSoftwareSystem(softwareSystemId);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/softwareSystems/create")
	public ResponseEntity<SoftwareSystemDto> createSoftwareSystem(
			@RequestParam String softwareSystemName
	) {
		logger.debug("createSoftwareSystem");

		try {
			SoftwareSystem softwareSystem = softwareSystemService.createSoftwareSystem(softwareSystemName);
			return new ResponseEntity<>(new SoftwareSystemDto(softwareSystem), HttpStatus.CREATED);

		} catch (KeyAlreadyExistsException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}/getSoftwareSystemRepresentationGroups")
	public ResponseEntity<List<String>> getSoftwareSystemRepresentationGroups(
			@PathVariable String softwareSystemId
	) {
		logger.debug("getSoftwareSystemRepresentationGroups");

		try {
			return new ResponseEntity<>(softwareSystemService.getSoftwareSystemRepresentationGroups(softwareSystemId), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/getRepresentationGroups")
	public ResponseEntity<Map<String, List<String>>> getRepresentationGroups() {
		logger.debug("getRepresentationInfoTypes");

		try {
			return new ResponseEntity<>(softwareSystemService.getRepresentationGroups(), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}
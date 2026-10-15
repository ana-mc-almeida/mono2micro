package pt.ist.socialsoftware.mono2micro.softwareSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

	@GetMapping(value = "/softwareSystems/{softwareSystemName}")
	public ResponseEntity<SoftwareSystemDto> getSoftwareSystem(
			@PathVariable String softwareSystemName
	) {
		logger.debug("getSoftwareSystem");

		try {
			return new ResponseEntity<>(
					new SoftwareSystemDto(softwareSystemService.getSoftwareSystem(softwareSystemName)),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemName}/getRepresentations")
	public ResponseEntity<List<RepresentationDto>> getRepresentations(
			@PathVariable String softwareSystemName
	) {
		logger.debug("getRepresentations");

		try {
			return new ResponseEntity<>(
					softwareSystemService.getRepresentationTypes(softwareSystemName),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemName}/getSoftwareSystemStrategies")
	public ResponseEntity<List<StrategyDto>> getSoftwareSystemStrategies(@PathVariable String softwareSystemName) {
		logger.debug("getSoftwareSystemStrategies");

		try {
			List<StrategyDto> strategies = softwareSystemService.getSoftwareSystemStrategies(softwareSystemName).stream().map(StrategyDto::new)
					.collect(Collectors.toList());
			return new ResponseEntity<>(strategies, HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemName}/getAllowableSoftwareSystemStrategyTypes")
	public ResponseEntity<List<String>> getAllowableSoftwareSystemStrategyTypes(@PathVariable String softwareSystemName) {
		logger.debug("getPossibleSoftwareSystemStrategyTypes");

		try {
			return new ResponseEntity<>(softwareSystemService.getAllowableSoftwareSystemStrategyTypes(softwareSystemName), HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemName}/getSoftwareSystemDecompositions")
	public ResponseEntity<List<DecompositionDto>> getSoftwareSystemDecompositions(
			@PathVariable String softwareSystemName
	) {
		logger.debug("getSoftwareSystemDecompositions");

		try {
			List<DecompositionDto> decompositionDtos = DecompositionDtoFactory.getDecompositionDtos(
					softwareSystemService.getSoftwareSystemStrategies(softwareSystemName).stream()
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

	@DeleteMapping(value = "/softwareSystems/{softwareSystemName}/delete")
	public ResponseEntity<HttpStatus> deleteSoftwareSystem(@PathVariable String softwareSystemName) {
		logger.debug("deleteSoftwareSystem");

		try {
			softwareSystemService.deleteSoftwareSystem(softwareSystemName);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping(value = "/softwareSystems/create")
	public ResponseEntity<HttpStatus> createSoftwareSystem(
			@RequestParam String softwareSystemName
	) {
		logger.debug("createSoftwareSystem");

		try {
			softwareSystemService.createSoftwareSystem(softwareSystemName);
			return new ResponseEntity<>(HttpStatus.CREATED);

		} catch (KeyAlreadyExistsException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemName}/getSoftwareSystemRepresentationGroups")
	public ResponseEntity<List<String>> getSoftwareSystemRepresentationGroups(
			@PathVariable String softwareSystemName
	) {
		logger.debug("getSoftwareSystemRepresentationInfoTypes");

		try {
			return new ResponseEntity<>(softwareSystemService.getSoftwareSystemRepresentationGroups(softwareSystemName), HttpStatus.OK);

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
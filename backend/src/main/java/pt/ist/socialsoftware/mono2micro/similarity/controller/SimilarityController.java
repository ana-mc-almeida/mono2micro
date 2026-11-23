package pt.ist.socialsoftware.mono2micro.similarity.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.ClusterRepresentationDto;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.ClusterRepresentationDtoFactory;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.AlgorithmOperation;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.EditingChainService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityKindDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/mono2micro")
public class SimilarityController {

	private static final Logger logger = LoggerFactory.getLogger(SimilarityController.class);

	@Autowired
	SimilarityService similarityService;

	@Autowired
	EditingChainService editingChainService;

	/** Every Similarity kind, offered or withheld, with what each failed: the derivation record (D-013). */
	@GetMapping(value = "/softwareSystems/{softwareSystemId}/similarities/available")
	public ResponseEntity<DerivationRecord> getAvailableSimilarityKinds(
			@PathVariable String softwareSystemId
	) {
		logger.debug("getAvailableSimilarityKinds");

		try {
			return new ResponseEntity<>(similarityService.getAvailableKinds(softwareSystemId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/** What a Similarity kind's form starts from (D-029). An unknown kind is not found. */
	@GetMapping(value = "/similarityKinds/{kindName}")
	public ResponseEntity<SimilarityKindDto> getSimilarityKind(
			@PathVariable String kindName
	) {
		logger.debug("getSimilarityKind");

		try {
			return new ResponseEntity<>(new SimilarityKindDto(SimilarityKind.getByName(kindName)), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			logger.debug(e.getMessage());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(value = "/softwareSystems/{softwareSystemId}/similarities")
	public ResponseEntity<List<SimilarityDto>> getSimilarities(
			@PathVariable String softwareSystemId
	) {
		logger.debug("getSimilarities");

		try {
			return new ResponseEntity<>(
					similarityService.getSimilarities(softwareSystemId).stream().map(Similarity::toDto).collect(Collectors.toList()),
					HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Rejects a kind the software system's representations do not offer with 422, before anything is computed (D-003, D-013), and an unknown
	 * kind, or a type or weights its kind is not computed with, with 400.
	 */
	@PostMapping(value = "/softwareSystems/{softwareSystemId}/similarities/create")
	public ResponseEntity<SimilarityDto> createSimilarity(
			@PathVariable String softwareSystemId,
			@RequestBody SimilarityDto similarityDto
	) {
		logger.debug("Create Similarity Distances");

		try {
			return new ResponseEntity<>(
					similarityService.createSimilarity(softwareSystemId, similarityDto).toDto(),
					HttpStatus.CREATED);
		} catch (UnavailableFeatureException e) {
			logger.debug(e.getMessage());
			return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/similarity/{similarityId}/getSimilarity")
	public ResponseEntity<SimilarityDto> getSimilarity(
			@PathVariable String similarityId
	) {
		logger.debug("getSimilarity");

		try {
			return new ResponseEntity<>(
					similarityService.getSimilarityById(similarityId).toDto(),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/** The cluster representations measured with the Similarity: those its operations produced and those imported with it (D-015). */
	@GetMapping(value = "/similarities/{similarityId}/clusterRepresentations")
	public ResponseEntity<List<ClusterRepresentationDto>> getDecompositions(
			@PathVariable String similarityId
	) {
		logger.debug("getDecompositions");

		try {
			return new ResponseEntity<>(
					ClusterRepresentationDtoFactory.getDecompositionDtos(similarityService.getDecompositions(similarityId)),
					HttpStatus.OK
			);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/** Every algorithm operation, offered or withheld for the Similarity, with what each failed: the derivation record (D-013). */
	@GetMapping(value = "/similarities/{similarityId}/operations/available")
	public ResponseEntity<DerivationRecord> getAvailableOperations(
			@PathVariable String similarityId
	) {
		logger.debug("getAvailableOperations");

		try {
			return new ResponseEntity<>(editingChainService.getAvailableAlgorithmOperations(similarityId), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Runs an algorithm operation on the Similarity and returns its output. The body's {@code type} picks the operation, found by self-registration,
	 * so a new algorithm needs no endpoint of its own. An operation the Similarity does not offer is rejected with 422 before anything is computed
	 * (D-003, D-013).
	 */
	@PostMapping(value = "/similarities/{similarityId}/operations")
	public ResponseEntity<ClusterRepresentationDto> runOperation(
			@PathVariable String similarityId,
			@RequestBody AlgorithmOperation operation
	) {
		logger.debug("runOperation");

		try {
			return new ResponseEntity<>(
					ClusterRepresentationDtoFactory.getDecompositionDto(editingChainService.runAlgorithmOperation(similarityId, operation)),
					HttpStatus.CREATED);
		} catch (UnavailableFeatureException e) {
			logger.debug(e.getMessage());
			return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping(value = "/similarity/{similarityId}/delete")
	public ResponseEntity<HttpStatus> deleteSimilarity(
			@PathVariable String similarityId
	) {
		logger.debug("Delete Similarity");

		try {
			similarityService.deleteSingleSimilarity(similarityId);

			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/similarity/{similarityId}/image")
	public ResponseEntity<byte[]> getDendrogramImage(
			@PathVariable String similarityId
	) {
		logger.debug("getDendrogramImage");

		try {
			return ResponseEntity.ok()
					.contentType(MediaType.IMAGE_PNG)
					.body(similarityService.getDendrogramImage(similarityId));

		} catch (Exception e) {
			System.err.println(e.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}
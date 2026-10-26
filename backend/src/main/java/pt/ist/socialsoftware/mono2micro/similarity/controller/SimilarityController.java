package pt.ist.socialsoftware.mono2micro.similarity.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.decomposition.DecompositionDto;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.decomposition.DecompositionDtoFactory;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDtoFactory;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;

import java.util.List;

@RestController
@RequestMapping(value = "/mono2micro")
public class SimilarityController {

	private static final Logger logger = LoggerFactory.getLogger(SimilarityController.class);

	@Autowired
	SimilarityService similarityService;

	@PostMapping(value = "/similarity/create")
	public ResponseEntity<SimilarityDto> createSimilarity(
			@RequestBody SimilarityDto similarityDto
	) {
		logger.debug("Create Similarity Distances");

		try {
			return new ResponseEntity<>(
					SimilarityDtoFactory.getSimilarityDto(similarityService.createSimilarity(similarityDto)),
					HttpStatus.CREATED);
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
					SimilarityDtoFactory.getSimilarityDto(similarityService.getSimilarityById(similarityId)),
					HttpStatus.OK
			);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/similarity/{similarityId}/decompositions")
	public ResponseEntity<List<DecompositionDto>> getDecompositions(
			@PathVariable String similarityId
	) {
		logger.debug("getDecompositions");

		try {
			return new ResponseEntity<>(
					DecompositionDtoFactory.getDecompositionDtos(similarityService.getDecompositions(similarityId)),
					HttpStatus.OK
			);

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
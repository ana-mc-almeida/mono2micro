package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.dto.ComparisonToolResponseFactory;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.dto.ComparisonToolResponse;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;

@RestController
@RequestMapping(value = "/mono2micro")
public class ComparisonToolController {
	private static final Logger logger = LoggerFactory.getLogger(ComparisonToolController.class);

	@Autowired
	DecompositionService decompositionService;

	@PostMapping(value = "/comparison/{decomposition1Id}/{decomposition2Id}")
	public ResponseEntity<ComparisonToolResponse> getAnalysis(
			@PathVariable String decomposition1Id,
			@PathVariable String decomposition2Id
	) {
		logger.debug("getAnalysis");

		try {
			Decomposition decomposition1 = decompositionService.updateDecomposition(decomposition1Id);
			Decomposition decomposition2 = decompositionService.updateDecomposition(decomposition2Id);

			ComparisonToolResponse comparisonToolResponse = ComparisonToolResponseFactory.getComparisonToolResponse(decomposition1, decomposition2);
			return new ResponseEntity<>(comparisonToolResponse, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}

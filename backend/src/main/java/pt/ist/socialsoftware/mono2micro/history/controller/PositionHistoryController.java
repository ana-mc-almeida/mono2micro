package pt.ist.socialsoftware.mono2micro.history.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.history.service.PositionHistoryService;

import java.io.IOException;

@RestController
@RequestMapping(value = "/mono2micro/positionHistory/{decompositionId}")
public class PositionHistoryController {
	@Autowired
	PositionHistoryService historyService;

	@Autowired
	ClusterRepresentationRepository decompositionRepository;

	private static final Logger logger = LoggerFactory.getLogger(PositionHistoryController.class);

	@PostMapping(value = "/saveGraphPositions")
	public ResponseEntity<HttpStatus> saveGraphPositions(
			@PathVariable String decompositionId,
			@RequestBody String graphPositions
	) {
		logger.debug("saveGraphPositions");
		try {
			ClusterRepresentation decomposition = decompositionRepository.getById(decompositionId);

			historyService.saveGraphPositions(decomposition, graphPositions);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping(value = "/getGraphPositions")
	public ResponseEntity<String> getGraphPositions(
			@PathVariable String decompositionId
	) {
		logger.debug("getGraphPositions");

		try {
			ClusterRepresentation decomposition = decompositionRepository.getById(decompositionId);

			String graphPositions = historyService.getGraphPositions(decomposition);

			if (graphPositions == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			else return new ResponseEntity<>(graphPositions, HttpStatus.OK);

		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}
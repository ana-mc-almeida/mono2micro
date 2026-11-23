package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.service;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Expert;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.history.service.HistoryService;
import pt.ist.socialsoftware.mono2micro.operation.Operation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.utils.export.ContextMapperContractBuilder;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

@Service
public class ClusterRepresentationService {
	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	ClusterRepresentationRepository decompositionRepository;

	@Autowired
	HistoryService historyService;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	GridFsService gridFsService;

	public void createExpertDecomposition(String similarityId, String expertName, Optional<MultipartFile> expertFile) throws Exception {
		Similarity similarity = similarityRepository.getById(similarityId);
		Expert expert = new Expert();

		ClusterRepresentation decomposition = expert.generateClusters(similarity, expertName, expertFile,
				decompositionRepository.findNamesBySimilarityId(similarityId));

		setupDecomposition(decomposition);
	}

	public void setupDecomposition(ClusterRepresentation decomposition) throws Exception {
		decomposition.setup();
		decomposition.calculateMetrics();

		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
		similarityRepository.save(decomposition.getSimilarity());
	}

	public ClusterRepresentation updateDecomposition(String decompositionId) throws Exception {
		ClusterRepresentation decomposition = getDecompositionById(decompositionId);
		if (decomposition.isOutdated()) {
			decomposition.update();
			decomposition.calculateMetrics();
			decomposition.setOutdated(false);
			decompositionRepository.save(decomposition);
		}
		return decomposition;
	}

	public ClusterRepresentation getDecompositionById(String decompositionId) {
		return decompositionRepository.getById(decompositionId);
	}

	public void exportDecompositionToContextMapper(String decompositionId, ZipOutputStream zipOutputStream) throws IOException, JSONException {
		ClusterRepresentation decomposition = getDecompositionById(decompositionId);

		zipOutputStream.putNextEntry(new ZipEntry("m2m_decomposition.json"));
		zipOutputStream.write(new ContextMapperContractBuilder(decomposition)
				.addStructureRepresentationData(gridFsService.getFile(
						representationRepository.findBySoftwareSystemIdAndType(decomposition.getSoftwareSystem().getId(), STRUCTURE)
								.orElseThrow(() -> new RuntimeException("No representation with type " + STRUCTURE))
								.getId()))
				.addSagaRefactorizationData(gridFsService.getFile(decompositionId + "_refactorization"))
				.buildContract()
				.getBytes());
		zipOutputStream.closeEntry();
		zipOutputStream.close();
	}

	public void deleteSingleDecomposition(String decompositionId) {
		deleteDecomposition(getDecompositionById(decompositionId));
	}

	public void deleteDecomposition(ClusterRepresentation decomposition) {
		decomposition.deleteProperties();
		historyService.deleteHistory(decomposition.getHistory());
		decompositionRepository.deleteById(decomposition.getId());
	}

	/** Applies a manual operation to the decomposition, recording it in its history, and saves both. */
	public void applyOperation(String decompositionId, Operation operation) {
		ClusterRepresentation decomposition = getDecompositionById(decompositionId);
		operation.execute(decomposition);
		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
	}

	public String getEdgeWeights(String decompositionId, String representationInfo) throws Exception {
		ClusterRepresentation clusterView = getDecompositionById(decompositionId);
		return clusterView.getEdgeWeights(representationInfo);
	}

	public String getSearchItems(String decompositionId, String representationInfo) throws Exception {
		ClusterRepresentation clusterView = getDecompositionById(decompositionId);
		return clusterView.getSearchItems(representationInfo);
	}

	public void snapshotDecomposition(String decompositionId) throws Exception {
		ClusterRepresentation decomposition = updateDecomposition(decompositionId);
		Set<String> decompositionNames = decompositionRepository.findNamesBySimilarityId(decomposition.getSimilarity().getId());

		String snapshotName = decomposition.getName() + " SNAPSHOT";
		if (decompositionNames.contains(snapshotName)) {
			int i = 1;
			do {
				i++;
			} while (decompositionNames.contains(snapshotName + "(" + i + ")"));
			snapshotName = snapshotName + "(" + i + ")";
		}

		ClusterRepresentation snapshotDecomposition = decomposition.snapshotDecomposition(snapshotName);

		decompositionRepository.save(snapshotDecomposition);
	}
}
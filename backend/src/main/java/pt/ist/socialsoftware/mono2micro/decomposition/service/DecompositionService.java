package pt.ist.socialsoftware.mono2micro.decomposition.service;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Expert;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.request.DecompositionRequest;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.DecompositionRepository;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.history.service.HistoryService;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;
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
public class DecompositionService {
	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	DecompositionRepository decompositionRepository;

	@Autowired
	HistoryService historyService;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	GridFsService gridFsService;

	public void createDecomposition(DecompositionRequest request) throws Exception {
		createDecomposition(getSimilarity(request.getSimilarityId()), request);
	}

	public void createDecomposition(Similarity similarity, DecompositionRequest request) throws Exception {
		Clustering clustering = similarity.getClustering();
		Decomposition decomposition = clustering.generateDecomposition(similarity, request,
				decompositionRepository.findNamesBySimilarityId(similarity.getId()));

		setupDecomposition(decomposition);
	}

	public void createExpertDecomposition(String similarityId, String expertName, Optional<MultipartFile> expertFile) throws Exception {
		Similarity similarity = getSimilarity(similarityId);
		Expert expert = new Expert();

		Decomposition decomposition = expert.generateClusters(similarity, expertName, expertFile,
				decompositionRepository.findNamesBySimilarityId(similarityId));

		setupDecomposition(decomposition);
	}

	public void setupDecomposition(Decomposition decomposition) throws Exception {
		decomposition.setup();
		decomposition.calculateMetrics();

		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
		similarityRepository.save(decomposition.getSimilarity());
	}

	public Decomposition updateDecomposition(String decompositionId) throws Exception {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		if (decomposition.isOutdated()) {
			decomposition.update();
			decomposition.calculateMetrics();
			decomposition.setOutdated(false);
			decompositionRepository.save(decomposition);
		}
		return decomposition;
	}

	public Decomposition getDecomposition(String decompositionId) {
		return decompositionRepository.getById(decompositionId);
	}

	public void exportDecompositionToContextMapper(String decompositionId, ZipOutputStream zipOutputStream) throws IOException, JSONException {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);

		zipOutputStream.putNextEntry(new ZipEntry("m2m_decomposition.json"));
		zipOutputStream.write(new ContextMapperContractBuilder(decomposition)
				.addStructureRepresentationData(gridFsService.getFile(
						representationRepository.findBySoftwareSystemIdAndType(decomposition.getStrategy().getSoftwareSystem().getId(), STRUCTURE)
								.orElseThrow(() -> new RuntimeException("No representation with type " + STRUCTURE))
								.getId()))
				.addSagaRefactorizationData(gridFsService.getFile(decompositionId + "_refactorization"))
				.buildContract()
				.getBytes());
		zipOutputStream.closeEntry();
		zipOutputStream.close();
	}

	public void deleteSingleDecomposition(String decompositionId) {
		deleteDecomposition(decompositionRepository.getById(decompositionId));
	}

	public void deleteDecomposition(Decomposition decomposition) {
		decomposition.deleteProperties();
		historyService.deleteHistory(decomposition.getHistory());
		decompositionRepository.deleteById(decomposition.getId());
	}

	public void mergeClustersOperation(String decompositionId, MergeOperation operation) {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		decomposition.mergeClusters(operation);
		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
	}

	public void renameClusterOperation(String decompositionId, RenameOperation operation) {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		decomposition.renameCluster(operation);
		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
	}

	public void splitClusterOperation(String decompositionId, SplitOperation operation) {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		decomposition.splitCluster(operation);
		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
	}

	public void transferEntitiesOperation(String decompositionId, TransferOperation operation) {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		decomposition.transferEntities(operation);
		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
	}

	public void formClusterOperation(String decompositionId, FormClusterOperation operation) {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		decomposition.formCluster(operation);
		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
	}

	public String getEdgeWeights(String decompositionId, String representationInfo) throws Exception {
		Decomposition clusterView = decompositionRepository.getById(decompositionId);
		return clusterView.getEdgeWeights(representationInfo);
	}

	public String getSearchItems(String decompositionId, String representationInfo) throws Exception {
		Decomposition clusterView = decompositionRepository.getById(decompositionId);
		return clusterView.getSearchItems(representationInfo);
	}

	public void snapshotDecomposition(String decompositionId) throws Exception {
		Decomposition decomposition = updateDecomposition(decompositionId);
		Set<String> decompositionNames = decompositionRepository.findNamesBySimilarityId(decomposition.getSimilarity().getId());

		String snapshotName = decomposition.getName() + " SNAPSHOT";
		if (decompositionNames.contains(snapshotName)) {
			int i = 1;
			do {
				i++;
			} while (decompositionNames.contains(snapshotName + "(" + i + ")"));
			snapshotName = snapshotName + "(" + i + ")";
		}

		Decomposition snapshotDecomposition = decomposition.snapshotDecomposition(snapshotName);

		decompositionRepository.save(snapshotDecomposition);
	}

	private Similarity getSimilarity(String similarityId) {
		return similarityRepository.findById(similarityId).orElseThrow(() -> new RuntimeException("No similarity " + similarityId + " found."));
	}
}
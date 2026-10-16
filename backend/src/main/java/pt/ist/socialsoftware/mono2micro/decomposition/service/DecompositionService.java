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
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.StrategyRepository;
import pt.ist.socialsoftware.mono2micro.utils.export.ContextMapperContractBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

@Service
public class DecompositionService {
	@Autowired
	StrategyRepository strategyRepository;

	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	DecompositionRepository decompositionRepository;

	@Autowired
	HistoryService historyService;

	@Autowired
	GridFsService gridFsService;

	public void createDecomposition(DecompositionRequest request) throws Exception {
		Similarity similarity = getSimilarity(request.getSimilarityId());

		Clustering clustering = similarity.getClustering();
		Decomposition decomposition = clustering.generateDecomposition(similarity, request);

		setupDecomposition(decomposition);
	}

	public void createExpertDecomposition(String similarityId, String expertName, Optional<MultipartFile> expertFile) throws Exception {
		Similarity similarity = getSimilarity(similarityId);
		Expert expert = new Expert();

		Decomposition decomposition = expert.generateClusters(similarity, expertName, expertFile);

		setupDecomposition(decomposition);
	}

	public void setupDecomposition(Decomposition decomposition) throws Exception {
		decomposition.setup();
		decomposition.calculateMetrics();

		historyService.saveHistory(decomposition.getHistory());
		decompositionRepository.save(decomposition);
		similarityRepository.save(decomposition.getSimilarity());
		strategyRepository.save(decomposition.getStrategy());
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
						decomposition.getStrategy().getSoftwareSystem().getRepresentationByFileType(STRUCTURE).getId()))
				.addSagaRefactorizationData(gridFsService.getFile(decompositionId + "_refactorization"))
				.buildContract()
				.getBytes());
		zipOutputStream.closeEntry();
		zipOutputStream.close();
	}

	public void deleteSingleDecomposition(String decompositionId) {
		Decomposition decomposition = decompositionRepository.getById(decompositionId);
		decomposition.deleteProperties();

		Strategy strategy = decomposition.getStrategy();
		strategy.removeDecomposition(decomposition.getName());
		Similarity similarity = decomposition.getSimilarity();
		similarity.removeDecomposition(decomposition.getName());

		similarityRepository.save(similarity);
		historyService.deleteHistory(decomposition.getHistory());
		strategyRepository.save(strategy);
		decompositionRepository.deleteById(decomposition.getId());
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
		Similarity similarity = decomposition.getSimilarity();

		String snapshotName = decomposition.getName() + " SNAPSHOT";
		if (similarity.getDecompositionByName(snapshotName) != null) {
			int i = 1;
			do {
				i++;
			} while (similarity.getDecompositionByName(snapshotName + "(" + i + ")") != null);
			snapshotName = snapshotName + "(" + i + ")";
		}

		Decomposition snapshotDecomposition = decomposition.snapshotDecomposition(snapshotName);

		decompositionRepository.save(snapshotDecomposition);
		similarityRepository.save(similarity);
		strategyRepository.save(similarity.getStrategy());
	}

	private Similarity getSimilarity(String similarityId) {
		return similarityRepository.findById(similarityId).orElseThrow(() -> new RuntimeException("No similarity " + similarityId + " found."));
	}
}
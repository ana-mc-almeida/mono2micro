package pt.ist.socialsoftware.mono2micro.operation.characterization;

import org.springframework.test.util.ReflectionTestUtils;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.DecompositionRepository;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.history.repository.HistoryRepository;
import pt.ist.socialsoftware.mono2micro.history.service.HistoryService;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The architect's side of a manual edit: the five operation requests, undo, redo, and where the
 * history stands, issued the way the REST controllers issue them.
 *
 * <p>Each request is the base {@code *Operation} a controller binds from JSON, handed to the
 * same {@code DecompositionService} method the controller calls; undo and redo go through the
 * real {@code HistoryService}. Only the two repositories are mocks: {@code getById} returns the
 * decomposition under test and every {@code save} does nothing. So a test written against this
 * says what an edit does to a decomposition and never which class did it.
 *
 * <p>That is the point. Tickets 05, 11 and 12 replace the code behind these calls (the
 * double dispatch, then the whole {@code History}), and the tests built on this are how they show
 * nothing changed. When the entry points move, this class is the one place to follow them.
 */
final class ManualEditing {

	/** The id every request addresses. */
	static final String DECOMPOSITION_ID = "decomposition-under-test";

	private final Decomposition decomposition;
	private final DecompositionService decompositionService = new DecompositionService();
	private final HistoryService historyService = new HistoryService();

	ManualEditing(Decomposition decomposition) {
		this.decomposition = decomposition;

		DecompositionRepository decompositionRepository = mock(DecompositionRepository.class);
		when(decompositionRepository.getById(DECOMPOSITION_ID)).thenReturn(decomposition);
		HistoryRepository historyRepository = mock(HistoryRepository.class);

		ReflectionTestUtils.setField(historyService, "decompositionRepository", decompositionRepository);
		ReflectionTestUtils.setField(historyService, "historyRepository", historyRepository);
		ReflectionTestUtils.setField(decompositionService, "decompositionRepository", decompositionRepository);
		ReflectionTestUtils.setField(decompositionService, "historyService", historyService);
	}

	ManualEditing merge(String cluster1, String cluster2, String newName) {
		MergeOperation request = new MergeOperation();
		request.setCluster1Name(cluster1);
		request.setCluster2Name(cluster2);
		request.setNewName(newName);
		decompositionService.mergeClustersOperation(DECOMPOSITION_ID, request);
		return this;
	}

	/** {@code entities} is the comma-separated ID list the request carries, e.g. {@code "1,2"}. */
	ManualEditing split(String originalCluster, String newCluster, String entities) {
		SplitOperation request = new SplitOperation();
		request.setOriginalCluster(originalCluster);
		request.setNewCluster(newCluster);
		request.setEntities(entities);
		decompositionService.splitClusterOperation(DECOMPOSITION_ID, request);
		return this;
	}

	/** {@code entities} is the comma-separated ID list the request carries, e.g. {@code "1,2"}. */
	ManualEditing transfer(String fromCluster, String toCluster, String entities) {
		TransferOperation request = new TransferOperation();
		request.setFromCluster(fromCluster);
		request.setToCluster(toCluster);
		request.setEntities(entities);
		decompositionService.transferEntitiesOperation(DECOMPOSITION_ID, request);
		return this;
	}

	ManualEditing rename(String clusterName, String newClusterName) {
		RenameOperation request = new RenameOperation();
		request.setClusterName(clusterName);
		request.setNewClusterName(newClusterName);
		decompositionService.renameClusterOperation(DECOMPOSITION_ID, request);
		return this;
	}

	/** {@code entities} maps each source cluster to the entity IDs taken from it. */
	ManualEditing formCluster(String newCluster, Map<String, List<Short>> entities) {
		FormClusterOperation request = new FormClusterOperation();
		request.setNewCluster(newCluster);
		request.setEntities(entities);
		decompositionService.formClusterOperation(DECOMPOSITION_ID, request);
		return this;
	}

	ManualEditing undo() {
		historyService.undoOperation(decomposition);
		return this;
	}

	ManualEditing redo() {
		historyService.redoOperation(decomposition);
		return this;
	}

	/** How many edits are applied: 0 before the first, and after undoing them all. */
	long position() {
		return decomposition.getHistory().getCurrentHistoryOperationDepth();
	}

	/** How many edits can be reached by redo from the start, the applied ones included. */
	int reachable() {
		return decomposition.getHistory().getMaxHistoryDepth();
	}

	boolean canUndo() {
		return historyService.canUndoRedo(decomposition).get("undo");
	}

	boolean canRedo() {
		return historyService.canUndoRedo(decomposition).get("redo");
	}
}

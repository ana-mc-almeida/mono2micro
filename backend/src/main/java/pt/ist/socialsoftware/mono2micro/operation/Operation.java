package pt.ist.socialsoftware.mono2micro.operation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.history.domain.History;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = RenameOperation.class, name = RenameOperation.RENAME_OPERATION),
		@JsonSubTypes.Type(value = MergeOperation.class, name = MergeOperation.MERGE_OPERATION),
		@JsonSubTypes.Type(value = SplitOperation.class, name = SplitOperation.SPLIT_OPERATION),
		@JsonSubTypes.Type(value = TransferOperation.class, name = TransferOperation.TRANSFER_OPERATION),
		@JsonSubTypes.Type(value = FormClusterOperation.class, name = FormClusterOperation.FORM_CLUSTER_OPERATION),
})
public abstract class Operation {
	protected Long historyDepth;

	public abstract String getOperationType();

	/** Applies this operation to {@code decomposition} and records it as the newest edit of its history. */
	public void execute(ClusterRepresentation decomposition) {
		executeOperation(decomposition);

		History history = decomposition.getHistory();

		Long newHistoryOperationDepth = history.incrementCurrentHistoryDepth();
		this.setHistoryDepth(newHistoryOperationDepth);

		// Remove conflicting Decomposition operations that have been overridden by the new operation
		history.removeOverriddenOperations(newHistoryOperationDepth);

		history.addOperation(this);
	}

	/** Applies this operation to {@code decomposition} through its handler (D-014), without touching the history. */
	public void executeOperation(ClusterRepresentation decomposition) {
		ManualOperationHandlers.scanned().apply(this, decomposition);
	}

	/** Reverts this operation on {@code decomposition} through its handler (D-014), without touching the history. */
	public void undo(ClusterRepresentation decomposition) {
		ManualOperationHandlers.scanned().undo(this, decomposition);
	}

	public void redo(ClusterRepresentation decomposition) { executeOperation(decomposition); }

	public Long getHistoryDepth() {
		return historyDepth;
	}

	public void setHistoryDepth(Long historyDepth) {
		this.historyDepth = historyDepth;
	}
}

package pt.ist.socialsoftware.mono2micro.operation.unit;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandlers;
import pt.ist.socialsoftware.mono2micro.operation.Operation;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The handler registry (D-014): a manual operation finds its code by (operation, cluster kind), a kind without a handler falls back to its
 * superclass's, and the boot check fails when some pair is covered neither way.
 *
 * <p>The cluster kinds and handlers here are inner classes, which the self-registration scan skips, so they never reach the real registry.
 */
public class ManualOperationHandlersTest {

	/** A cluster kind with no handlers of its own. */
	class ChildPartitions extends PartitionsDecomposition {
	}

	/** Records which (operation, decomposition) pairs it was asked to apply or undo. */
	class RecordingHandler<D extends Decomposition> implements ManualOperationHandler<RenameOperation, D> {
		private final Class<D> kind;
		final List<String> calls = new ArrayList<>();

		RecordingHandler(Class<D> kind) {
			this.kind = kind;
		}

		@Override
		public Class<RenameOperation> operation() {
			return RenameOperation.class;
		}

		@Override
		public Class<D> clusterKind() {
			return kind;
		}

		@Override
		public void apply(RenameOperation operation, D decomposition) {
			calls.add("apply " + operation.getClusterName() + " on " + decomposition.getClass().getSimpleName());
		}

		@Override
		public void undo(RenameOperation operation, D decomposition) {
			calls.add("undo " + operation.getClusterName() + " on " + decomposition.getClass().getSimpleName());
		}
	}

	private static RenameOperation rename(String clusterName) {
		RenameOperation operation = new RenameOperation();
		operation.setClusterName(clusterName);
		return operation;
	}

	@Test
	public void aKindWithItsOwnHandlerUsesIt() {
		RecordingHandler<PartitionsDecomposition> partitions = new RecordingHandler<>(PartitionsDecomposition.class);
		RecordingHandler<ChildPartitions> child = new RecordingHandler<>(ChildPartitions.class);
		ManualOperationHandlers registry = new ManualOperationHandlers(Arrays.asList(partitions, child));

		registry.apply(rename("C1"), new ChildPartitions());
		registry.undo(rename("C1"), new PartitionsDecomposition());

		assertThat(child.calls).containsExactly("apply C1 on ChildPartitions");
		assertThat(partitions.calls).containsExactly("undo C1 on PartitionsDecomposition");
	}

	@Test
	public void aKindWithoutAHandlerFallsBackToItsSuperclasssHandler() {
		RecordingHandler<PartitionsDecomposition> partitions = new RecordingHandler<>(PartitionsDecomposition.class);
		ManualOperationHandlers registry = new ManualOperationHandlers(Collections.singletonList(partitions));

		registry.apply(rename("C1"), new ChildPartitions());
		registry.undo(rename("C2"), new ChildPartitions());

		assertThat(partitions.calls).containsExactly("apply C1 on ChildPartitions", "undo C2 on ChildPartitions");
	}

	@Test
	public void anOperationWithNoHandlerUpTheHierarchyIsRefused() {
		ManualOperationHandlers registry = new ManualOperationHandlers(Collections.singletonList(new RecordingHandler<>(ChildPartitions.class)));

		assertThatThrownBy(() -> registry.apply(rename("C1"), new PartitionsDecomposition()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RenameOperation")
				.hasMessageContaining("PartitionsDecomposition");
	}

	@Test
	public void twoHandlersForOnePairAreRefused() {
		List<RecordingHandler<PartitionsDecomposition>> twins = Arrays.asList(
				new RecordingHandler<>(PartitionsDecomposition.class), new RecordingHandler<>(PartitionsDecomposition.class));

		assertThatThrownBy(() -> new ManualOperationHandlers(twins))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("RenameOperation");
	}

	@Test
	public void theCheckFailsWhenAnOperationHasNoHandlerForAKind() {
		ManualOperationHandlers registry = new ManualOperationHandlers(Collections.singletonList(new RecordingHandler<>(PartitionsDecomposition.class)));

		assertThatThrownBy(() -> registry.verify(Arrays.asList(RenameOperation.class, MergeOperation.class), kinds(PartitionsDecomposition.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("MergeOperation")
				.hasMessageContaining("PartitionsDecomposition");
	}

	@Test
	public void theCheckAcceptsAKindCoveredByFallback() {
		ManualOperationHandlers registry = new ManualOperationHandlers(Collections.singletonList(new RecordingHandler<>(PartitionsDecomposition.class)));

		assertThatCode(() -> registry.verify(Collections.singletonList(RenameOperation.class),
				kinds(PartitionsDecomposition.class, ChildPartitions.class)))
				.doesNotThrowAnyException();
	}

	@Test
	public void theScannedHandlersCoverEveryManualOperationForEveryClusterKind() {
		assertThat(SelfRegistration.types(Operation.class)).containsExactlyInAnyOrder(
				FormClusterOperation.class, MergeOperation.class, RenameOperation.class, SplitOperation.class, TransferOperation.class);

		assertThatCode(() -> ManualOperationHandlers.scanned().verify(SelfRegistration.types(Operation.class), SelfRegistration.types(Decomposition.class)))
				.doesNotThrowAnyException();
	}

	@SafeVarargs
	private static List<Class<? extends Decomposition>> kinds(Class<? extends Decomposition>... kinds) {
		return Arrays.asList(kinds);
	}
}

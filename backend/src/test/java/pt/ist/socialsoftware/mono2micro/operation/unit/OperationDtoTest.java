package pt.ist.socialsoftware.mono2micro.operation.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.DecompositionFixture;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The request-shaped side of the operations: type strings, and the request a controller binds
 * being executable as it is.
 *
 * <p>Not setter padding. This is the production path: a controller binds JSON to the base
 * {@code *Operation} and executes it, and the operation finds its handler in the registry
 * (D-014). Merge's undo state is read-only in JSON, so a request cannot set it, which is why
 * {@code storeState} captures it on {@code execute} rather than it being carried in.
 *
 * <p>The type strings are the discriminators Jackson writes into the persisted history
 * ({@code Operation}'s {@code @JsonSubTypes}). They are transcribed as literals here rather than referenced,
 * so that renaming a constant shows up as a red test rather than a silent recompile — the
 * convention {@code SimilarityCase} uses for the same reason. Changing one is a migration, since
 * histories already in Mongo carry the old spelling.
 */
@DisplayName("Operation request objects")
class OperationDtoTest {

	@Test
	@DisplayName("type strings match the persisted discriminators")
	void typeStrings() {
		assertThat(new RenameOperation().getOperationType()).isEqualTo("RenameOperation");
		assertThat(new MergeOperation().getOperationType()).isEqualTo("MergeOperation");
		assertThat(new SplitOperation().getOperationType()).isEqualTo("SplitOperation");
		assertThat(new TransferOperation().getOperationType()).isEqualTo("TransferOperation");
		assertThat(new FormClusterOperation().getOperationType()).isEqualTo("FormClusterOperation");
	}

	@Test
	@DisplayName("a rename request executes as bound")
	void renameRoundTrip() {
		RenameOperation request = new RenameOperation();
		request.setClusterName("C1");
		request.setNewClusterName("Renamed");

		PartitionsDecomposition decomposition = DecompositionFixture.with().cluster("C1", 1).build();
		request.executeOperation(decomposition);
		assertThat(decomposition.getClusters()).containsOnlyKeys("Renamed");
	}

	@Test
	@DisplayName("a split request executes as bound")
	void splitRoundTrip() {
		SplitOperation request = new SplitOperation();
		request.setOriginalCluster("C1");
		request.setNewCluster("C2");
		request.setEntities("1");

		PartitionsDecomposition decomposition = DecompositionFixture.with().cluster("C1", 1, 2).build();
		request.executeOperation(decomposition);
		assertThat(decomposition.getCluster("C2").getElementsIDs()).containsExactly((short) 1);
	}

	@Test
	@DisplayName("a transfer request executes as bound")
	void transferRoundTrip() {
		TransferOperation request = new TransferOperation();
		request.setFromCluster("C1");
		request.setToCluster("C2");
		request.setEntities("1");

		PartitionsDecomposition decomposition = DecompositionFixture.with()
				.cluster("C1", 1)
				.cluster("C2", 2)
				.build();
		request.executeOperation(decomposition);
		assertThat(decomposition.getCluster("C2").getElementsIDs())
				.containsExactlyInAnyOrder((short) 1, (short) 2);
	}

	@Test
	@DisplayName("a form cluster request executes as bound")
	void formClusterRoundTrip() {
		FormClusterOperation request = new FormClusterOperation();
		request.setNewCluster("NC");
		request.setEntities(Collections.singletonMap("C1", Arrays.asList((short) 1)));

		PartitionsDecomposition decomposition = DecompositionFixture.with().cluster("C1", 1, 2).build();
		request.executeOperation(decomposition);
		assertThat(decomposition.getCluster("NC").getElementsIDs()).containsExactly((short) 1);
	}

	/**
	 * {@code cluster1Entities} and {@code cluster2Entities} are not request fields: they are undo
	 * state, captured by {@code storeState} during {@code execute} against the decomposition as it
	 * stands, and only when still null. A request that could set them would skip the capture and
	 * undo to whatever it carried, so JSON reads them as read-only.
	 */
	@Test
	@DisplayName("a merge request binds the names but not the undo state")
	void mergeRequestBindsNamesOnly() throws Exception {
		MergeOperation request = new ObjectMapper().readValue("{\"type\": \"MergeOperation\", \"cluster1Name\": \"C1\", \"cluster2Name\": \"C2\","
				+ " \"newName\": \"M\", \"cluster1Entities\": \"stale\", \"cluster2Entities\": \"stale\"}", MergeOperation.class);

		assertThat(request.getCluster1Name()).isEqualTo("C1");
		assertThat(request.getCluster2Name()).isEqualTo("C2");
		assertThat(request.getNewName()).isEqualTo("M");
		assertThat(request.getCluster1Entities()).isNull();
		assertThat(request.getCluster2Entities()).isNull();
	}

	@Test
	@DisplayName("merge captures the pre-merge membership on execute, once")
	void mergeCapturesStateOnExecute() {
		PartitionsDecomposition decomposition = DecompositionFixture.with()
				.cluster("C1", 1, 2)
				.cluster("C2", 3)
				.build();

		MergeOperation operation = new MergeOperation("C1", "C2", "M");
		operation.execute(decomposition);

		// Captured as a comma-separated entity list, in whatever order the set yields.
		assertThat(operation.getCluster1Entities().split(",")).containsExactlyInAnyOrder("1", "2");
		assertThat(operation.getCluster2Entities()).isEqualTo("3");
	}
}

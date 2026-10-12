package pt.ist.socialsoftware.mono2micro.operation.characterization;

import pt.ist.socialsoftware.mono2micro.operation.DecompositionFixture;
import pt.ist.socialsoftware.mono2micro.operation.DecompositionSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.functionality.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.functionality.domain.FunctionalityRedesign;
import pt.ist.socialsoftware.mono2micro.functionality.domain.LocalTransaction;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What each manual operation does to a decomposition, and what undo and redo give back, pinned as
 * the architect sees it: the resulting clusters, the functionalities and their redesigns, and
 * where the history stands.
 *
 * <p>Characterization for tickets 05, 11 and 12, which replace everything behind these calls.
 * The tests go through {@link ManualEditing}, which issues requests the way the controllers do,
 * so nothing here names an operation class. Ticket 05 should pass them unchanged; tickets 11 and
 * 12 port them by changing {@code ManualEditing}, not the assertions, with one exception below.
 * The per-operation {@code *PartitionsOperationTest} classes pin the cluster arithmetic in more
 * detail, coupling dependencies included; this class pins one worked example per operation end
 * to end.
 *
 * <p>Every test starts from the same decomposition, with the real {@code AccessesInformation}
 * attached (its hooks run offline through {@link StubContextManager} and
 * {@link InMemoryFunctionalityService}):
 *
 * <pre>
 * C1 = {1, 2} C2 = {3, 4} C3 = {5}
 *
 * F12 touches 1 (C1) and 3 (C2)
 * F1 touches 2 (C1)
 * F3 touches 5 (C3)
 * F23 touches 4 (C2) and 5 (C3)
 * </pre>
 *
 * <p>Each functionality has one redesign, the default one, with a local transaction per cluster
 * it touches.
 *
 * <p><b>The functionality rule today.</b> Rename edits functionalities in place: the renamed
 * cluster's key in {@code entitiesPerCluster} and every local transaction in that cluster, in
 * every redesign. Every other operation keeps a functionality untouched if it touches none of the
 * entities the operation reports as affected, and otherwise drops it, deleting its redesigns,
 * and marks the decomposition outdated. An outdated decomposition recomputes its missing
 * functionalities from the traces when {@code DecompositionService.updateDecomposition} next runs
 * on it (not on every read: {@code getDecomposition} does not), which is not reached offline;
 * "outdated" is what promises it here.
 *
 * <p><b>Known defect: undo does not restore dropped functionalities.</b> Undo replays the inverse
 * operation, which drops again rather than restoring, so after undo the clusters are back but the
 * functionalities dropped by the edit are still gone, their hand-made redesigns with them, and the
 * decomposition is still outdated. Undoing a split even drops more than the split did. Rename is
 * the exception, since its inverse is another rename.
 *
 * <p>The tests whose display name starts with "today" pin the defect, kept apart from the cluster
 * and position assertions. Each has a {@code @Disabled} twin asserting the correct outcome: undo
 * gives back the edit's input, functionalities and redesigns included, and redo gives back its
 * output. The editing chain does exactly that (tickets 11 and 12), so ticket 12 enables the twins
 * and deletes the "today" tests. See {@code ../implementation/docs/gap-analysis.md}, Gap 7.
 */
@DisplayName("Manual operations, as the architect sees them")
class ManualOperationCharacterizationTest {

	private static final String UNDO_DEFECT = "Known defect: undo replays the inverse operation, which drops "
			+ "functionalities instead of restoring them. Ticket 12 fixes it. See ../implementation/docs/gap-analysis.md";

	private static final String ORIGINAL_CLUSTERS = "{C1=[1, 2], C2=[3, 4], C3=[5]}";

	private static final String F12 = "F12 {C1=[1], C2=[3]} [1: C1, 2: C2]";
	private static final String F1 = "F1 {C1=[2]} [1: C1]";
	private static final String F3 = "F3 {C3=[5]} [1: C3]";
	private static final String F23 = "F23 {C2=[4], C3=[5]} [1: C2, 2: C3]";

	private InMemoryFunctionalityService functionalityService;
	private AccessesInformation accessesInformation;
	private PartitionsDecomposition decomposition;
	private ManualEditing editing;

	@BeforeEach
	void setUp() throws IOException {
		functionalityService = new InMemoryFunctionalityService();
		StubContextManager.install(functionalityService);

		accessesInformation = new AccessesInformation();
		addFunctionality("F12", "C1", 1, "C2", 3);
		addFunctionality("F1", "C1", 2);
		addFunctionality("F3", "C3", 5);
		addFunctionality("F23", "C2", 4, "C3", 5);

		decomposition = DecompositionFixture.with()
				.cluster("C1", 1, 2)
				.cluster("C2", 3, 4)
				.cluster("C3", 5)
				.representationInformation(accessesInformation)
				.build();
		editing = new ManualEditing(decomposition);
	}

	@AfterEach
	void tearDown() {
		StubContextManager.uninstall();
	}

	@Test
	@DisplayName("the starting point")
	void startingPoint() {
		assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
		assertOriginalFunctionalities();
		assertPosition(0, 0);
	}

	@Nested
	@DisplayName("merge C1 and C2 into M")
	class Merge {

		private static final String MERGED = "{C3=[5], M=[1, 2, 3, 4]}";

		@BeforeEach
		void merge() {
			editing.merge("C1", "C2", "M");
		}

		@Test
		@DisplayName("one cluster holds both clusters' entities")
		void resultingClusters() {
			assertThat(clusters()).isEqualTo(MERGED);
		}

		@Test
		@DisplayName("drops every functionality touching the merged cluster, and its redesigns")
		void resultingFunctionalities() {
			assertThat(functionalities()).containsExactly(F3);
			assertThat(functionalityService.redesignFileNames()).containsExactly(redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@DisplayName("is one step of history")
		void historyPosition() {
			assertPosition(1, 1);
		}

		@Test
		@DisplayName("undo restores the clusters")
		void undo() {
			editing.undo();

			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertPosition(0, 1);
		}

		/** Pins the known defect; see the class javadoc. */
		@Test
		@DisplayName("today, undo leaves the dropped functionalities dropped")
		void undoLeavesDroppedFunctionalitiesDropped() {
			editing.undo();

			assertThat(functionalities()).containsExactly(F3);
			assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@Disabled(UNDO_DEFECT)
		@DisplayName("undo restores the dropped functionalities and their redesigns")
		void undoRestoresDroppedFunctionalities() {
			editing.undo();

			assertOriginalFunctionalities();
		}

		@Test
		@DisplayName("redo merges again")
		void redo() {
			editing.undo().redo();

			assertThat(clusters()).isEqualTo(MERGED);
			assertThat(functionalities()).containsExactly(F3);
			assertPosition(1, 1);
		}
	}

	/**
	 * Undo replays a merge of N back into C1, and that merge drops every functionality touching
	 * C1's entities: so today undo drops F12, which the split itself had kept.
	 */
	@Nested
	@DisplayName("split entity 2 off C1 into N")
	class Split {

		private static final String SPLIT = "{C1=[1], C2=[3, 4], C3=[5], N=[2]}";

		@BeforeEach
		void split() {
			editing.split("C1", "N", "2");
		}

		@Test
		@DisplayName("the new cluster holds the entities split off")
		void resultingClusters() {
			assertThat(clusters()).isEqualTo(SPLIT);
		}

		@Test
		@DisplayName("drops every functionality touching the new cluster, and its redesigns")
		void resultingFunctionalities() {
			assertThat(functionalities()).containsExactly(F12, F23, F3);
			assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(
					redesignFile("F12"), redesignFile("F23"), redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@DisplayName("is one step of history")
		void historyPosition() {
			assertPosition(1, 1);
		}

		@Test
		@DisplayName("undo restores the clusters")
		void undo() {
			editing.undo();

			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertPosition(0, 1);
		}

		/** Pins the known defect; see the class javadoc. */
		@Test
		@DisplayName("today, undo leaves the dropped functionalities dropped")
		void undoLeavesDroppedFunctionalitiesDropped() {
			editing.undo();

			// F12 too, which the split itself had kept.
			assertThat(functionalities()).containsExactly(F23, F3);
			assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(redesignFile("F23"), redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@Disabled(UNDO_DEFECT)
		@DisplayName("undo restores the dropped functionalities and their redesigns")
		void undoRestoresDroppedFunctionalities() {
			editing.undo();

			assertOriginalFunctionalities();
		}

		@Test
		@DisplayName("redo splits again")
		void redo() {
			editing.undo().redo();

			assertThat(clusters()).isEqualTo(SPLIT);
			assertPosition(1, 1);
		}

		/** Pins the known defect; see the class javadoc. */
		@Test
		@DisplayName("today, redo does not bring back what undo dropped")
		void redoLeavesUndoDropsDropped() {
			editing.undo().redo();

			assertThat(functionalities()).containsExactly(F23, F3);
		}

		@Test
		@Disabled(UNDO_DEFECT)
		@DisplayName("redo restores what the split kept")
		void redoRestoresWhatTheSplitKept() {
			editing.undo().redo();

			assertThat(functionalities()).containsExactly(F12, F23, F3);
		}
	}

	@Nested
	@DisplayName("transfer entity 4 from C2 to C3")
	class Transfer {

		private static final String TRANSFERRED = "{C1=[1, 2], C2=[3], C3=[4, 5]}";

		@BeforeEach
		void transfer() {
			editing.transfer("C2", "C3", "4");
		}

		@Test
		@DisplayName("the entity moves to the target cluster")
		void resultingClusters() {
			assertThat(clusters()).isEqualTo(TRANSFERRED);
		}

		@Test
		@DisplayName("drops every functionality touching a transferred entity, and its redesigns")
		void resultingFunctionalities() {
			assertThat(functionalities()).containsExactly(F1, F12, F3);
			assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(
					redesignFile("F1"), redesignFile("F12"), redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@DisplayName("is one step of history")
		void historyPosition() {
			assertPosition(1, 1);
		}

		@Test
		@DisplayName("undo restores the clusters")
		void undo() {
			editing.undo();

			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertPosition(0, 1);
		}

		/** Pins the known defect; see the class javadoc. */
		@Test
		@DisplayName("today, undo leaves the dropped functionalities dropped")
		void undoLeavesDroppedFunctionalitiesDropped() {
			editing.undo();

			assertThat(functionalities()).containsExactly(F1, F12, F3);
			assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(redesignFile("F1"), redesignFile("F12"), redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@Disabled(UNDO_DEFECT)
		@DisplayName("undo restores the dropped functionalities and their redesigns")
		void undoRestoresDroppedFunctionalities() {
			editing.undo();

			assertOriginalFunctionalities();
		}

		@Test
		@DisplayName("redo transfers again")
		void redo() {
			editing.undo().redo();

			assertThat(clusters()).isEqualTo(TRANSFERRED);
			assertThat(functionalities()).containsExactly(F1, F12, F3);
			assertPosition(1, 1);
		}
	}

	@Nested
	@DisplayName("rename C2 to R")
	class Rename {

		private static final String RENAMED = "{C1=[1, 2], C3=[5], R=[3, 4]}";

		private static final String F12_RENAMED = "F12 {C1=[1], R=[3]} [1: C1, 2: R]";
		private static final String F23_RENAMED = "F23 {C3=[5], R=[4]} [1: R, 2: C3]";

		@BeforeEach
		void rename() {
			editing.rename("C2", "R");
		}

		@Test
		@DisplayName("the cluster keeps its entities under the new name")
		void resultingClusters() {
			assertThat(clusters()).isEqualTo(RENAMED);
		}

		@Test
		@DisplayName("keeps every functionality, renaming the cluster in them and in their redesigns")
		void resultingFunctionalities() {
			assertThat(functionalities()).containsExactly(F1, F12_RENAMED, F23_RENAMED, F3);
			assertThat(functionalityService.redesignFileNames()).hasSize(4);
			assertThat(decomposition.isOutdated()).isFalse();
		}

		@Test
		@DisplayName("is one step of history")
		void historyPosition() {
			assertPosition(1, 1);
		}

		@Test
		@DisplayName("undo restores the clusters and the functionalities")
		void undo() {
			editing.undo();

			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertThat(functionalities()).containsExactly(F1, F12, F23, F3);
			assertThat(decomposition.isOutdated()).isFalse();
			assertPosition(0, 1);
		}

		@Test
		@DisplayName("redo renames again")
		void redo() {
			editing.undo().redo();

			assertThat(clusters()).isEqualTo(RENAMED);
			assertThat(functionalities()).containsExactly(F1, F12_RENAMED, F23_RENAMED, F3);
			assertPosition(1, 1);
		}
	}

	/** Taking every entity of C2 empties it, and an emptied source cluster is removed. */
	@Nested
	@DisplayName("form F from entity 2 of C1 and entities 3 and 4 of C2")
	class FormCluster {

		private static final String FORMED = "{C1=[1], C3=[5], F=[2, 3, 4]}";

		@BeforeEach
		void formCluster() {
			Map<String, List<Short>> entities = new TreeMap<>();
			entities.put("C1", Arrays.asList((short) 2));
			entities.put("C2", Arrays.asList((short) 3, (short) 4));
			editing.formCluster("F", entities);
		}

		@Test
		@DisplayName("the new cluster holds the entities, and the emptied source is gone")
		void resultingClusters() {
			assertThat(clusters()).isEqualTo(FORMED);
		}

		@Test
		@DisplayName("drops every functionality touching a moved entity, and its redesigns")
		void resultingFunctionalities() {
			assertThat(functionalities()).containsExactly(F3);
			assertThat(functionalityService.redesignFileNames()).containsExactly(redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@DisplayName("is one step of history")
		void historyPosition() {
			assertPosition(1, 1);
		}

		@Test
		@DisplayName("undo restores the clusters, the removed one included")
		void undo() {
			editing.undo();

			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertPosition(0, 1);
		}

		/** Pins the known defect; see the class javadoc. */
		@Test
		@DisplayName("today, undo leaves the dropped functionalities dropped")
		void undoLeavesDroppedFunctionalitiesDropped() {
			editing.undo();

			assertThat(functionalities()).containsExactly(F3);
			assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(redesignFile("F3"));
			assertThat(decomposition.isOutdated()).isTrue();
		}

		@Test
		@Disabled(UNDO_DEFECT)
		@DisplayName("undo restores the dropped functionalities and their redesigns")
		void undoRestoresDroppedFunctionalities() {
			editing.undo();

			assertOriginalFunctionalities();
		}

		@Test
		@DisplayName("redo forms the cluster again")
		void redo() {
			editing.undo().redo();

			assertThat(clusters()).isEqualTo(FORMED);
			assertThat(functionalities()).containsExactly(F3);
			assertPosition(1, 1);
		}
	}

	@Nested
	@DisplayName("history")
	class EditHistory {

		private static final String AFTER_SPLIT = Split.SPLIT;
		private static final String AFTER_SPLIT_AND_RENAME = "{C1=[1], C2=[3, 4], N=[2], R=[5]}";
		private static final String AFTER_RENAME = "{C1=[1, 2], C2=[3, 4], R=[5]}";

		@Test
		@DisplayName("undo and redo walk several edits in order")
		void walksSeveralEdits() {
			editing.split("C1", "N", "2").rename("C3", "R");
			assertThat(clusters()).isEqualTo(AFTER_SPLIT_AND_RENAME);
			assertPosition(2, 2);

			editing.undo();
			assertThat(clusters()).isEqualTo(AFTER_SPLIT);
			assertPosition(1, 2);

			editing.undo();
			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertPosition(0, 2);

			editing.redo().redo();
			assertThat(clusters()).isEqualTo(AFTER_SPLIT_AND_RENAME);
			assertPosition(2, 2);
		}

		@Test
		@DisplayName("a new edit after undo discards the undone edits")
		void newEditDiscardsUndoneBranch() {
			editing.split("C1", "N", "2").transfer("C2", "C3", "4").undo();
			assertPosition(1, 2);

			editing.rename("C3", "R");

			assertThat(clusters()).isEqualTo(AFTER_SPLIT_AND_RENAME);
			assertPosition(2, 2);

			// Walking back and forward again never reaches the transfer.
			editing.undo().undo();
			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			editing.redo().redo();
			assertThat(clusters()).isEqualTo(AFTER_SPLIT_AND_RENAME);
			assertPosition(2, 2);
		}

		@Test
		@DisplayName("a new edit after undoing everything starts the history again")
		void newEditAfterUndoingEverything() {
			editing.split("C1", "N", "2").transfer("C2", "C3", "4").undo().undo();
			assertPosition(0, 2);

			editing.rename("C3", "R");

			assertThat(clusters()).isEqualTo(AFTER_RENAME);
			assertPosition(1, 1);
		}

		@Test
		@DisplayName("undo with nothing applied fails and changes nothing")
		void undoAtStartFails() {
			assertThatThrownBy(editing::undo).hasMessage("No more operations to undo");

			assertThat(clusters()).isEqualTo(ORIGINAL_CLUSTERS);
			assertPosition(0, 0);
		}

		@Test
		@DisplayName("redo with nothing undone fails and changes nothing")
		void redoAtEndFails() {
			editing.rename("C3", "R");

			assertThatThrownBy(editing::redo).hasMessage("No more operations to redo");

			assertThat(clusters()).isEqualTo(AFTER_RENAME);
			assertPosition(1, 1);
		}
	}

	/** Asserts every functionality of the starting point is there, redesigns included, and up to date. */
	private void assertOriginalFunctionalities() {
		assertThat(functionalities()).containsExactly(F1, F12, F23, F3);
		assertThat(functionalityService.redesignFileNames()).containsExactlyInAnyOrder(
				redesignFile("F1"), redesignFile("F12"), redesignFile("F23"), redesignFile("F3"));
		assertThat(decomposition.isOutdated()).isFalse();
	}

	/** Asserts the history position: edits applied, and edits reachable by redo. */
	private void assertPosition(long applied, int reachable) {
		assertThat(editing.position()).as("edits applied").isEqualTo(applied);
		assertThat(editing.reachable()).as("edits reachable").isEqualTo(reachable);
		assertThat(editing.canUndo()).as("can undo").isEqualTo(applied > 0);
		assertThat(editing.canRedo()).as("can redo").isEqualTo(applied < reachable);
	}

	/** Cluster name → entity IDs, sorted, as one string so a failure reads as a diff. */
	private String clusters() {
		return DecompositionSnapshot.of(decomposition).clusters().toString();
	}

	/**
	 * One line per functionality, sorted by name: its name, its entities per cluster, and the names
	 * of its default redesign's local transactions (the root excluded), read back from the store.
	 */
	private List<String> functionalities() {
		return accessesInformation.getFunctionalities().values().stream()
				.map(this::describe)
				.sorted()
				.collect(Collectors.toList());
	}

	private String describe(Functionality functionality) {
		Map<String, Set<Short>> entitiesPerCluster = new TreeMap<>();
		functionality.getEntitiesPerCluster().forEach((cluster, entities) -> entitiesPerCluster.put(cluster, new TreeSet<>(entities)));

		List<String> localTransactions;
		try {
			localTransactions = functionalityService.getFunctionalityRedesign(functionality, Constants.DEFAULT_REDESIGN_NAME)
					.getRedesign().stream()
					.filter(localTransaction -> localTransaction.getId() != 0)
					.map(LocalTransaction::getName)
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return functionality.getName() + " " + entitiesPerCluster + " " + localTransactions;
	}

	/**
	 * Adds a functionality touching the given entities, each in the cluster named before it, with a
	 * default redesign holding one local transaction per cluster in the order given.
	 *
	 * @param clustersAndEntities
	 * alternating cluster name and {@code int} entity ID, e.g.
	 * {@code "C1", 1, "C2", 3}
	 */
	private void addFunctionality(String name, Object... clustersAndEntities) throws IOException {
		Functionality functionality = new Functionality(ManualEditing.DECOMPOSITION_NAME, name);
		FunctionalityRedesign redesign = new FunctionalityRedesign(Constants.DEFAULT_REDESIGN_NAME);

		LocalTransaction root = new LocalTransaction(0, "-1");
		root.setName(name);
		root.setRemoteInvocations(new ArrayList<>());
		redesign.getRedesign().add(root);

		for (int i = 0; i < clustersAndEntities.length; i += 2) {
			String cluster = (String) clustersAndEntities[i];
			short entity = (short) (int) clustersAndEntities[i + 1];
			functionality.addEntity(entity, (byte) 1);
			functionality.getEntitiesPerCluster().computeIfAbsent(cluster, key -> new TreeSet<>()).add(entity);

			int id = i / 2 + 1;
			LocalTransaction localTransaction = new LocalTransaction(id, cluster);
			localTransaction.setName(id + ": " + cluster);
			localTransaction.setRemoteInvocations(new ArrayList<>());
			root.addRemoteInvocations(id);
			redesign.getRedesign().add(localTransaction);
		}

		functionality.setFunctionalityRedesignNameUsedForMetrics(redesign.getName());
		functionality.addFunctionalityRedesign(redesign.getName(), redesignFile(name));
		functionalityService.saveFunctionalityRedesign(functionality, redesign);
		accessesInformation.addFunctionality(functionality);
	}

	/** Where the default redesign of the named functionality is stored, as production names it. */
	private static String redesignFile(String functionalityName) {
		return new Functionality(ManualEditing.DECOMPOSITION_NAME, functionalityName).getId() + Constants.DEFAULT_REDESIGN_NAME;
	}
}

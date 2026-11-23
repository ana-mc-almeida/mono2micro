package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms.SciPyOperation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.service.ClusterRepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The editing chain runs an algorithm operation on a Similarity: it saves the output and links it on the operation (D-012, D-016). A
 * representation no operation has as output was collected.
 */
public class EditingChainServiceTest {

	private static final String SIMILARITY = "similarity";

	private final Similarity similarity = mock(Similarity.class);
	private final OperationRepository operationRepository = mock(OperationRepository.class);
	private final ClusterRepresentationRepository clusterRepresentationRepository = mock(ClusterRepresentationRepository.class);
	private final ClusterRepresentationService clusterRepresentationService = mock(ClusterRepresentationService.class);
	private final EditingChainService service = new EditingChainService();

	@BeforeEach
	public void setUp() {
		when(similarity.getId()).thenReturn(SIMILARITY);
		when(similarity.getName()).thenReturn("Accesses");
		when(similarity.getSoftwareSystem()).thenReturn(new SoftwareSystem("system"));

		service.operationRepository = operationRepository;
		service.clusterRepresentationRepository = clusterRepresentationRepository;
		service.clusterRepresentationService = clusterRepresentationService;
	}

	@Test
	public void aRunSavesItsOutputAndTheOperationLinkedToIt() throws Exception {
		when(clusterRepresentationRepository.findNamesBySimilarityId(SIMILARITY)).thenReturn(Collections.emptySet());
		SciPyOperation operation = cutWithoutPython();

		ClusterRepresentation output = service.run(operation, similarity);

		verify(clusterRepresentationService).setupDecomposition(output);
		ArgumentCaptor<Operation> saved = ArgumentCaptor.forClass(Operation.class);
		verify(operationRepository).save(saved.capture());
		assertThat(saved.getValue()).isSameAs(operation);
		assertThat(operation.getOutput()).isSameAs(output);
		assertThat(operation.getSimilarity()).isSameAs(similarity);
		assertThat(operation.isUndone()).isFalse();
		assertThat(output.getSimilarity()).isSameAs(similarity);
	}

	@Test
	public void anOutputIsNamedApartFromTheSimilaritysOthers() throws Exception {
		when(clusterRepresentationRepository.findNamesBySimilarityId(SIMILARITY)).thenReturn(new HashSet<>(Arrays.asList("Accesses N3", "Accesses N3(2)")));

		assertThat(service.run(cutWithoutPython(), similarity).getName()).isEqualTo("Accesses N3(3)");
	}

	@Test
	public void aRepresentationNoOperationProducedWasCollected() {
		ClusterRepresentation produced = new PartitionsClusterRepresentation(similarity);
		ClusterRepresentation imported = new PartitionsClusterRepresentation(similarity);
		when(operationRepository.existsByOutput_Id(produced.getId())).thenReturn(true);

		assertThat(service.isCollected(produced)).isFalse();
		assertThat(service.isCollected(imported)).isTrue();
	}

	/** A SciPy operation cutting into 3 clusters, whose output is built here instead of by the Python service. */
	private static SciPyOperation cutWithoutPython() {
		return new SciPyOperation("N", 3) {
			@Override
			public ClusterRepresentation apply(Similarity input) {
				ClusterRepresentation output = new PartitionsClusterRepresentation(input);
				output.setName(input.getName() + " N3");
				return output;
			}
		};
	}
}

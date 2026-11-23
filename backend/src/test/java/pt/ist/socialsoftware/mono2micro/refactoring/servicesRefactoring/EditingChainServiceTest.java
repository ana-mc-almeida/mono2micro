package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException;
import pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures;
import pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures.Fixture;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms.SciPyOperation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.service.ClusterRepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The editing chain runs an algorithm operation on a Similarity: it saves the output and links it on the operation (D-012, D-016), and refuses an
 * operation the Similarity does not offer before anything is computed (D-003). A representation no operation has as output was collected.
 */
public class EditingChainServiceTest {

	private static final String SIMILARITY = "similarity";

	private final Fixture system = SoftwareSystemFixtures.accessesBased();
	private final Similarity similarity = mock(Similarity.class);
	private final OperationRepository operationRepository = mock(OperationRepository.class);
	private final ClusterRepresentationRepository clusterRepresentationRepository = mock(ClusterRepresentationRepository.class);
	private final ClusterRepresentationService clusterRepresentationService = mock(ClusterRepresentationService.class);
	private final EditingChainService service = new EditingChainService();

	@BeforeEach
	public void setUp() {
		when(similarity.getId()).thenReturn(SIMILARITY);
		when(similarity.getName()).thenReturn("Accesses");
		when(similarity.getKindName()).thenReturn(AccessesKind.ACCESSES);
		when(similarity.getSoftwareSystem()).thenReturn(system.getSoftwareSystem());

		SimilarityRepository similarityRepository = mock(SimilarityRepository.class);
		when(similarityRepository.getById(SIMILARITY)).thenReturn(similarity);

		service.operationRepository = operationRepository;
		service.similarityRepository = similarityRepository;
		service.representationRepository = SoftwareSystemFixtures.repositoryServing(system);
		service.clusterRepresentationRepository = clusterRepresentationRepository;
		service.clusterRepresentationService = clusterRepresentationService;
	}

	@Test
	public void aRunSavesItsOutputAndTheOperationLinkedToIt() throws Exception {
		when(clusterRepresentationRepository.findNamesBySimilarityId(SIMILARITY)).thenReturn(Collections.emptySet());
		SciPyOperation operation = cutWithoutPython();

		ClusterRepresentation output = service.runAlgorithmOperation(SIMILARITY, operation);

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

		assertThat(service.runAlgorithmOperation(SIMILARITY, cutWithoutPython()).getName()).isEqualTo("Accesses N3(3)");
	}

	/** SciPy requires only a Similarity kind; this one also requires a Structure representation, which the Accesses uploads lack. */
	@Test
	public void anOperationTheSimilarityDoesNotOfferIsRefusedBeforeItRuns() {
		SciPyOperation operation = new SciPyOperation("N", 3) {
			@Override
			public List<Requirement> requirements() {
				return Collections.singletonList(Requires.of(StructureRepresentation.class));
			}

			@Override
			public ClusterRepresentation apply(Similarity input) {
				throw new AssertionError("ran an operation the Similarity does not offer");
			}
		};

		assertThatThrownBy(() -> service.runAlgorithmOperation(SIMILARITY, operation))
				.isInstanceOf(UnavailableFeatureException.class)
				.hasMessageContaining("requires a Structure representation");
		verify(operationRepository, never()).save(any());
	}

	@Test
	public void theRecordOffersSciPyForASimilarity() {
		assertThat(service.getAvailableAlgorithmOperations(SIMILARITY).availableNames()).containsExactly(SciPyOperation.SCIPY);
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

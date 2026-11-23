package pt.ist.socialsoftware.mono2micro.similarity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesAndRepositoryKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.ClassVectorizationKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.RepositoryKind;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.RepositoryWeights;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyAccessesAndRepositoryDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyStructureDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.StructureKind;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Which representations a Similarity reads, at the {@link SimilarityService#resolveRepresentations} seam (D-016), and which requests
 * {@link SimilarityService#createSimilarity} refuses before computing anything. No Spring and no Mongo: the software system's
 * representations come from a mocked repository.
 */
public class SimilarityServiceTest {
	private static final String SYSTEM = "system";

	@Mock
	private RepresentationRepository representationRepository;

	@InjectMocks
	private SimilarityService similarityService;

	private final IDToEntityRepresentation idToEntity = new IDToEntityRepresentation();
	private final EntityToIDRepresentation entityToId = new EntityToIDRepresentation();
	private final AccessesRepresentation accesses = new AccessesRepresentation();
	private final AuthorRepresentation author = new AuthorRepresentation();
	private final CommitRepresentation commit = new CommitRepresentation();
	private final CodeEmbeddingsRepresentation codeEmbeddings = new CodeEmbeddingsRepresentation();
	private final StructureRepresentation structure = new StructureRepresentation();

	@BeforeEach
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	/** The family's shared requirements come first, then the kind's own, whatever order the software system holds them in. */
	@Test
	public void aSimilarityReadsTheRepresentationsItsKindRequires() {
		when(representationRepository.findBySoftwareSystemId(SYSTEM))
				.thenReturn(Arrays.asList(structure, codeEmbeddings, commit, author, accesses, entityToId, idToEntity));

		assertThat(similarityService.resolveRepresentations(new AccessesKind(), SYSTEM)).containsExactly(idToEntity, accesses);
		assertThat(similarityService.resolveRepresentations(new RepositoryKind(), SYSTEM)).containsExactly(idToEntity, accesses, author, commit);
		assertThat(similarityService.resolveRepresentations(new AccessesAndRepositoryKind(), SYSTEM))
				.containsExactly(idToEntity, accesses, author, commit);
		assertThat(similarityService.resolveRepresentations(new StructureKind(), SYSTEM))
				.containsExactly(idToEntity, accesses, entityToId, structure);
		assertThat(similarityService.resolveRepresentations(new ClassVectorizationKind(), SYSTEM))
				.containsExactly(idToEntity, accesses, entityToId, codeEmbeddings);
	}

	/** A kind missing one of its representations is refused, never resolved to the ones that are there. */
	@Test
	public void aKindMissingARepresentationIsRefused() {
		when(representationRepository.findBySoftwareSystemId(SYSTEM)).thenReturn(Arrays.asList(idToEntity, accesses, author, structure));

		assertThatThrownBy(() -> similarityService.resolveRepresentations(new RepositoryKind(), SYSTEM))
				.isInstanceOf(UnavailableFeatureException.class);
	}

	/** A kind fixes its criteria (D-013), so weights for another kind's criteria are refused, not computed under the wrong name. */
	@Test
	public void weightsTheKindIsNotComputedWithAreRefused() {
		SimilarityScipyAccessesAndRepositoryDto request = new SimilarityScipyAccessesAndRepositoryDto();
		request.setKindName(AccessesKind.ACCESSES);
		request.setWeightsList(Collections.singletonList(new RepositoryWeights()));

		assertThatThrownBy(() -> similarityService.createSimilarity(SYSTEM, request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The Accesses kind is computed with the weights [ACCESSES_WEIGHTS], not [REPOSITORY_WEIGHTS].");
	}

	@Test
	public void theWeightsOfAnotherKindOfTheSameTypeAreRefused() {
		SimilarityScipyAccessesAndRepositoryDto request = new SimilarityScipyAccessesAndRepositoryDto();
		request.setKindName(AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY);
		request.setWeightsList(Collections.singletonList(new AccessesWeights()));

		assertThatThrownBy(() -> similarityService.createSimilarity(SYSTEM, request)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void aTypeTheKindIsNotComputedByIsRefused() {
		SimilarityScipyStructureDto request = new SimilarityScipyStructureDto();
		request.setKindName(AccessesKind.ACCESSES);

		assertThatThrownBy(() -> similarityService.createSimilarity(SYSTEM, request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("The Accesses kind is computed by ");
	}

	@Test
	public void anUnknownKindIsRefused() {
		SimilarityScipyStructureDto request = new SimilarityScipyStructureDto();
		request.setKindName("Accesses and Structure");

		assertThatThrownBy(() -> similarityService.createSimilarity(SYSTEM, request)).isInstanceOf(IllegalArgumentException.class);
	}
}

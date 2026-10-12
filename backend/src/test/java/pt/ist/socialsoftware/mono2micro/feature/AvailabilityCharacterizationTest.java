package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.SciPyClustering;
import pt.ist.socialsoftware.mono2micro.codebase.CodebaseService;
import pt.ist.socialsoftware.mono2micro.codebase.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.codebase.repository.CodebaseRepository;
import pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures;
import pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.domain.StructureRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.ACCESSES_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.CODE_EMBEDDINGS_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.EMPTY_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.REPOSITORY_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures.STRUCTURE_BASED_NAME;
import static pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.domain.AuthorRepresentation.AUTHOR;
import static pt.ist.socialsoftware.mono2micro.representation.domain.CodeEmbeddingsRepresentation.CODE_EMBEDDINGS;
import static pt.ist.socialsoftware.mono2micro.representation.domain.CommitRepresentation.COMMIT;
import static pt.ist.socialsoftware.mono2micro.representation.domain.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.STRUCTURE_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.StructureRepresentation.STRUCTURE;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.ACCESSES_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.CLASS_VECTORIZATION_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.ENTITY_VECTORIZATION_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.REPOSITORY_STRATEGY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.STRUCTURE_STRATEGY;

/**
 * Pins today's availability answers, so that the shared availability check of D-013 and D-019
 * can be shown to give the same ones.
 *
 * <p>Two questions are characterized: which of the seven Similarity kinds (today's strategy
 * types) a set of uploads makes available, and which of them the SciPy algorithm accepts. Each
 * test asserts the exact answer, offered and withheld alike, and never which map or method
 * produced it. Whatever replaces those internals must keep these answers, except where a test
 * names the decision that changes one.
 *
 * <p>Follows {@code CodebaseServiceAvailabilityTest}: in-memory fixtures, a stubbed repository,
 * no Spring, Mongo or Docker.
 */
public class AvailabilityCharacterizationTest {

	private static final String UNGROUPED_STRUCTURE_NAME = "fixture-structure-without-entity-to-id";
	private static final String EVERYTHING_NAME = "fixture-everything";
	private static final String ACCESSES_ONLY_NAME = "fixture-accesses-only";

	private static final Set<String> VECTORIZATION_KINDS = Set.of(
			CLASS_VECTORIZATION_STRATEGY,
			ENTITY_VECTORIZATION_STRATEGY,
			FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY,
			FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY);

	private static final Set<String> ALL_KINDS = union(
			Set.of(ACCESSES_STRATEGY, REPOSITORY_STRATEGY, STRUCTURE_STRATEGY),
			VECTORIZATION_KINDS);

	/** Every representation type the tool accepts, by type, so a test can leave one out. */
	private static final List<Supplier<Representation>> EVERY_REPRESENTATION = List.of(
			IDToEntityRepresentation::new,
			EntityToIDRepresentation::new,
			AccessesRepresentation::new,
			AuthorRepresentation::new,
			CommitRepresentation::new,
			CodeEmbeddingsRepresentation::new,
			StructureRepresentation::new);

	@Mock
	private CodebaseRepository codebaseRepository;

	@InjectMocks
	private CodebaseService codebaseService;

	@BeforeEach
	public void givenTheFixtureCodebasesExist() {
		MockitoAnnotations.initMocks(this);
		for (Codebase codebase : CodebaseFixtures.all())
			givenTheRepositoryFinds(codebase);
		givenTheRepositoryFinds(CodebaseFixtures.codebaseWith(
				UNGROUPED_STRUCTURE_NAME,
				new IDToEntityRepresentation(),
				new AccessesRepresentation(),
				new StructureRepresentation()));
		givenTheRepositoryFinds(everythingExcept(null, EVERYTHING_NAME));
		givenTheRepositoryFinds(CodebaseFixtures.codebaseWith(
				ACCESSES_ONLY_NAME,
				new AccessesRepresentation()));
	}

	// Similarity kinds offered per upload set

	@Test
	public void noUploadsOfferNoKind() {
		assertThat(offeredKinds(EMPTY_NAME)).isEmpty();
	}

	/** Every kind needs the ID-to-entity map, so accesses alone are not enough. */
	@Test
	public void accessesWithoutTheIdToEntityMapOfferNoKind() {
		assertThat(offeredKinds(ACCESSES_ONLY_NAME)).isEmpty();
	}

	@Test
	public void accessesBasedUploadsOfferOnlyTheAccessesKind() {
		assertThat(offeredKinds(ACCESSES_BASED_NAME))
				.containsExactlyInAnyOrder(ACCESSES_STRATEGY);
	}

	@Test
	public void repositoryBasedUploadsOfferTheAccessesAndRepositoryKinds() {
		assertThat(offeredKinds(REPOSITORY_BASED_NAME))
				.containsExactlyInAnyOrder(ACCESSES_STRATEGY, REPOSITORY_STRATEGY);
	}

	@Test
	public void codeEmbeddingsBasedUploadsOfferTheAccessesKindAndTheFourVectorizationKinds() {
		assertThat(offeredKinds(CODE_EMBEDDINGS_BASED_NAME))
				.containsExactlyInAnyOrderElementsOf(union(Set.of(ACCESSES_STRATEGY), VECTORIZATION_KINDS));
	}

	@Test
	public void structureBasedUploadsOfferTheAccessesAndStructureKinds() {
		assertThat(offeredKinds(STRUCTURE_BASED_NAME))
				.containsExactlyInAnyOrder(ACCESSES_STRATEGY, STRUCTURE_STRATEGY);
	}

	@Test
	public void everyRepresentationUploadedOffersAllSevenKinds() {
		assertThat(offeredKinds(EVERYTHING_NAME)).containsExactlyInAnyOrderElementsOf(ALL_KINDS);
	}

	/**
	 * Leaving out one representation type at a time withholds exactly the kinds that need it, so
	 * no kind can lose or gain a requirement without one of these cases failing.
	 */
	@ParameterizedTest(name = "without {0}")
	@MethodSource("kindsOfferedWithoutEachRepresentation")
	public void leavingOutOneRepresentationWithholdsExactlyTheKindsThatNeedIt(String missingType, Set<String> expectedKinds) {
		Codebase codebase = everythingExcept(missingType, "fixture-everything-but-" + missingType);
		givenTheRepositoryFinds(codebase);

		assertThat(offeredKinds(codebase.getName())).containsExactlyInAnyOrderElementsOf(expectedKinds);
	}

	/** Without Entity-to-ID, Structure is withheld, because the Structure kind declares it (D-019); see the Structure test below. */
	static Stream<Arguments> kindsOfferedWithoutEachRepresentation() {
		return Stream.of(
				Arguments.of(ID_TO_ENTITY, Set.of()),
				Arguments.of(ACCESSES, Set.of()),
				Arguments.of(ENTITY_TO_ID, Set.of(ACCESSES_STRATEGY, REPOSITORY_STRATEGY)),
				Arguments.of(CODE_EMBEDDINGS, Set.of(ACCESSES_STRATEGY, REPOSITORY_STRATEGY, STRUCTURE_STRATEGY)),
				Arguments.of(AUTHOR, without(ALL_KINDS, REPOSITORY_STRATEGY)),
				Arguments.of(COMMIT, without(ALL_KINDS, REPOSITORY_STRATEGY)),
				Arguments.of(STRUCTURE, without(ALL_KINDS, STRUCTURE_STRATEGY)));
	}

	/**
	 * The Structure kind's dependence on Entity-to-ID, a behavior change D-019 allows.
	 *
	 * <p>Before ticket 04, Structure was offered without Entity-to-ID, although
	 * {@code SimilarityScipyStructure.fillElements} reads it and would fail without it. D-019 has
	 * the Structure kind declare Entity-to-ID, so Structure is now withheld; the leave-one-out case
	 * without Entity-to-ID changed with it. No user sees the change: the UI could never reach this
	 * case, because the Structure upload group requires Entity-to-ID and the frontend uploads by
	 * group. The second assertion pins that.
	 */
	@Test
	public void structureIsWithheldWithoutEntityToIdAndItsUploadGroupIsNotSatisfied() {
		assertThat(offeredKinds(UNGROUPED_STRUCTURE_NAME))
				.containsExactlyInAnyOrder(ACCESSES_STRATEGY);
		assertThat(codebaseService.getCodebaseRepresentationGroups(UNGROUPED_STRUCTURE_NAME))
				.doesNotContain(STRUCTURE_TYPE);
	}

	// Similarity kinds the SciPy algorithm accepts

	@Test
	public void theSciPyAlgorithmAcceptsAllSevenKinds() {
		assertThat(sciPy().getAlgorithmSupportedStrategyTypes()).containsExactlyInAnyOrderElementsOf(ALL_KINDS);
	}

	/**
	 * {@code SciPyClustering}'s constructor fetches a bean from {@code ContextManager}, which
	 * boots a Spring context. A mock calling real methods is built without running the
	 * constructor, so the answer can be read without Spring.
	 */
	private static Clustering sciPy() {
		return mock(SciPyClustering.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
	}

	private List<String> offeredKinds(String codebaseName) {
		return codebaseService.getAllowableCodebaseStrategyTypes(codebaseName);
	}

	private void givenTheRepositoryFinds(Codebase codebase) {
		when(codebaseRepository.findByName(codebase.getName())).thenReturn(codebase);
	}

	/** A codebase with every representation type except {@code missingType}; {@code null} omits none. */
	private static Codebase everythingExcept(String missingType, String name) {
		List<Representation> representations = new ArrayList<>();
		for (Supplier<Representation> representation : EVERY_REPRESENTATION)
			representations.add(representation.get());
		representations.removeIf(representation -> representation.getType().equals(missingType));
		return CodebaseFixtures.codebaseWith(name, representations.toArray(new Representation[0]));
	}

	private static Set<String> union(Set<String> first, Set<String> second) {
		Set<String> union = new HashSet<>(first);
		union.addAll(second);
		return union;
	}

	private static Set<String> without(Set<String> kinds, String kind) {
		Set<String> without = new HashSet<>(kinds);
		without.remove(kind);
		return without;
	}
}

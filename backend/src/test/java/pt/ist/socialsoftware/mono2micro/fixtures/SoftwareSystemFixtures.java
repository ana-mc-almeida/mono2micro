package pt.ist.socialsoftware.mono2micro.fixtures;

import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Software systems with known uploaded representations, for tests that assert which features
 * a given set of uploads makes available.
 *
 * <p>{@code codebases/} is gitignored and empty in a fresh clone, so tests cannot depend on
 * whatever a developer happens to have uploaded locally. These fixtures are built in memory
 * instead: a {@link SoftwareSystem} and the {@link Representation} objects it collected. Nothing
 * is persisted and no database is contacted, so a fixture gives the same answer on every machine,
 * offline.
 *
 * <p>A software system keeps no list of its representations; each representation references its software system,
 * and they are found by repository query (D-016). So a {@link Fixture} pairs the two, and
 * {@link #stubbed(RepresentationRepository, Fixture...)} makes a mocked repository answer that
 * query for each fixture.
 *
 * <p>This works because availability is derived from representation <em>types</em> alone,
 * compared against {@link Representation#representationGroupToRepresentations}. The uploaded
 * file contents are never consulted, so the fixtures carry none.
 *
 * <p>There is one fixture per representation group the tool defines, plus {@link #empty()}.
 * Each fixture satisfies its own group and fails the others, so both the offered and the
 * withheld path are testable by picking two fixtures:
 *
 * <pre>
 * Fixture offered = SoftwareSystemFixtures.repositoryBased(); // has authorship data
 * Fixture withheld = SoftwareSystemFixtures.accessesBased(); // deliberately does not
 * </pre>
 *
 * <p>A fixture's id is its name, so a test addresses it by the name constants:
 *
 * <pre>
 * stubbed(representationRepository, SoftwareSystemFixtures.all());
 * softwareSystemService.getAllowableSoftwareSystemStrategyTypes(ACCESSES_BASED_NAME);
 * </pre>
 *
 * <p>Fixtures are built fresh on every call, so a test may mutate one without affecting another.
 */
public class SoftwareSystemFixtures {

	public static final String EMPTY_NAME = "fixture-empty";
	public static final String ACCESSES_BASED_NAME = "fixture-accesses-based";
	public static final String REPOSITORY_BASED_NAME = "fixture-repository-based";
	public static final String CODE_EMBEDDINGS_BASED_NAME = "fixture-code-embeddings-based";
	public static final String STRUCTURE_BASED_NAME = "fixture-structure-based";

	/** A software system and the representations it collected. */
	public static final class Fixture {
		private final SoftwareSystem softwareSystem;
		private final List<Representation> representations;

		private Fixture(SoftwareSystem softwareSystem, List<Representation> representations) {
			this.softwareSystem = softwareSystem;
			this.representations = representations;
		}

		public SoftwareSystem getSoftwareSystem() {
			return softwareSystem;
		}

		/** Mutable, so a test can take one away. */
		public List<Representation> getRepresentations() {
			return representations;
		}

		public String getId() {
			return softwareSystem.getId();
		}
	}

	/**
	 * A software system with nothing uploaded. Satisfies no representation group, so every feature
	 * is withheld — the baseline against which any offered feature is a difference.
	 */
	public static Fixture empty() {
		return softwareSystemWith(EMPTY_NAME);
	}

	/**
	 * Uploads satisfying {@code Accesses Based} only.
	 *
	 * <p>Deliberately carries no authorship or commit data, so a feature requiring those is
	 * withheld. This is the counterpart to {@link #repositoryBased()}.
	 */
	public static Fixture accessesBased() {
		return softwareSystemWith(
				ACCESSES_BASED_NAME,
				new IDToEntityRepresentation(),
				new AccessesRepresentation());
	}

	/**
	 * Uploads satisfying {@code Repository Based} — the accesses uploads plus authorship and
	 * commit data, so a feature requiring version history is offered.
	 */
	public static Fixture repositoryBased() {
		return softwareSystemWith(
				REPOSITORY_BASED_NAME,
				new IDToEntityRepresentation(),
				new AccessesRepresentation(),
				new AuthorRepresentation(),
				new CommitRepresentation());
	}

	/** Uploads satisfying {@code Code Embeddings Based}. */
	public static Fixture codeEmbeddingsBased() {
		return softwareSystemWith(
				CODE_EMBEDDINGS_BASED_NAME,
				new IDToEntityRepresentation(),
				new EntityToIDRepresentation(),
				new AccessesRepresentation(),
				new CodeEmbeddingsRepresentation());
	}

	/** Uploads satisfying {@code Structure Based}. */
	public static Fixture structureBased() {
		return softwareSystemWith(
				STRUCTURE_BASED_NAME,
				new IDToEntityRepresentation(),
				new EntityToIDRepresentation(),
				new AccessesRepresentation(),
				new StructureRepresentation());
	}

	/** Every fixture, for tests asserting that different uploads yield different answers. */
	public static Fixture[] all() {
		return new Fixture[] {
				empty(),
				accessesBased(),
				repositoryBased(),
				codeEmbeddingsBased(),
				structureBased() };
	}

	/**
	 * A software system with exactly the given representations, for the case no named fixture covers.
	 * Prefer a named fixture where one fits, so that tests share vocabulary.
	 */
	public static Fixture softwareSystemWith(String name, Representation... representations) {
		SoftwareSystem softwareSystem = new SoftwareSystem(name);
		softwareSystem.setId(name);
		for (Representation representation : representations) {
			representation.setName(name + " & " + representation.getType());
			representation.setSoftwareSystem(softwareSystem);
		}
		return new Fixture(softwareSystem, new ArrayList<>(Arrays.asList(representations)));
	}

	/**
	 * Makes a mocked repository answer {@code findBySoftwareSystemId} for each fixture, and
	 * {@code findBySoftwareSystemIdAndType} by the same filter the real default method applies.
	 * Mockito 2.23 cannot call a real default method of an interface mock, hence the explicit
	 * answer. Returns the repository.
	 */
	public static RepresentationRepository stubbed(RepresentationRepository representationRepository, Fixture... fixtures) {
		// doReturn/doAnswer, not when(): calling a stubbed method again would run its answer.
		for (Fixture fixture : fixtures)
			doReturn(fixture.getRepresentations()).when(representationRepository).findBySoftwareSystemId(fixture.getId());
		doAnswer(invocation -> RepresentationRepository.ofType(
				representationRepository.findBySoftwareSystemId(invocation.getArgument(0)),
				invocation.getArgument(1)))
				.when(representationRepository).findBySoftwareSystemIdAndType(anyString(), anyString());
		return representationRepository;
	}

	/** A mocked repository serving the given fixtures. */
	public static RepresentationRepository repositoryServing(Fixture... fixtures) {
		return stubbed(mock(RepresentationRepository.class), fixtures);
	}

	private SoftwareSystemFixtures() {}
}

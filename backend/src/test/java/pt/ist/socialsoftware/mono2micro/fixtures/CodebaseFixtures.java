package pt.ist.socialsoftware.mono2micro.fixtures;

import pt.ist.socialsoftware.mono2micro.codebase.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.domain.StructureRepresentation;

import java.util.Arrays;
import java.util.List;

/**
 * Codebases with known uploaded representations, for tests that assert which features
 * a given set of uploads makes available.
 *
 * <p>{@code codebases/} is gitignored and empty in a fresh clone, so tests cannot depend on
 * whatever a developer happens to have uploaded locally. These fixtures are built in memory
 * instead: a {@link Codebase} holding {@link Representation} objects. Nothing is persisted and
 * no database is contacted, so a fixture gives the same answer on every machine, offline.
 *
 * <p>This works because availability is derived from representation <em>types</em> alone —
 * {@link Codebase#getRepresentationGroups()} reads {@code getType()} and compares against
 * {@link Representation#representationGroupToRepresentations}. The uploaded file contents are
 * never consulted, so the fixtures carry none.
 *
 * <p>There is one fixture per representation group the tool defines, plus {@link #empty()}.
 * Each fixture satisfies its own group and fails the others, so both the offered and the
 * withheld path are testable by picking two fixtures:
 *
 * <pre>
 * Codebase offered  = CodebaseFixtures.repositoryBased();   // has authorship data
 * Codebase withheld = CodebaseFixtures.accessesBased();     // deliberately does not
 * </pre>
 *
 * <p>To test a service that looks a codebase up rather than receiving one, stub the repository:
 *
 * <pre>
 * when(codebaseRepository.findByName(ACCESSES_BASED_NAME))
 *         .thenReturn(CodebaseFixtures.accessesBased());
 * </pre>
 *
 * <p>Fixtures are built fresh on every call, so a test may mutate one without affecting another.
 */
public class CodebaseFixtures {

    public static final String EMPTY_NAME = "fixture-empty";
    public static final String ACCESSES_BASED_NAME = "fixture-accesses-based";
    public static final String REPOSITORY_BASED_NAME = "fixture-repository-based";
    public static final String CODE_EMBEDDINGS_BASED_NAME = "fixture-code-embeddings-based";
    public static final String STRUCTURE_BASED_NAME = "fixture-structure-based";

    /**
     * A codebase with nothing uploaded. Satisfies no representation group, so every feature
     * is withheld — the baseline against which any offered feature is a difference.
     */
    public static Codebase empty() {
        return codebaseNamed(EMPTY_NAME);
    }

    /**
     * Uploads satisfying {@code Accesses Based} only.
     *
     * <p>Deliberately carries no authorship or commit data, so a feature requiring those is
     * withheld. This is the counterpart to {@link #repositoryBased()}.
     */
    public static Codebase accessesBased() {
        return codebaseWith(
                ACCESSES_BASED_NAME,
                new IDToEntityRepresentation(),
                new AccessesRepresentation());
    }

    /**
     * Uploads satisfying {@code Repository Based} — the accesses uploads plus authorship and
     * commit data, so a feature requiring version history is offered.
     */
    public static Codebase repositoryBased() {
        return codebaseWith(
                REPOSITORY_BASED_NAME,
                new IDToEntityRepresentation(),
                new AccessesRepresentation(),
                new AuthorRepresentation(),
                new CommitRepresentation());
    }

    /** Uploads satisfying {@code Code Embeddings Based}. */
    public static Codebase codeEmbeddingsBased() {
        return codebaseWith(
                CODE_EMBEDDINGS_BASED_NAME,
                new IDToEntityRepresentation(),
                new EntityToIDRepresentation(),
                new AccessesRepresentation(),
                new CodeEmbeddingsRepresentation());
    }

    /** Uploads satisfying {@code Structure Based}. */
    public static Codebase structureBased() {
        return codebaseWith(
                STRUCTURE_BASED_NAME,
                new IDToEntityRepresentation(),
                new EntityToIDRepresentation(),
                new AccessesRepresentation(),
                new StructureRepresentation());
    }

    /** Every fixture, for tests asserting that different uploads yield different answers. */
    public static List<Codebase> all() {
        return Arrays.asList(
                empty(),
                accessesBased(),
                repositoryBased(),
                codeEmbeddingsBased(),
                structureBased());
    }

    /**
     * A codebase with exactly the given representations, for the case no named fixture covers.
     * Prefer a named fixture where one fits, so that tests share vocabulary.
     */
    public static Codebase codebaseWith(String name, Representation... representations) {
        Codebase codebase = codebaseNamed(name);
        for (Representation representation : representations) {
            representation.setName(name + " & " + representation.getType());
            representation.setCodebase(codebase);
            codebase.addRepresentation(representation);
        }
        return codebase;
    }

    private static Codebase codebaseNamed(String name) {
        return new Codebase(name);
    }

    private CodebaseFixtures() {}
}

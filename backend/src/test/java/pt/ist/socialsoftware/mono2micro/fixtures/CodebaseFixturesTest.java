package pt.ist.socialsoftware.mono2micro.fixtures;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.codebase.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.domain.AuthorRepresentation.AUTHOR;
import static pt.ist.socialsoftware.mono2micro.representation.domain.CodeEmbeddingsRepresentation.CODE_EMBEDDINGS;
import static pt.ist.socialsoftware.mono2micro.representation.domain.CommitRepresentation.COMMIT;
import static pt.ist.socialsoftware.mono2micro.representation.domain.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.ACCESSES_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.CODE_EMBEDDINGS_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.REPOSITORY_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.STRUCTURE_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.StructureRepresentation.STRUCTURE;

/**
 * Pins down what the fixtures promise, so a later change to the representation groups that
 * would silently invalidate every availability test fails here instead.
 */
public class CodebaseFixturesTest {

	@Test
	public void emptyCodebaseHasNoUploadsAndSatisfiesNoGroup() {
		Codebase codebase = CodebaseFixtures.empty();

		assertThat(codebase.getRepresentations()).isEmpty();
		assertThat(codebase.getRepresentationGroups()).isEmpty();
	}

	@Test
	public void accessesBasedSatisfiesAccessesGroupOnly() {
		Codebase codebase = CodebaseFixtures.accessesBased();

		assertThat(uploadedTypes(codebase)).containsExactlyInAnyOrder(ID_TO_ENTITY, ACCESSES);
		assertThat(codebase.getRepresentationGroups()).containsExactly(ACCESSES_TYPE);
	}

	@Test
	public void repositoryBasedSatisfiesRepositoryGroup() {
		Codebase codebase = CodebaseFixtures.repositoryBased();

		assertThat(uploadedTypes(codebase))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ACCESSES, AUTHOR, COMMIT);
		assertThat(codebase.getRepresentationGroups())
				.containsExactlyInAnyOrder(ACCESSES_TYPE, REPOSITORY_TYPE);
	}

	@Test
	public void codeEmbeddingsBasedSatisfiesCodeEmbeddingsGroup() {
		Codebase codebase = CodebaseFixtures.codeEmbeddingsBased();

		assertThat(uploadedTypes(codebase))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS);
		assertThat(codebase.getRepresentationGroups())
				.containsExactlyInAnyOrder(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE);
	}

	@Test
	public void structureBasedSatisfiesStructureGroup() {
		Codebase codebase = CodebaseFixtures.structureBased();

		assertThat(uploadedTypes(codebase))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, STRUCTURE);
		assertThat(codebase.getRepresentationGroups())
				.containsExactlyInAnyOrder(ACCESSES_TYPE, STRUCTURE_TYPE);
	}

	/**
	 * The claim every availability test rests on: the same tool offers different things for
	 * different inputs. Without this, a test asserting "feature offered" could be passing
	 * because the tool offers it unconditionally.
	 */
	@Test
	public void repositoryDataIsWhatSeparatesTheTwoContrastingFixtures() {
		Codebase offered = CodebaseFixtures.repositoryBased();
		Codebase withheld = CodebaseFixtures.accessesBased();

		assertThat(offered.getRepresentationGroups()).contains(REPOSITORY_TYPE);
		assertThat(withheld.getRepresentationGroups()).doesNotContain(REPOSITORY_TYPE);

		assertThat(uploadedTypes(offered)).contains(AUTHOR, COMMIT);
		assertThat(uploadedTypes(withheld)).doesNotContain(AUTHOR, COMMIT);
	}

	/**
	 * Guards the promise that there is a fixture per representation group. A group added to
	 * the tool with no fixture behind it fails here, rather than leaving a later availability
	 * test quietly untestable.
	 */
	@Test
	public void everyRepresentationGroupIsCoveredByAFixture() {
		for (String group : Representation.representationGroupToRepresentations.keySet()) {
			assertThat(fixturesSatisfying(group))
					.as("fixtures satisfying group <%s>", group)
					.isNotEmpty();
		}
	}

	/** Mutating one fixture must not leak into the next test that asks for the same one. */
	@Test
	public void fixturesAreBuiltFreshOnEveryCall() {
		Codebase first = CodebaseFixtures.accessesBased();
		first.removeRepresentation(first.getRepresentations().get(0).getName());
		assertThat(first.getRepresentations()).hasSize(1);

		assertThat(CodebaseFixtures.accessesBased().getRepresentations()).hasSize(2);
	}

	@Test
	public void fixtureNamesAreDistinct() {
		List<String> names = CodebaseFixtures.all().stream()
				.map(Codebase::getName)
				.collect(Collectors.toList());

		assertThat(names).doesNotHaveDuplicates();
	}

	@Test
	public void representationsKnowTheCodebaseTheyBelongTo() {
		Codebase codebase = CodebaseFixtures.repositoryBased();

		for (Representation representation : codebase.getRepresentations()) {
			assertThat(representation.getCodebase()).isSameAs(codebase);
			assertThat(representation.getName()).contains(codebase.getName());
		}
	}

	@Test
	public void lookupByFileTypeFindsAnUploadedRepresentation() {
		Codebase codebase = CodebaseFixtures.repositoryBased();

		assertThat(codebase.getRepresentationByFileType(AUTHOR)).isNotNull();
		assertThat(codebase.getRepresentationByFileType(STRUCTURE)).isNull();
	}

	private static List<String> uploadedTypes(Codebase codebase) {
		return codebase.getRepresentations().stream()
				.map(Representation::getType)
				.collect(Collectors.toList());
	}

	private static List<Codebase> fixturesSatisfying(String group) {
		return CodebaseFixtures.all().stream()
				.filter(codebase -> codebase.getRepresentationGroups().contains(group))
				.collect(Collectors.toList());
	}
}

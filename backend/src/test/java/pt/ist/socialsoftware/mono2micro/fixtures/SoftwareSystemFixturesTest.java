package pt.ist.socialsoftware.mono2micro.fixtures;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation.AUTHOR;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation.CODE_EMBEDDINGS;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation.COMMIT;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.ACCESSES_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.CODE_EMBEDDINGS_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.REPOSITORY_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.STRUCTURE_TYPE;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

/**
 * Pins down what the fixtures promise, so a later change to the representation groups that
 * would silently invalidate every availability test fails here instead.
 */
public class SoftwareSystemFixturesTest {

	@Test
	public void emptySoftwareSystemHasNoUploadsAndSatisfiesNoGroup() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.empty();

		assertThat(softwareSystem.getRepresentations()).isEmpty();
		assertThat(softwareSystem.getRepresentationGroups()).isEmpty();
	}

	@Test
	public void accessesBasedSatisfiesAccessesGroupOnly() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.accessesBased();

		assertThat(uploadedTypes(softwareSystem)).containsExactlyInAnyOrder(ID_TO_ENTITY, ACCESSES);
		assertThat(softwareSystem.getRepresentationGroups()).containsExactly(ACCESSES_TYPE);
	}

	@Test
	public void repositoryBasedSatisfiesRepositoryGroup() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.repositoryBased();

		assertThat(uploadedTypes(softwareSystem))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ACCESSES, AUTHOR, COMMIT);
		assertThat(softwareSystem.getRepresentationGroups())
				.containsExactlyInAnyOrder(ACCESSES_TYPE, REPOSITORY_TYPE);
	}

	@Test
	public void codeEmbeddingsBasedSatisfiesCodeEmbeddingsGroup() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.codeEmbeddingsBased();

		assertThat(uploadedTypes(softwareSystem))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS);
		assertThat(softwareSystem.getRepresentationGroups())
				.containsExactlyInAnyOrder(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE);
	}

	@Test
	public void structureBasedSatisfiesStructureGroup() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.structureBased();

		assertThat(uploadedTypes(softwareSystem))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, STRUCTURE);
		assertThat(softwareSystem.getRepresentationGroups())
				.containsExactlyInAnyOrder(ACCESSES_TYPE, STRUCTURE_TYPE);
	}

	/**
	 * The claim every availability test rests on: the same tool offers different things for
	 * different inputs. Without this, a test asserting "feature offered" could be passing
	 * because the tool offers it unconditionally.
	 */
	@Test
	public void repositoryDataIsWhatSeparatesTheTwoContrastingFixtures() {
		SoftwareSystem offered = SoftwareSystemFixtures.repositoryBased();
		SoftwareSystem withheld = SoftwareSystemFixtures.accessesBased();

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
		SoftwareSystem first = SoftwareSystemFixtures.accessesBased();
		first.removeRepresentation(first.getRepresentations().get(0).getName());
		assertThat(first.getRepresentations()).hasSize(1);

		assertThat(SoftwareSystemFixtures.accessesBased().getRepresentations()).hasSize(2);
	}

	@Test
	public void fixtureNamesAreDistinct() {
		List<String> names = SoftwareSystemFixtures.all().stream()
				.map(SoftwareSystem::getName)
				.collect(Collectors.toList());

		assertThat(names).doesNotHaveDuplicates();
	}

	@Test
	public void representationsKnowTheSoftwareSystemTheyBelongTo() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.repositoryBased();

		for (Representation representation : softwareSystem.getRepresentations()) {
			assertThat(representation.getSoftwareSystem()).isSameAs(softwareSystem);
			assertThat(representation.getName()).contains(softwareSystem.getName());
		}
	}

	@Test
	public void lookupByFileTypeFindsAnUploadedRepresentation() {
		SoftwareSystem softwareSystem = SoftwareSystemFixtures.repositoryBased();

		assertThat(softwareSystem.getRepresentationByFileType(AUTHOR)).isNotNull();
		assertThat(softwareSystem.getRepresentationByFileType(STRUCTURE)).isNull();
	}

	private static List<String> uploadedTypes(SoftwareSystem softwareSystem) {
		return softwareSystem.getRepresentations().stream()
				.map(Representation::getType)
				.collect(Collectors.toList());
	}

	private static List<SoftwareSystem> fixturesSatisfying(String group) {
		return SoftwareSystemFixtures.all().stream()
				.filter(softwareSystem -> softwareSystem.getRepresentationGroups().contains(group))
				.collect(Collectors.toList());
	}
}

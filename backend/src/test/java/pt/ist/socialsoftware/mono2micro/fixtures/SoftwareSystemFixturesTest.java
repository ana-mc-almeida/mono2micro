package pt.ist.socialsoftware.mono2micro.fixtures;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ist.socialsoftware.mono2micro.fixtures.SoftwareSystemFixtures.Fixture;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.softwareSystem.SoftwareSystemService;

import java.util.Arrays;
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
		Fixture fixture = SoftwareSystemFixtures.empty();

		assertThat(fixture.getRepresentations()).isEmpty();
		assertThat(groups(fixture)).isEmpty();
	}

	@Test
	public void accessesBasedSatisfiesAccessesGroupOnly() {
		Fixture fixture = SoftwareSystemFixtures.accessesBased();

		assertThat(uploadedTypes(fixture)).containsExactlyInAnyOrder(ID_TO_ENTITY, ACCESSES);
		assertThat(groups(fixture)).containsExactly(ACCESSES_TYPE);
	}

	@Test
	public void repositoryBasedSatisfiesRepositoryGroup() {
		Fixture fixture = SoftwareSystemFixtures.repositoryBased();

		assertThat(uploadedTypes(fixture))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ACCESSES, AUTHOR, COMMIT);
		assertThat(groups(fixture))
				.containsExactlyInAnyOrder(ACCESSES_TYPE, REPOSITORY_TYPE);
	}

	@Test
	public void codeEmbeddingsBasedSatisfiesCodeEmbeddingsGroup() {
		Fixture fixture = SoftwareSystemFixtures.codeEmbeddingsBased();

		assertThat(uploadedTypes(fixture))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS);
		assertThat(groups(fixture))
				.containsExactlyInAnyOrder(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE);
	}

	@Test
	public void structureBasedSatisfiesStructureGroup() {
		Fixture fixture = SoftwareSystemFixtures.structureBased();

		assertThat(uploadedTypes(fixture))
				.containsExactlyInAnyOrder(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, STRUCTURE);
		assertThat(groups(fixture))
				.containsExactlyInAnyOrder(ACCESSES_TYPE, STRUCTURE_TYPE);
	}

	/**
	 * The claim every availability test rests on: the same tool offers different things for
	 * different inputs. Without this, a test asserting "feature offered" could be passing
	 * because the tool offers it unconditionally.
	 */
	@Test
	public void repositoryDataIsWhatSeparatesTheTwoContrastingFixtures() {
		Fixture offered = SoftwareSystemFixtures.repositoryBased();
		Fixture withheld = SoftwareSystemFixtures.accessesBased();

		assertThat(groups(offered)).contains(REPOSITORY_TYPE);
		assertThat(groups(withheld)).doesNotContain(REPOSITORY_TYPE);

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
		Fixture first = SoftwareSystemFixtures.accessesBased();
		first.getRepresentations().remove(0);
		assertThat(first.getRepresentations()).hasSize(1);

		assertThat(SoftwareSystemFixtures.accessesBased().getRepresentations()).hasSize(2);
	}

	@Test
	public void fixtureNamesAndIdsAreDistinct() {
		List<String> names = Arrays.stream(SoftwareSystemFixtures.all())
				.map(fixture -> fixture.getSoftwareSystem().getName())
				.collect(Collectors.toList());
		List<String> ids = Arrays.stream(SoftwareSystemFixtures.all())
				.map(Fixture::getId)
				.collect(Collectors.toList());

		assertThat(names).doesNotHaveDuplicates();
		assertThat(ids).doesNotHaveDuplicates();
	}

	/** References go from child to parent (D-016): each representation knows its software system. */
	@Test
	public void representationsKnowTheSoftwareSystemTheyBelongTo() {
		Fixture fixture = SoftwareSystemFixtures.repositoryBased();

		for (Representation representation : fixture.getRepresentations()) {
			assertThat(representation.getSoftwareSystem()).isSameAs(fixture.getSoftwareSystem());
			assertThat(representation.getName()).contains(fixture.getSoftwareSystem().getName());
		}
	}

	@Test
	public void lookupByTypeFindsAnUploadedRepresentation() {
		Fixture fixture = SoftwareSystemFixtures.repositoryBased();
		RepresentationRepository repository = SoftwareSystemFixtures.repositoryServing(fixture);

		assertThat(repository.findBySoftwareSystemIdAndType(fixture.getId(), AUTHOR)).isPresent();
		assertThat(repository.findBySoftwareSystemIdAndType(fixture.getId(), STRUCTURE)).isEmpty();
	}

	private static List<String> uploadedTypes(Fixture fixture) {
		return fixture.getRepresentations().stream()
				.map(Representation::getType)
				.collect(Collectors.toList());
	}

	/** The representation groups the software system satisfies, as the software system service derives them. */
	private static List<String> groups(Fixture fixture) {
		SoftwareSystemService softwareSystemService = new SoftwareSystemService();
		ReflectionTestUtils.setField(softwareSystemService, "representationRepository", SoftwareSystemFixtures.repositoryServing(fixture));
		return softwareSystemService.getSoftwareSystemRepresentationGroups(fixture.getId());
	}

	private static List<Fixture> fixturesSatisfying(String group) {
		return Arrays.stream(SoftwareSystemFixtures.all())
				.filter(fixture -> groups(fixture).contains(group))
				.collect(Collectors.toList());
	}
}

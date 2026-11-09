package pt.ist.socialsoftware.mono2micro.representation;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyAccessesAndRepositoryDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.AccessesKind;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What a Similarity or a recommendation asks for its software system: that of the representations it reads (D-016). */
class SoftwareSystemOfTest {

	@Test
	void theRepresentationsGiveTheirSoftwareSystem() {
		SoftwareSystem system = new SoftwareSystem("system");
		Representation idToEntity = new IDToEntityRepresentation();
		idToEntity.setSoftwareSystem(system);
		Representation accesses = new AccessesRepresentation();
		accesses.setSoftwareSystem(system);

		assertThat(Representation.softwareSystemOf(Arrays.asList(idToEntity, accesses))).isSameAs(system);
	}

	/** A Similarity reads one software system's representations; a mix means broken data, refused naming both systems. */
	@Test
	void representationsOfTwoSoftwareSystemsAreRefused() {
		SoftwareSystem first = new SoftwareSystem("first");
		SoftwareSystem second = new SoftwareSystem("second");
		Representation idToEntity = new IDToEntityRepresentation();
		idToEntity.setSoftwareSystem(first);
		Representation accesses = new AccessesRepresentation();
		accesses.setSoftwareSystem(second);

		assertThatThrownBy(() -> Representation.softwareSystemOf(Arrays.asList(idToEntity, accesses)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(first.getId())
				.hasMessageContaining(second.getId());
	}

	/** The check runs when a Similarity is made, so a later read of its software system can take the first representation's. */
	@Test
	void aSimilarityOfTwoSoftwareSystemsIsNotMade() {
		Representation idToEntity = new IDToEntityRepresentation();
		idToEntity.setSoftwareSystem(new SoftwareSystem("first"));
		Representation accesses = new AccessesRepresentation();
		accesses.setSoftwareSystem(new SoftwareSystem("second"));

		assertThatThrownBy(() -> new SimilarityScipyAccessesAndRepository(AccessesKind.ACCESSES, Arrays.asList(idToEntity, accesses), "name",
				new SimilarityScipyAccessesAndRepositoryDto())).isInstanceOf(IllegalStateException.class);
	}

	/** A Similarity reads at least the representations its kind's family shares, so none means broken data, said as such. */
	@Test
	void noRepresentationsHaveNoSoftwareSystem() {
		assertThatThrownBy(() -> Representation.softwareSystemOf(Collections.emptyList()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("no representations");
	}
}

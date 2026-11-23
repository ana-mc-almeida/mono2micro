package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A cluster representation is a representation of its Similarity's software system, and is saved only with one (D-010, D-015). */
public class ClusterRepresentationTest {

	private final SoftwareSystem system = new SoftwareSystem("system");

	@Test
	public void itIsARepresentationOfItsSimilaritysSoftwareSystem() {
		ClusterRepresentation clusterRepresentation = new PartitionsClusterRepresentation(similarityOf(system));

		assertThat(clusterRepresentation).isInstanceOf(Representation.class);
		assertThat(clusterRepresentation.getSoftwareSystem()).isSameAs(system);
		assertThatCode(clusterRepresentation::requireSimilarityOfItsSoftwareSystem).doesNotThrowAnyException();
	}

	@Test
	public void oneWithoutASimilarityIsRefused() {
		assertThatThrownBy(new PartitionsClusterRepresentation()::requireSimilarityOfItsSoftwareSystem)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("is measured with no Similarity");
	}

	@Test
	public void oneMeasuredWithASimilarityOfAnotherSoftwareSystemIsRefused() {
		ClusterRepresentation clusterRepresentation = new PartitionsClusterRepresentation(similarityOf(system));
		clusterRepresentation.setSoftwareSystem(new SoftwareSystem("other"));

		assertThatThrownBy(clusterRepresentation::requireSimilarityOfItsSoftwareSystem)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("but its Similarity");
	}

	@Test
	public void itIsNotReadFromARepresentationFile() {
		assertThatThrownBy(() -> new PartitionsClusterRepresentation().init(system, new byte[0]))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private static Similarity similarityOf(SoftwareSystem softwareSystem) {
		Similarity similarity = mock(Similarity.class);
		when(similarity.getId()).thenReturn("similarity");
		when(similarity.getSoftwareSystem()).thenReturn(softwareSystem);
		return similarity;
	}
}

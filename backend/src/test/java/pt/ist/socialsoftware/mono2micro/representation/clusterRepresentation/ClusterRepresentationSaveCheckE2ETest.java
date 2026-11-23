package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code ClusterRepresentationSaveCheck} runs on every save through Spring Data (D-015): a cluster representation measured with a Similarity of
 * another software system is refused before anything is written. {@code ClusterRepresentationTest} covers the rule; this covers that the listener
 * applies it.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("Saving a cluster representation")
class ClusterRepresentationSaveCheckE2ETest {

	@Autowired
	private ClusterRepresentationRepository clusterRepresentationRepository;

	@Test
	@DisplayName("one measured with a Similarity of another software system is refused and not stored")
	void oneMeasuredWithASimilarityOfAnotherSoftwareSystemIsNotSaved() {
		Similarity similarity = mock(Similarity.class);
		when(similarity.getId()).thenReturn("similarity");
		when(similarity.getSoftwareSystem()).thenReturn(new SoftwareSystem("system"));
		ClusterRepresentation clusterRepresentation = new PartitionsClusterRepresentation(similarity);
		clusterRepresentation.setSoftwareSystem(new SoftwareSystem("other"));

		assertThatThrownBy(() -> clusterRepresentationRepository.save(clusterRepresentation))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("but its Similarity");
		assertThat(clusterRepresentationRepository.findById(clusterRepresentation.getId())).isEmpty();
	}
}

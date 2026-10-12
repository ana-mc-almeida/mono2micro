package pt.ist.socialsoftware.mono2micro.similarity.kind;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SimilarityKindInTest {

	@Test
	public void isMetWhenTheInputIsOneOfTheKinds() {
		SimilarityKindIn requirement = new SimilarityKindIn(Arrays.asList("Accesses", "Repository"));

		assertThat(requirement.isMetBy(AvailabilityContext.forInput(new AccessesKind()))).isTrue();
	}

	@Test
	public void isNotMetWhenTheInputIsAnotherKind() {
		SimilarityKindIn requirement = new SimilarityKindIn(Collections.singletonList("Accesses"));

		assertThat(requirement.isMetBy(AvailabilityContext.forInput(new StructureKind()))).isFalse();
	}

	@Test
	public void isNotMetWithoutAnInput() {
		SimilarityKindIn requirement = new SimilarityKindIn(Collections.singletonList("Accesses"));

		assertThat(requirement.isMetBy(AvailabilityContext.ofRepresentationKinds(Collections.emptySet()))).isFalse();
	}

	@Test
	public void isNamedAfterItsKindsInTheOrderGiven() {
		assertThat(new SimilarityKindIn(Arrays.asList("Structure", "Accesses")).name())
				.isEqualTo("Similarity kind in {Structure, Accesses}");
	}
}

package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The general requirements and {@link AnyOf}, each met and unmet, with the name the derivation
 * record reports when it fails.
 */
public class RequirementTest {

	@JsonTypeName("Accesses")
	private static class FakeAccessesKind implements VariableFeature {
	}

	@JsonTypeName("Structure")
	private static class FakeStructureKind implements VariableFeature {
	}

	private static AvailabilityContext uploads(String... kinds) {
		return AvailabilityContext.ofRepresentationKinds(new HashSet<>(Arrays.asList(kinds)));
	}

	// HasRepresentation

	@Test
	public void hasRepresentationIsMetWhenTheSystemCollectedThatKind() {
		assertThat(new HasRepresentation("Accesses").isMetBy(uploads("IDToEntity", "Accesses"))).isTrue();
	}

	@Test
	public void hasRepresentationIsNotMetWhenTheSystemDidNotCollectThatKind() {
		assertThat(new HasRepresentation("Accesses").isMetBy(uploads("IDToEntity"))).isFalse();
	}

	@Test
	public void hasRepresentationIsNamedAfterTheKindWithTheRightArticle() {
		assertThat(new HasRepresentation("Accesses").name()).isEqualTo("has an Accesses representation");
		assertThat(new HasRepresentation("Structure").name()).isEqualTo("has a Structure representation");
	}

	// SimilarityKindIn

	@Test
	public void similarityKindInIsMetWhenTheInputIsOneOfTheKinds() {
		SimilarityKindIn requirement = new SimilarityKindIn(Arrays.asList("Accesses", "Repository"));

		assertThat(requirement.isMetBy(uploads().withInput(new FakeAccessesKind()))).isTrue();
	}

	@Test
	public void similarityKindInIsNotMetWhenTheInputIsAnotherKind() {
		SimilarityKindIn requirement = new SimilarityKindIn(Collections.singletonList("Accesses"));

		assertThat(requirement.isMetBy(uploads().withInput(new FakeStructureKind()))).isFalse();
	}

	@Test
	public void similarityKindInIsNotMetWithoutAnInput() {
		SimilarityKindIn requirement = new SimilarityKindIn(Collections.singletonList("Accesses"));

		assertThat(requirement.isMetBy(uploads())).isFalse();
	}

	@Test
	public void similarityKindInIsNamedAfterItsKindsInTheOrderGiven() {
		assertThat(new SimilarityKindIn(Arrays.asList("Structure", "Accesses")).name())
				.isEqualTo("Similarity kind in {Structure, Accesses}");
	}

	// AnyOf

	@Test
	public void anyOfIsMetWhenOneAlternativeIsMet() {
		AnyOf requirement = new AnyOf(new HasRepresentation("Structure"), new HasRepresentation("Accesses"));

		assertThat(requirement.isMetBy(uploads("Accesses"))).isTrue();
	}

	@Test
	public void anyOfIsNotMetWhenNoAlternativeIsMet() {
		AnyOf requirement = new AnyOf(new HasRepresentation("Structure"), new HasRepresentation("Accesses"));

		assertThat(requirement.isMetBy(uploads("IDToEntity"))).isFalse();
	}

	@Test
	public void anyOfWithNoAlternativesIsNeverMet() {
		assertThat(new AnyOf().isMetBy(uploads("Accesses"))).isFalse();
	}

	@Test
	public void anyOfIsNamedAfterItsAlternatives() {
		AnyOf requirement = new AnyOf(new HasRepresentation("Structure"), new HasRepresentation("Accesses"));

		assertThat(requirement.name())
				.isEqualTo("any of {has a Structure representation, has an Accesses representation}");
	}
}

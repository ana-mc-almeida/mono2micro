package pt.ist.socialsoftware.mono2micro.representation;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class HasRepresentationTest {

	private static AvailabilityContext uploads(String... kinds) {
		return AvailabilityContext.ofRepresentationKinds(new HashSet<>(Arrays.asList(kinds)));
	}

	@Test
	public void isMetWhenTheSystemCollectedThatKind() {
		assertThat(new HasRepresentation("Accesses").isMetBy(uploads("IDToEntity", "Accesses"))).isTrue();
	}

	@Test
	public void isNotMetWhenTheSystemDidNotCollectThatKind() {
		assertThat(new HasRepresentation("Accesses").isMetBy(uploads("IDToEntity"))).isFalse();
	}

	@Test
	public void isNamedAfterTheKindWithTheRightArticle() {
		assertThat(new HasRepresentation("Accesses").name()).isEqualTo("has an Accesses representation");
		assertThat(new HasRepresentation("Structure").name()).isEqualTo("has a Structure representation");
	}
}

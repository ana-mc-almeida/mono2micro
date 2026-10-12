package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class AnyOfTest {

	private static AvailabilityContext uploads(String... kinds) {
		return AvailabilityContext.ofRepresentationKinds(new HashSet<>(Arrays.asList(kinds)));
	}

	@Test
	public void anyOfIsMetWhenOneAlternativeIsMet() {
		AnyOf requirement = new AnyOf(new Uploaded("Structure"), new Uploaded("Accesses"));

		assertThat(requirement.isMetBy(uploads("Accesses"))).isTrue();
	}

	@Test
	public void anyOfIsNotMetWhenNoAlternativeIsMet() {
		AnyOf requirement = new AnyOf(new Uploaded("Structure"), new Uploaded("Accesses"));

		assertThat(requirement.isMetBy(uploads("IDToEntity"))).isFalse();
	}

	@Test
	public void anyOfWithNoAlternativesIsNeverMet() {
		assertThat(new AnyOf().isMetBy(uploads("Accesses"))).isFalse();
	}

	@Test
	public void anyOfIsNamedAfterItsAlternatives() {
		AnyOf requirement = new AnyOf(new Uploaded("Structure"), new Uploaded("Accesses"));

		assertThat(requirement.name()).isEqualTo("any of {uploaded Structure, uploaded Accesses}");
	}
}

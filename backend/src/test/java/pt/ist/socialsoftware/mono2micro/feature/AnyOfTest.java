package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnyOfTest {

	private final TestFamilies test = new TestFamilies();

	private final Requirement accessesOrStructure = Requires.anyOf(
			Requires.of(TestFamilies.StructureUpload.class),
			Requires.of(TestFamilies.AccessesUpload.class));

	@Test
	public void anyOfIsMetWhenOneAlternativeIsMet() {
		assertThat(accessesOrStructure.isMetBy(test.systemHolding(test.new AccessesUpload()))).isTrue();
	}

	@Test
	public void anyOfIsNotMetWhenNoAlternativeIsMet() {
		assertThat(accessesOrStructure.isMetBy(test.systemHolding())).isFalse();
	}

	@Test
	public void anyOfWithNoAlternativesIsNeverMet() {
		assertThat(Requires.anyOf().isMetBy(test.systemHolding(test.new AccessesUpload()))).isFalse();
	}
}

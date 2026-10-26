package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The three shapes of requirement (D-025 point 4): requires a feature, requires a family (some feature of it is present), and {@link AnyOf} over
 * either. A feature names a class either way, and the context decides whether it is a feature or a family's member type.
 */
public class RequiresTest {

	private final TestFamilies test = new TestFamilies();

	private final Requirement accessesUpload = Requires.of(TestFamilies.AccessesUpload.class);
	private final Requirement anyKind = Requires.of(TestFamilies.Kind.class);

	@Test
	public void requiringAFeatureIsMetWhenItIsPresent() {
		assertThat(accessesUpload.isMetBy(test.systemHolding(test.new AccessesUpload()))).isTrue();
	}

	@Test
	public void requiringAFeatureIsNotMetWhenItIsAbsent() {
		assertThat(accessesUpload.isMetBy(test.systemHolding(test.new StructureUpload()))).isFalse();
	}

	@Test
	public void requiringAFamilyIsMetWhenAnyOfItsFeaturesIsPresent() {
		assertThat(anyKind.isMetBy(test.systemHolding().withInput(test.new StructureKind()))).isTrue();
	}

	@Test
	public void requiringAFamilyIsNotMetWhenNoneOfItsFeaturesIsPresent() {
		assertThat(anyKind.isMetBy(test.systemHolding(test.new AccessesUpload(), test.new StructureUpload()))).isFalse();
	}

	@Test
	public void anyOfAFeatureAndAFamilyIsMetByEither() {
		Requirement either = Requires.anyOf(accessesUpload, anyKind);

		assertThat(either.isMetBy(test.systemHolding(test.new AccessesUpload()))).isTrue();
		assertThat(either.isMetBy(test.systemHolding().withInput(test.new AccessesKind()))).isTrue();
		assertThat(either.isMetBy(test.systemHolding(test.new StructureUpload()))).isFalse();
	}
}

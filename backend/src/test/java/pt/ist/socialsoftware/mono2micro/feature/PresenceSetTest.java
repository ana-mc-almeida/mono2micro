package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The context is the set of present features (D-025 point 2): built once for a software system by asking each family which of its features are
 * present, and again for an input. Every question after that is a lookup.
 */
public class PresenceSetTest {

	private final TestFamilies test = new TestFamilies();

	private final FeatureId accessesUpload = FeatureId.of(TestFamilies.AccessesUpload.class);
	private final FeatureId structureUpload = FeatureId.of(TestFamilies.StructureUpload.class);
	private final FeatureId accessesKind = FeatureId.of(TestFamilies.AccessesKind.class);

	@Test
	public void aSystemsContextHoldsWhatItsFamiliesFindPresent() {
		AvailabilityContext context = test.systemHolding(test.new AccessesUpload());

		assertThat(context.isPresent(accessesUpload)).isTrue();
		assertThat(context.isPresent(structureUpload)).isFalse();
	}

	@Test
	public void aFeatureIsPresentOnlyInItsOwnFamily() {
		AvailabilityContext context = test.systemHolding(test.new AccessesUpload());

		assertThat(accessesKind.getName()).isEqualTo(accessesUpload.getName());
		assertThat(context.isPresent(accessesKind)).isFalse();
	}

	@Test
	public void addingAnInputAsksTheFamiliesAgainAndKeepsWhatWasPresent() {
		AvailabilityContext context = test.systemHolding(test.new AccessesUpload()).withInput(test.new AccessesKind());

		assertThat(context.isPresent(accessesKind)).isTrue();
		assertThat(context.isPresent(accessesUpload)).isTrue();
	}

	@Test
	public void addingAnInputLeavesTheSystemsContextUnchanged() {
		AvailabilityContext system = test.systemHolding();
		system.withInput(test.new AccessesKind());

		assertThat(system.isPresent(accessesKind)).isFalse();
	}

	@Test
	public void aFeaturesIdIsTheNearestClassDeclaringItsName() {
		Object subclass = test.new AccessesKind() {
		};

		assertThat(FeatureId.ofFeature(subclass)).isEqualTo(accessesKind);
	}
}

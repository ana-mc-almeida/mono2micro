package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The shared check: one record entry per feature asked about, offered and withheld alike, each naming the requirements it failed.
 *
 * <p>The features are inner classes, which the self-registration scan does not see, and belong to no family, so they have only their own
 * requirements.
 */
public class AvailabilityCheckTest {

	private final TestFamilies test = new TestFamilies();

	@JsonTypeName("Needs accesses")
	class NeedsAccesses implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Collections.singletonList(Requires.of(TestFamilies.AccessesUpload.class));
		}
	}

	@JsonTypeName("Needs both")
	class NeedsBoth implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Arrays.asList(Requires.of(TestFamilies.AccessesUpload.class), Requires.of(TestFamilies.StructureUpload.class));
		}
	}

	/** Only a test can declare an exclusion: every feature of the tool excludes nothing (D-025 point 9). */
	@JsonTypeName("Excludes structure")
	class ExcludesStructure implements VariableFeature {
		@Override
		public Set<FeatureId> excludes() {
			return Collections.singleton(FeatureId.of(TestFamilies.StructureUpload.class));
		}
	}

	@Test
	public void everyFeatureAppearsWithWhatItFailed() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsBoth()), test.systemHolding(test.new AccessesUpload()));

		assertThat(record.getFeatures())
				.extracting(FeatureAvailability::getName, FeatureAvailability::isAvailable, FeatureAvailability::getFailedRequirements)
				.containsExactly(
						tuple("Needs accesses", true, Collections.emptyList()),
						tuple("Needs both", false, Collections.singletonList("requires a Structure upload")));
	}

	@Test
	public void availableNamesListsOnlyTheOfferedFeatures() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsBoth()), test.systemHolding(test.new AccessesUpload()));

		assertThat(record.availableNames()).containsExactly("Needs accesses");
	}

	@Test
	public void aFeatureIsWithheldWhenAFeatureItExcludesIsPresent() {
		DerivationRecord record = AvailabilityCheck.derive(
				Collections.singletonList(new ExcludesStructure()), test.systemHolding(test.new StructureUpload()));

		assertThat(record.getFeatures())
				.extracting(FeatureAvailability::isAvailable, FeatureAvailability::getFailedRequirements)
				.containsExactly(tuple(false, Collections.singletonList("excludes a Structure upload")));
	}

	@Test
	public void aFeatureIsOfferedWhenNoFeatureItExcludesIsPresent() {
		DerivationRecord record = AvailabilityCheck.derive(
				Collections.singletonList(new ExcludesStructure()), test.systemHolding(test.new AccessesUpload()));

		assertThat(record.availableNames()).containsExactly("Excludes structure");
	}
}

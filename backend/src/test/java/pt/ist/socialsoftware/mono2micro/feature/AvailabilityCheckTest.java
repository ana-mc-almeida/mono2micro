package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	@Test
	public void everyFeatureAppearsWithWhatItFailed() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsBoth()), test.systemHolding(test.new AccessesUpload()));

		assertThat(record.getFeatures())
				.extracting(FeatureAvailability::getName, FeatureAvailability::isAvailable, FeatureAvailability::getFailedRequirements)
				.containsExactly(
						tuple("Needs accesses", true, Collections.emptyList()),
						tuple("Needs both", false, Collections.singletonList(Requires.of(TestFamilies.StructureUpload.class).name())));
	}

	@Test
	public void availableNamesListsOnlyTheOfferedFeatures() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsBoth()), test.systemHolding(test.new AccessesUpload()));

		assertThat(record.availableNames()).containsExactly("Needs accesses");
	}
}

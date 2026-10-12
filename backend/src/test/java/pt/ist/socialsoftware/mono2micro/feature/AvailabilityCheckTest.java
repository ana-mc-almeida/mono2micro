package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The shared check: one record entry per feature of the family, offered and withheld alike, each
 * naming the requirements it failed.
 */
public class AvailabilityCheckTest {

	@JsonTypeName("Needs accesses")
	private static class NeedsAccesses implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Collections.singletonList(new Uploaded("Accesses"));
		}
	}

	@JsonTypeName("Needs history")
	private static class NeedsHistory implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Arrays.asList(
					new Uploaded("Accesses"),
					new Uploaded("Changes Authorship"),
					new Uploaded("File Changes"));
		}
	}

	@Test
	public void everyFeatureOfTheFamilyAppearsWithWhatItFailed() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsHistory()),
				AvailabilityContext.ofRepresentationKinds(Collections.singleton("Accesses")));

		assertThat(record.getFeatures())
				.extracting(FeatureAvailability::getName, FeatureAvailability::isAvailable, FeatureAvailability::getFailedRequirements)
				.containsExactly(
						tuple("Needs accesses", true, Collections.emptyList()),
						tuple("Needs history", false, Arrays.asList(
								"uploaded Changes Authorship",
								"uploaded File Changes")));
	}

	@Test
	public void availableNamesListsOnlyTheOfferedFeatures() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsHistory()),
				AvailabilityContext.ofRepresentationKinds(Collections.singleton("Accesses")));

		assertThat(record.availableNames()).containsExactly("Needs accesses");
	}
}

package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.codebase.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.fixtures.CodebaseFixtures;

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
			return Collections.singletonList(new HasRepresentation("Accesses"));
		}
	}

	@JsonTypeName("Needs history")
	private static class NeedsHistory implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Arrays.asList(
					new HasRepresentation("Accesses"),
					new HasRepresentation("Changes Authorship"),
					new HasRepresentation("File Changes"));
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
								"has a Changes Authorship representation",
								"has a File Changes representation")));
	}

	@Test
	public void availableNamesListsOnlyTheOfferedFeatures() {
		DerivationRecord record = AvailabilityCheck.derive(
				Arrays.asList(new NeedsAccesses(), new NeedsHistory()),
				AvailabilityContext.ofRepresentationKinds(Collections.singleton("Accesses")));

		assertThat(record.availableNames()).containsExactly("Needs accesses");
	}

	@Test
	public void aSystemsContextHoldsTheSystemAndTheKindsOfItsCollectedRepresentations() {
		Codebase system = CodebaseFixtures.accessesBased();

		AvailabilityContext context = AvailabilityContext.of(system);

		assertThat(context.getSystem()).isSameAs(system);
		assertThat(context.getRepresentationKinds()).containsExactlyInAnyOrder("IDToEntity", "Accesses");
		assertThat(context.getInput()).isEmpty();
	}
}

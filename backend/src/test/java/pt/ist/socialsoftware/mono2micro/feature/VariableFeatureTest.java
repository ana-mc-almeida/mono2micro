package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link VariableFeature} contract itself, independently of any family.
 *
 * <p>{@link SilentFeature} below is deliberately empty: it is the shape of a feature on the day it
 * adopts the interface, before it has anything to declare. That it compiles at all is the point:
 * every method has a default, so adoption costs nothing. If a default is ever removed, or an
 * abstract method added, this class stops compiling, which is the intended alarm.
 */
public class VariableFeatureTest {

	@JsonTypeName("Silent")
	private static class SilentFeature implements VariableFeature {
	}

	@JsonTypeName("Reads accesses")
	private static class AccessesReadingFeature implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Arrays.asList(new Uploaded("IDToEntity"), new Uploaded("Accesses"));
		}
	}

	@Test
	public void aFeatureDeclaringNothingHasNoRequirements() {
		assertThat(new SilentFeature().requirements()).isEmpty();
	}

	@Test
	public void aFeatureDeclaringNothingExcludesNothing() {
		assertThat(new SilentFeature().excludes()).isEmpty();
	}

	/**
	 * Every one of the model's 21 cross-tree constraints is an implication, so {@code excludes()}
	 * is expected empty everywhere (D-019). A non-empty return is a finding about the model, not a
	 * bug; see {@link VariableFeature#excludes()}.
	 */
	@Test
	public void excludesStaysEmptyWhenAFeatureDeclaresRequirements() {
		assertThat(new AccessesReadingFeature().excludes()).isEmpty();
	}

	@Test
	public void aFeatureWithNoRequirementsIsAvailableEvenToASystemWithNoUploads() {
		DerivationRecord record = AvailabilityCheck.derive(
				Collections.singletonList(new SilentFeature()),
				AvailabilityContext.ofRepresentationKinds(Collections.emptySet()));

		FeatureAvailability feature = record.getFeatures().get(0);
		assertThat(feature.isAvailable()).isTrue();
		assertThat(feature.getFailedRequirements()).isEmpty();
	}

	@Test
	public void aFeatureIsWithheldWhenARequirementFailsAndTheFailureIsNamed() {
		DerivationRecord record = AvailabilityCheck.derive(
				Collections.singletonList(new AccessesReadingFeature()),
				AvailabilityContext.ofRepresentationKinds(Collections.singleton("IDToEntity")));

		FeatureAvailability feature = record.getFeatures().get(0);
		assertThat(feature.isAvailable()).isFalse();
		assertThat(feature.getFailedRequirements()).containsExactly("uploaded Accesses");
	}
}

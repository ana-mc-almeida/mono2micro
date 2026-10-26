package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The check adds a family's shared requirements to each feature's own list (D-025 point 5). In {@link TestFamilies}, every kind shares the
 * Accesses upload, and the Structure kind adds the Structure upload of its own.
 */
public class SharedRequirementsTest {

	private final TestFamilies test = new TestFamilies();

	private DerivationRecord kindsFor(AvailabilityContext context) {
		return AvailabilityCheck.derive(Arrays.asList(test.new AccessesKind(), test.new StructureKind()), context);
	}

	@Test
	public void aFeatureWithoutRequirementsOfItsOwnStillNeedsWhatItsFamilyShares() {
		assertThat(kindsFor(test.systemHolding()).availableNames()).isEmpty();
		assertThat(kindsFor(test.systemHolding(test.new AccessesUpload())).availableNames()).containsExactly("Accesses");
	}

	@Test
	public void aFeatureNeedsBothWhatItsFamilySharesAndItsOwn() {
		assertThat(kindsFor(test.systemHolding(test.new StructureUpload())).availableNames()).isEmpty();
		assertThat(kindsFor(test.systemHolding(test.new AccessesUpload(), test.new StructureUpload())).availableNames())
				.containsExactly("Accesses", "Structure");
	}

	@Test
	public void theSharedRequirementsComeFirstInTheRecord() {
		DerivationRecord record = kindsFor(test.systemHolding());

		assertThat(record.getFeatures().get(1).getFailedRequirements())
				.containsExactly("requires an Accesses upload", "requires a Structure upload");
	}
}

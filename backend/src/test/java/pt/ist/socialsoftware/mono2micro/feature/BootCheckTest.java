package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The boot fails when the families do not add up (D-025 point 7): two families claim one feature, a variable feature belongs to no family, or a
 * requirement or exclusion names something that is neither a feature nor a family. Each case is built from {@link TestFamilies} plus one
 * family that breaks it.
 */
public class BootCheckTest {

	private final TestFamilies test = new TestFamilies();

	private final List<Class<? extends VariableFeature>> testKinds = Arrays.asList(TestFamilies.AccessesKind.class, TestFamilies.StructureKind.class);

	/** Claimed by no family. */
	@JsonTypeName("Orphan")
	class Orphan {
	}

	@JsonTypeName("Unclaimed")
	class Unclaimed implements VariableFeature {
	}

	@JsonTypeName("Needs orphan")
	class NeedsOrphan implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Collections.singletonList(Requires.anyOf(Requires.of(TestFamilies.AccessesUpload.class), Requires.of(Orphan.class)));
		}
	}

	@JsonTypeName("Needs a string")
	class NeedsString implements VariableFeature {
		@Override
		public List<Requirement> requirements() {
			return Collections.singletonList(Requires.of(String.class));
		}
	}

	@JsonTypeName("Excludes orphan")
	class ExcludesOrphan implements VariableFeature {
		@Override
		public Set<FeatureId> excludes() {
			return Collections.singleton(FeatureId.of(Orphan.class));
		}
	}

	private FeatureFamily<VariableFeature> familyOf(VariableFeature member) {
		return new FeatureFamily<VariableFeature>("extra", VariableFeature.class, Collections.singletonList(member)) {
		};
	}

	private FeatureFamilies withTestFamilies(FeatureFamily<?> extra) {
		return new FeatureFamilies(Arrays.asList(test.uploads, test.kinds, extra));
	}

	private List<Class<? extends VariableFeature>> testKindsAnd(Class<? extends VariableFeature> extra) {
		List<Class<? extends VariableFeature>> types = new ArrayList<>(testKinds);
		types.add(extra);
		return types;
	}

	@Test
	public void familiesThatAddUpPass() {
		assertThatCode(() -> test.families.verify(testKinds)).doesNotThrowAnyException();
	}

	@Test
	public void twoFamiliesClaimingOneFeatureFail() {
		FeatureFamily<TestFamilies.Upload> second = new FeatureFamily<TestFamilies.Upload>(
				"second upload", TestFamilies.Upload.class, Collections.singletonList(test.new AccessesUpload())) {
		};

		assertThatThrownBy(() -> withTestFamilies(second).verify(testKinds))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Accesses")
				.hasMessageContaining("upload")
				.hasMessageContaining("second upload");
	}

	@Test
	public void aVariableFeatureNoFamilyClaimsFails() {
		assertThatThrownBy(() -> test.families.verify(testKindsAnd(Unclaimed.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(Unclaimed.class.getName());
	}

	@Test
	public void aRequirementNamingAnUnclaimedFeatureFailsEvenInsideAnyOf() {
		assertThatThrownBy(() -> withTestFamilies(familyOf(new NeedsOrphan())).verify(testKindsAnd(NeedsOrphan.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Needs orphan")
				.hasMessageContaining(Orphan.class.getName());
	}

	@Test
	public void aRequirementNamingAClassThatIsNoFeatureFails() {
		assertThatThrownBy(() -> withTestFamilies(familyOf(new NeedsString())).verify(testKindsAnd(NeedsString.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Needs a string")
				.hasMessageContaining(String.class.getName());
	}

	@Test
	public void anExclusionNamingAnUnclaimedFeatureFails() {
		assertThatThrownBy(() -> withTestFamilies(familyOf(new ExcludesOrphan())).verify(testKindsAnd(ExcludesOrphan.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Excludes orphan")
				.hasMessageContaining(Orphan.class.getName());
	}

	@Test
	public void aSharedRequirementNamingAnUnclaimedFeatureFails() {
		FeatureFamily<VariableFeature> sharesOrphan = new FeatureFamily<VariableFeature>("extra", VariableFeature.class, Collections.emptyList()) {
			@Override
			public List<Requirement> sharedRequirements() {
				return Collections.singletonList(Requires.of(Orphan.class));
			}
		};

		assertThatThrownBy(() -> withTestFamilies(sharesOrphan).verify(testKinds))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("extra")
				.hasMessageContaining(Orphan.class.getName());
	}
}

package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A requirement's name is generated from its target and the label of the target's family (D-025 point 6), so two features with one name, such
 * as the Accesses upload and the Accesses kind, fail with two different names. There are no custom messages.
 */
public class RequirementNamesTest {

	private final TestFamilies test = new TestFamilies();

	/** An inner class that no family claims. */
	@JsonTypeName("Orphan")
	class Orphan {
	}

	@Test
	public void requiringAFeatureNamesItWithItsFamilyLabel() {
		assertThat(Requires.of(TestFamilies.AccessesUpload.class).name(test.families)).isEqualTo("requires an Accesses upload");
		assertThat(Requires.of(TestFamilies.AccessesKind.class).name(test.families)).isEqualTo("requires an Accesses kind as input");
	}

	@Test
	public void requiringAFamilyNamesTheFamilyLabel() {
		assertThat(Requires.of(TestFamilies.Kind.class).name(test.families)).isEqualTo("requires a kind as input");
	}

	@Test
	public void anyOfNamesEachAlternativeUnderOneRequires() {
		Requirement either = Requires.anyOf(Requires.of(TestFamilies.StructureUpload.class), Requires.of(TestFamilies.Kind.class));

		assertThat(either.name(test.families)).isEqualTo("requires any of {a Structure upload, a kind as input}");
	}

	/** In the tool this cannot happen, since the boot fails on a target no family claims; the name falls back to the feature's own. */
	@Test
	public void aTargetNoFamilyClaimsIsNamedByItself() {
		assertThat(Requires.of(Orphan.class).name(test.families)).isEqualTo("requires Orphan");
	}
}

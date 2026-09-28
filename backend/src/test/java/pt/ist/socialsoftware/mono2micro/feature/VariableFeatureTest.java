package pt.ist.socialsoftware.mono2micro.feature;

import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link VariableFeature} contract itself, independently of any hierarchy.
 *
 * <p>Nothing implements the interface yet — this ticket is the expand half of expand–contract,
 * so the interface exists and the tool behaves exactly as before. What is testable at this
 * point is the contract a future implementor inherits: that the three declarations exist, that
 * all three default to empty, and that a feature declaring nothing costs nothing to write.
 *
 * <p>{@link SilentFeature} below is deliberately empty: it declares an implementing class and
 * nothing else, which is exactly the shape a hierarchy member will have on the day it starts
 * implementing the interface and before it has anything to declare. That it compiles at all is
 * the point — every method has a default, so adoption costs nothing. If a default is ever
 * removed, or an abstract method added, this class stops compiling, which is the intended alarm.
 */
public class VariableFeatureTest {

	/**
	 * A feature that declares nothing — the cheapest possible implementation.
	 */
	private static class SilentFeature implements VariableFeature {
	}

	/**
	 * A feature that declares in both vocabularies, showing the pairing the interface exists for.
	 * {@code AUTHOR} is a representation type the tool already speaks; {@code Version analysis}
	 * is the model's own name for what that upload provides.
	 */
	private static class VersionAnalysingFeature implements VariableFeature {
		@Override
		public Set<String> requiresRepresentations() {
			return Set.of("author", "commit");
		}

		@Override
		public Set<String> requiresFeatures() {
			return Set.of("Version analysis");
		}
	}

	@Test
	public void aFeatureDeclaringNothingRequiresNoRepresentations() {
		assertThat(new SilentFeature().requiresRepresentations()).isEmpty();
	}

	@Test
	public void aFeatureDeclaringNothingRequiresNoModelFeatures() {
		assertThat(new SilentFeature().requiresFeatures()).isEmpty();
	}

	@Test
	public void aFeatureDeclaringNothingExcludesNothing() {
		assertThat(new SilentFeature().excludes()).isEmpty();
	}

	/**
	 * The expectation the interface documents: every one of the model's 21 cross-tree constraints
	 * is an implication, so {@code excludes()} is expected empty everywhere. A non-empty return
	 * is a finding about the model, not a bug — see {@link VariableFeature#excludes()}.
	 */
	@Test
	public void excludesIsEmptyByDefaultSoAnIncompatibilityMustBeDeclaredDeliberately() {
		assertThat(new VersionAnalysingFeature().excludes()).isEmpty();
	}

	@Test
	public void theTwoVocabulariesAreDeclaredSeparately() {
		VersionAnalysingFeature feature = new VersionAnalysingFeature();

		assertThat(feature.requiresRepresentations())
				.containsExactlyInAnyOrder("author", "commit");
		assertThat(feature.requiresFeatures())
				.containsExactly("Version analysis");
	}

	/**
	 * The declarations are read by generic availability code that must not have to defend against
	 * a null from an implementor that forgot one. Defaults returning empty collections rather than
	 * null is what makes {@code containsAll} safe at the availability seam.
	 */
	@Test
	public void everyDeclarationReturnsACollectionRatherThanNull() {
		VariableFeature feature = new SilentFeature();

		for (Collection<String> declaration : List.of(
				feature.requiresRepresentations(),
				feature.requiresFeatures(),
				feature.excludes())) {
			assertThat(declaration).isNotNull();
		}
	}
}

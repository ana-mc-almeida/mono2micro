package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Two test-only families for the core's tests, so they depend on no module of the tool (D-022).
 *
 * <p>Every class here is an inner class or an anonymous one. The self-registration scan finds only top-level and static nested classes, so none of
 * these joins the tool's own families, and none of them is a variable feature the boot check would find unclaimed.
 *
 * <ul>
 * <li>{@link #uploads}: features a software system holds, like the representation kinds. One is present when the system holds an instance of it.
 * <li>{@link #kinds}: variable features an input is, like the Similarity kinds. One is present when it is the input. Every kind shares the
 * requirement of an Accesses upload.
 * </ul>
 *
 * <p>The upload and the kind named "Accesses" are two features, because they belong to two families.
 */
class TestFamilies {

	abstract class Upload {
	}

	@JsonTypeName("Accesses")
	class AccessesUpload extends Upload {
	}

	@JsonTypeName("Structure")
	class StructureUpload extends Upload {
	}

	abstract class Kind implements VariableFeature {
	}

	@JsonTypeName("Accesses")
	class AccessesKind extends Kind {
	}

	@JsonTypeName("Structure")
	class StructureKind extends Kind {
		@Override
		public List<Requirement> requirements() {
			return Collections.singletonList(Requires.of(StructureUpload.class));
		}
	}

	final FeatureFamily<Upload> uploads = new FeatureFamily<Upload>("upload", Upload.class, Arrays.asList(new AccessesUpload(), new StructureUpload())) {
		@Override
		public Set<FeatureId> presentInSystem(Collection<?> holdings) {
			return featuresAmong(holdings);
		}
	};

	final FeatureFamily<Kind> kinds = new FeatureFamily<Kind>("kind as input", Kind.class, Arrays.asList(new AccessesKind(), new StructureKind())) {
		@Override
		public Set<FeatureId> presentInInput(Object input) {
			return featuresAmong(Collections.singleton(input));
		}

		@Override
		public List<Requirement> sharedRequirements() {
			return Collections.singletonList(Requires.of(AccessesUpload.class));
		}
	};

	final FeatureFamilies families = new FeatureFamilies(Arrays.asList(uploads, kinds));

	/** The context of a software system that holds {@code holdings}. */
	AvailabilityContext systemHolding(Object... holdings) {
		return families.contextFor(Arrays.asList(holdings));
	}
}

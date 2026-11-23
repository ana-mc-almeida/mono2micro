package pt.ist.socialsoftware.mono2micro.representation;

import pt.ist.socialsoftware.mono2micro.feature.FeatureFamily;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.Collection;
import java.util.Set;

/**
 * The representation kinds, as features other features require (D-025). A kind has an id, its class, but is not a variable feature, because
 * nothing offers it: the architect collects it. A kind is present when the software system collected a representation of it.
 */
public class RepresentationKindFamily extends FeatureFamily<Representation> {

	public RepresentationKindFamily() {
		super("representation", Representation.class);
	}

	@Override
	public Set<FeatureId> presentInSystem(Collection<?> holdings) {
		return featuresAmong(holdings);
	}
}

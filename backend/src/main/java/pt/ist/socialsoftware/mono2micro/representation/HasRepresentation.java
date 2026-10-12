package pt.ist.socialsoftware.mono2micro.representation;

import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;

/**
 * Met when the System has collected a representation of the given kind, named as the tool's
 * representation types name themselves ({@code Accesses}, {@code Changes Authorship}).
 */
public class HasRepresentation implements Requirement {

	private final String representationKind;

	public HasRepresentation(String representationKind) {
		this.representationKind = representationKind;
	}

	@Override
	public String name() {
		String article = "AEIOU".indexOf(Character.toUpperCase(representationKind.charAt(0))) >= 0 ? "an" : "a";
		return "has " + article + " " + representationKind + " representation";
	}

	@Override
	public boolean isMetBy(AvailabilityContext context) {
		return context.getRepresentationKinds().contains(representationKind);
	}
}

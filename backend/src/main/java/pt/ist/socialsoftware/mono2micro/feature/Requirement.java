package pt.ist.socialsoftware.mono2micro.feature;

/**
 * One thing a {@link VariableFeature} needs before the tool can offer it, named in the tool's own
 * terms ("has an Accesses representation", "Similarity kind in {…}").
 *
 * <p>A requirement is an object rather than a string so that the shared check never needs a branch
 * per kind of requirement: a new kind is one new class (D-019). Requirements used by several
 * families live in this package; one tied to a single family lives with that family.
 */
public interface Requirement {

	/**
	 * What the derivation record reports when this requirement fails. It is in tool terms; the model
	 * feature each requirement corresponds to is recorded outside the code (D-019 point 2).
	 */
	String name();

	boolean isMetBy(AvailabilityContext context);
}

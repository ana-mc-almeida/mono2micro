package pt.ist.socialsoftware.mono2micro.feature;

/**
 * A test-only requirement, so the core's tests depend on no module of the tool (D-022). Met when
 * the context's representation kinds include its kind.
 */
class Uploaded implements Requirement {

	private final String kind;

	Uploaded(String kind) {
		this.kind = kind;
	}

	@Override
	public String name() {
		return "uploaded " + kind;
	}

	@Override
	public boolean isMetBy(AvailabilityContext context) {
		return context.getRepresentationKinds().contains(kind);
	}
}

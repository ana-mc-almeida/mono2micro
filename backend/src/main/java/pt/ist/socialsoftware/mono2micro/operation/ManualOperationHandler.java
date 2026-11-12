package pt.ist.socialsoftware.mono2micro.operation;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;

/**
 * The code that runs one manual operation on one cluster kind (D-014). A handler registers itself by being a concrete class with a no-arg
 * constructor, found by the self-registration scan, and {@link ManualOperationHandlers} picks it by its {@link #operation()} and
 * {@link #clusterKind()}.
 *
 * <p>A new cluster kind needs no handler where its superclass's does the right thing, and a new manual operation needs one handler per kind
 * that no superclass covers, so neither edits the other. A handler that runs another operation, as an undo does, runs it through
 * {@link ManualOperationHandlers} too, so the other operation's handler for the decomposition's kind is the one that runs.
 */
public interface ManualOperationHandler<O extends Operation, D extends Decomposition> {

	/** The operation this handler runs. */
	Class<O> operation();

	/** The cluster kind it runs on, and every subclass of it that has no handler of its own. */
	Class<D> clusterKind();

	/** Applies {@code operation} to {@code decomposition}, without touching its history. */
	void apply(O operation, D decomposition);

	/** Reverts what {@link #apply} did, without touching the history. */
	void undo(O operation, D decomposition);
}

package pt.ist.socialsoftware.mono2micro.operation;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The registry keyed by (operation, cluster kind) that a manual operation finds its code in (D-014). A kind with no handler of its own falls
 * back to its superclass's, up to {@link Decomposition}.
 */
public final class ManualOperationHandlers {

	private static volatile ManualOperationHandlers scanned;

	private final Map<Key, ManualOperationHandler<?, ?>> handlers = new HashMap<>();

	/**
	 * @throws IllegalStateException
	 * if two handlers claim the same (operation, cluster kind)
	 */
	public ManualOperationHandlers(Collection<? extends ManualOperationHandler<?, ?>> handlers) {
		for (ManualOperationHandler<?, ?> handler : handlers) {
			Key key = new Key(handler.operation(), handler.clusterKind());
			ManualOperationHandler<?, ?> previous = this.handlers.putIfAbsent(key, handler);
			if (previous != null)
				throw new IllegalStateException(previous.getClass().getName() + " and " + handler.getClass().getName() + " both handle " + key);
		}
	}

	/** The tool's handlers, found by the self-registration scan, so adding a handler is adding its class. */
	public static ManualOperationHandlers scanned() {
		ManualOperationHandlers registry = scanned;
		if (registry == null) {
			List<ManualOperationHandler<?, ?>> handlers = new ArrayList<>();
			for (ManualOperationHandler<?, ?> handler : SelfRegistration.instances(ManualOperationHandler.class))
				handlers.add(handler);
			registry = new ManualOperationHandlers(handlers);
			scanned = registry;
		}
		return registry;
	}

	/** Runs {@code operation}'s handler for {@code decomposition}'s cluster kind. */
	public void apply(Operation operation, Decomposition decomposition) {
		apply(handlerFor(operation.getClass(), decomposition.getClass()), operation, decomposition);
	}

	/** Runs the undo of {@code operation}'s handler for {@code decomposition}'s cluster kind. */
	public void undo(Operation operation, Decomposition decomposition) {
		undo(handlerFor(operation.getClass(), decomposition.getClass()), operation, decomposition);
	}

	/**
	 * Fails when some operation has no handler for some cluster kind, directly or by fallback, so a gap stops the boot instead of a user's edit.
	 *
	 * @throws IllegalStateException
	 * naming the first uncovered pair
	 */
	public void verify(Collection<Class<? extends Operation>> operations, Collection<Class<? extends Decomposition>> kinds) {
		for (Class<? extends Operation> operation : operations)
			for (Class<? extends Decomposition> kind : kinds)
				handlerFor(operation, kind);
	}

	private ManualOperationHandler<?, ?> handlerFor(Class<?> operation, Class<?> kind) {
		return find(operation, kind).orElseThrow(() -> new IllegalStateException("No handler runs " + operation.getSimpleName() + " on "
				+ kind.getSimpleName() + " or any cluster kind it extends"));
	}

	private Optional<ManualOperationHandler<?, ?>> find(Class<?> operation, Class<?> kind) {
		for (Class<?> type = kind; type != null && Decomposition.class.isAssignableFrom(type); type = type.getSuperclass()) {
			ManualOperationHandler<?, ?> handler = handlers.get(new Key(operation, type));
			if (handler != null)
				return Optional.of(handler);
		}
		return Optional.empty();
	}

	private static <O extends Operation, D extends Decomposition> void apply(ManualOperationHandler<O, D> handler, Operation operation,
			Decomposition decomposition) {
		handler.apply(handler.operation().cast(operation), handler.clusterKind().cast(decomposition));
	}

	private static <O extends Operation, D extends Decomposition> void undo(ManualOperationHandler<O, D> handler, Operation operation,
			Decomposition decomposition) {
		handler.undo(handler.operation().cast(operation), handler.clusterKind().cast(decomposition));
	}

	private static final class Key {
		private final Class<?> operation;
		private final Class<?> kind;

		Key(Class<?> operation, Class<?> kind) {
			this.operation = operation;
			this.kind = kind;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Key))
				return false;
			Key key = (Key) other;
			return operation.equals(key.operation) && kind.equals(key.kind);
		}

		@Override
		public int hashCode() {
			return Objects.hash(operation, kind);
		}

		@Override
		public String toString() {
			return "(" + operation.getSimpleName() + ", " + kind.getSimpleName() + ")";
		}
	}
}

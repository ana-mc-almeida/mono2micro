package pt.ist.socialsoftware.mono2micro.utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one self-registration scan: a family's members are found on the classpath, not listed in a
 * factory, so adding a member is adding its class.
 *
 * <p>A member's name is its {@code @JsonTypeName}, which is also its JSON type, registry key and
 * frontend lookup key (D-019 point 7). The scan keeps one prototype of each concrete member, built
 * with its no-arg constructor, so a family can ask its members what they declare.
 */
public final class SelfRegistration {

	private static final String BASE_PACKAGE = "pt.ist.socialsoftware.mono2micro";

	private static final Map<Class<?>, SortedMap<String, ?>> PROTOTYPES = new ConcurrentHashMap<>();

	private SelfRegistration() {
	}

	/**
	 * One prototype of every concrete class assignable to {@code family}, keyed and ordered by name.
	 *
	 * @throws IllegalStateException
	 * if a member has no {@code @JsonTypeName} of its own, or two
	 * members share a name
	 */
	@SuppressWarnings("unchecked")
	public static <T> SortedMap<String, T> prototypes(Class<T> family) {
		return (SortedMap<String, T>) PROTOTYPES.computeIfAbsent(family, SelfRegistration::scan);
	}

	/**
	 * The name of a member, read from the nearest {@code @JsonTypeName} up its class hierarchy.
	 *
	 * <p>Looking up the hierarchy lets a runtime subclass, such as a proxy or a mock, answer with
	 * the name of the feature it stands for. The scan is stricter on purpose: a registered class
	 * must declare its own name, so a real subclass cannot inherit its parent's and collide.
	 */
	public static String nameOf(Object member) {
		return findName(member).orElseThrow(() -> new IllegalStateException(member.getClass().getName() + " has no @JsonTypeName"));
	}

	public static boolean hasName(Object member) {
		return findName(member).isPresent();
	}

	private static Optional<String> findName(Object member) {
		for (Class<?> type = member.getClass(); type != null; type = type.getSuperclass()) {
			JsonTypeName name = type.getDeclaredAnnotation(JsonTypeName.class);
			if (name != null)
				return Optional.of(name.value());
		}
		return Optional.empty();
	}

	private static <T> SortedMap<String, T> scan(Class<T> family) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(family));

		SortedMap<String, T> members = new TreeMap<>();
		for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
			Class<? extends T> type = load(candidate.getBeanClassName(), family);
			JsonTypeName name = type.getDeclaredAnnotation(JsonTypeName.class);
			if (name == null)
				throw new IllegalStateException(type.getName() + " is a " + family.getSimpleName() + " without a @JsonTypeName");
			if (members.containsKey(name.value()))
				throw new IllegalStateException("Two " + family.getSimpleName() + " members are named \"" + name.value() + "\": "
						+ members.get(name.value()).getClass().getName() + " and " + type.getName());
			members.put(name.value(), instantiate(type));
		}
		return Collections.unmodifiableSortedMap(members);
	}

	private static <T> Class<? extends T> load(String className, Class<T> family) {
		try {
			return Class.forName(className).asSubclass(family);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private static <T> T instantiate(Class<T> type) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(type.getName() + " needs a no-arg constructor to be registered", e);
		}
	}
}

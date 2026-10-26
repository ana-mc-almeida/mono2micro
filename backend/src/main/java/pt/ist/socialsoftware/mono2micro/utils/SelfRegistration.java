package pt.ist.socialsoftware.mono2micro.utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
	private static final Map<Class<?>, List<?>> INSTANCES = new ConcurrentHashMap<>();

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
	 * One instance of every concrete class assignable to {@code type}, in class-name order, for types whose members need no name, such as the
	 * feature families.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> instances(Class<T> type) {
		return (List<T>) INSTANCES.computeIfAbsent(type, SelfRegistration::scanInstances);
	}

	private static <T> List<T> scanInstances(Class<T> type) {
		List<T> instances = new ArrayList<>();
		for (Class<? extends T> member : candidates(type))
			instances.add(instantiate(member));
		return Collections.unmodifiableList(instances);
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

	/**
	 * The nearest class up {@code type}'s hierarchy that declares a {@code @JsonTypeName}, so a runtime subclass resolves to the member it
	 * stands for, as in {@link #nameOf(Object)}.
	 *
	 * @throws IllegalStateException
	 * if no class up the hierarchy declares one
	 */
	public static Class<?> namedType(Class<?> type) {
		return findNamedType(type).orElseThrow(() -> new IllegalStateException(type.getName() + " has no @JsonTypeName"));
	}

	private static Optional<String> findName(Object member) {
		return findNamedType(member.getClass()).map(type -> type.getDeclaredAnnotation(JsonTypeName.class).value());
	}

	private static Optional<Class<?>> findNamedType(Class<?> start) {
		for (Class<?> type = start; type != null; type = type.getSuperclass())
			if (type.getDeclaredAnnotation(JsonTypeName.class) != null)
				return Optional.of(type);
		return Optional.empty();
	}

	/** Every concrete class assignable to {@code type}, in class-name order. */
	public static <T> List<Class<? extends T>> types(Class<T> type) {
		return candidates(type);
	}

	private static <T> List<Class<? extends T>> candidates(Class<T> type) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(type));
		List<Class<? extends T>> types = new ArrayList<>();
		for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE))
			types.add(load(candidate.getBeanClassName(), type));
		types.sort(Comparator.comparing(Class::getName));
		return types;
	}

	private static <T> SortedMap<String, T> scan(Class<T> family) {
		SortedMap<String, T> members = new TreeMap<>();
		for (Class<? extends T> type : candidates(family)) {
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

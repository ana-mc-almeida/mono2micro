package pt.ist.socialsoftware.mono2micro.feature;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * A feature's one identity (D-025 point 3): its class. The class gives the feature its name, its {@code @JsonTypeName}, and the one
 * {@link FeatureFamily} that claims the class gives it its family. So a variable feature takes its id from its own class, and a requirement
 * naming it is the same id (D-019 point 7). Two features with one name, such as the Accesses representation and the Accesses Similarity kind,
 * stay two ids.
 *
 * <p>A runtime subclass, such as a proxy or a mock, has the id of the nearest class up its hierarchy that declares a name: the feature it stands
 * for.
 */
public final class FeatureId {

	private final Class<?> type;
	private final String name;

	private FeatureId(Class<?> type) {
		this.type = type;
		this.name = type.getDeclaredAnnotation(JsonTypeName.class).value();
	}

	/**
	 * @throws IllegalStateException
	 * if no class up {@code type}'s hierarchy declares a {@code @JsonTypeName}
	 */
	public static FeatureId of(Class<?> type) {
		return new FeatureId(SelfRegistration.namedType(type));
	}

	public static FeatureId ofFeature(Object feature) {
		return of(feature.getClass());
	}

	Class<?> getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FeatureId && ((FeatureId) other).type.equals(type);
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}

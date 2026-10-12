package pt.ist.socialsoftware.mono2micro.utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The shared scan: it finds every concrete class of a family, keys it by its
 * {@code @JsonTypeName} and keeps one prototype of each. Each test family below is its own
 * type, so no real family's scan ever sees these classes.
 */
public class SelfRegistrationTest {

	public abstract static class Fruit {
	}

	@JsonTypeName("Apple")
	public static class Apple extends Fruit {
	}

	@JsonTypeName("Pear")
	private static class Pear extends Fruit {
	}

	/** Abstract, so it is not a feature of its own and needs no name. */
	public abstract static class Citrus extends Fruit {
	}

	@JsonTypeName("Lemon")
	public static class Lemon extends Citrus {
	}

	public interface Unnamed {
	}

	/** Concrete but nameless: the mistake the scan must refuse to hide. */
	public static class Forgotten implements Unnamed {
	}

	public interface Twins {
	}

	@JsonTypeName("Same")
	public static class FirstTwin implements Twins {
	}

	@JsonTypeName("Same")
	public static class SecondTwin implements Twins {
	}

	@Test
	public void theScanFindsEveryConcreteClassOfTheFamilyKeyedByItsName() {
		Map<String, Fruit> fruits = SelfRegistration.prototypes(Fruit.class);

		assertThat(fruits).containsOnlyKeys("Apple", "Lemon", "Pear");
		assertThat(fruits.get("Apple")).isInstanceOf(Apple.class);
		assertThat(fruits.get("Pear")).isInstanceOf(Pear.class);
	}

	@Test
	public void theScanKeepsOnePrototypePerFeature() {
		assertThat(SelfRegistration.prototypes(Fruit.class).get("Apple"))
				.isSameAs(SelfRegistration.prototypes(Fruit.class).get("Apple"));
	}

	@Test
	public void theScanListsFeaturesInNameOrder() {
		assertThat(SelfRegistration.prototypes(Fruit.class).keySet()).containsExactly("Apple", "Lemon", "Pear");
	}

	@Test
	public void aFeatureWithoutAJsonTypeNameIsRefused() {
		assertThatThrownBy(() -> SelfRegistration.prototypes(Unnamed.class))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(Forgotten.class.getName());
	}

	@Test
	public void twoFeaturesWithTheSameNameAreRefused() {
		assertThatThrownBy(() -> SelfRegistration.prototypes(Twins.class))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Same");
	}

	@Test
	public void aFeaturesNameIsItsJsonTypeName() {
		assertThat(SelfRegistration.nameOf(new Lemon())).isEqualTo("Lemon");
	}
}

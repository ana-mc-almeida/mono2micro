package pt.ist.socialsoftware.mono2micro;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * Registers every class that declares a {@code @JsonTypeName} as a JSON subtype, so that a polymorphic request body such as a
 * {@code SimilarityDto}, its {@code Weights} or a {@code RecommendationDto} finds its subtypes by the self-registration scan, and adding one is
 * adding its class (D-019 point 8). Spring Boot adds every {@link Module} bean to the {@code ObjectMapper} it builds.
 */
@Configuration
public class JsonSubtypeConfiguration {

	@Bean
	public Module selfRegisteredSubtypes() {
		return subtypesModule();
	}

	/** The module of every self-registered subtype, for an {@code ObjectMapper} built outside Spring that reads requests as the controllers do. */
	public static Module subtypesModule() {
		SimpleModule module = new SimpleModule("selfRegisteredSubtypes");
		module.registerSubtypes(SelfRegistration.namedTypes().toArray(new Class<?>[0]));
		return module;
	}
}

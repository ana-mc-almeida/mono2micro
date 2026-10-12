package pt.ist.socialsoftware.mono2micro;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * The one place that knows every family of {@code VariableFeature}s (D-022). Runs each family's scan when the application starts, so a feature without a
 * {@code @JsonTypeName}, or two features sharing one, stops the boot instead of failing the first
 * availability request.
 */
@Component
public class FeatureRegistrationCheck implements InitializingBean {

	@Override
	public void afterPropertiesSet() {
		SelfRegistration.prototypes(SimilarityKind.class);
		SelfRegistration.prototypes(Clustering.class);
	}
}

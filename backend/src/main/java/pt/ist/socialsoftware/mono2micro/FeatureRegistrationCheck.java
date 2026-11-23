package pt.ist.socialsoftware.mono2micro;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * Runs the families' scans and {@link FeatureFamilies#verify} when the application starts (D-025 point 7), so a feature without a
 * {@code @JsonTypeName}, two features sharing one, a feature no family claims, or a requirement naming nothing stops the boot instead of failing
 * the first availability request.
 */
@Component
public class FeatureRegistrationCheck implements InitializingBean {

	@Override
	public void afterPropertiesSet() {
		FeatureFamilies.scanned().verify(SelfRegistration.types(VariableFeature.class));
	}
}

package pt.ist.socialsoftware.mono2micro.operation;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

/**
 * Stops the boot when some manual operation has no handler for some registered cluster kind, directly or by fallback (D-014), so the gap is
 * found when the application starts and not by a user's edit.
 */
@Component
public class ManualOperationHandlerCheck implements InitializingBean {

	@Override
	public void afterPropertiesSet() {
		ManualOperationHandlers.scanned().verify(SelfRegistration.types(Operation.class), SelfRegistration.types(Decomposition.class));
	}
}

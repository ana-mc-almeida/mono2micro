package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.stereotype.Component;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

/**
 * Deletes the operation that produced a representation when the representation is deleted, so no operation outlives its output. It listens for the
 * deletion rather than being called, because {@code representation/} never imports the operations (D-016).
 */
@Component
public class OperationOutputDeletion extends AbstractMongoEventListener<Representation> {

	@Autowired
	OperationRepository operationRepository;

	@Override
	public void onAfterDelete(AfterDeleteEvent<Representation> event) {
		Object id = event.getSource().get("_id");
		if (id != null)
			operationRepository.deleteByOutput_Id(id.toString());
	}
}

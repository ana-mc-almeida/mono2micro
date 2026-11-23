package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;

/**
 * Checks, on every save, that a cluster representation is measured with a Similarity of its own software system (D-015). Every save goes through
 * Spring Data, so none can skip the check.
 */
@Component
public class ClusterRepresentationSaveCheck extends AbstractMongoEventListener<ClusterRepresentation> {

	@Override
	public void onBeforeConvert(BeforeConvertEvent<ClusterRepresentation> event) {
		event.getSource().requireSimilarityOfItsSoftwareSystem();
	}
}

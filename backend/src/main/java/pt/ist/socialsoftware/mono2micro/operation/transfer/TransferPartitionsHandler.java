package pt.ist.socialsoftware.mono2micro.operation.transfer;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.Cluster;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.Partition;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.granularity.Element;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Moves entities from one partition to another; undo moves them back. */
public class TransferPartitionsHandler implements ManualOperationHandler<TransferOperation, PartitionsClusterRepresentation> {

	@Override
	public Class<TransferOperation> operation() {
		return TransferOperation.class;
	}

	@Override
	public Class<PartitionsClusterRepresentation> clusterKind() {
		return PartitionsClusterRepresentation.class;
	}

	@Override
	public void apply(TransferOperation operation, PartitionsClusterRepresentation decomposition) {
		transfer(operation, decomposition);
		decomposition.getRepresentationInformations().forEach(representationInformation -> representationInformation.removeFunctionalitiesWithEntityIDs(
				decomposition,
				Arrays.stream(operation.getEntities().split(",")).map(Short::valueOf).collect(Collectors.toSet())
		)
		);
	}

	@Override
	public void undo(TransferOperation operation, PartitionsClusterRepresentation decomposition) {
		apply(new TransferOperation(operation.getToCluster(), operation.getFromCluster(), operation.getEntities()), decomposition);
	}

	private void transfer(TransferOperation operation, PartitionsClusterRepresentation decomposition) {
		Cluster from = decomposition.getCluster(operation.getFromCluster());
		Cluster to = decomposition.getCluster(operation.getToCluster());
		Set<Short> entitiesList = Arrays.stream(operation.getEntities().split(",")).map(Short::valueOf).collect(Collectors.toSet());

		for (Short entityID : entitiesList) {
			Element entity = from.getElementByID(entityID);
			if (entity != null) {
				to.addElement(entity);
				from.removeElement(entity);
			}
		}

		for (Cluster cluster : decomposition.getClusters().values()) {
			Partition partition = (Partition) cluster;
			partition.transferCouplingDependencies(entitiesList, operation.getFromCluster(), operation.getToCluster());
		}
	}
}

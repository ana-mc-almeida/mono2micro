package pt.ist.socialsoftware.mono2micro.operation.transfer;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.granularity.Element;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Moves entities from one partition to another; undo moves them back. */
public class TransferPartitionsHandler implements ManualOperationHandler<TransferOperation, PartitionsDecomposition> {

	@Override
	public Class<TransferOperation> operation() {
		return TransferOperation.class;
	}

	@Override
	public Class<PartitionsDecomposition> clusterKind() {
		return PartitionsDecomposition.class;
	}

	@Override
	public void apply(TransferOperation operation, PartitionsDecomposition decomposition) {
		transfer(operation, decomposition);
		decomposition.getRepresentationInformations().forEach(representationInformation -> representationInformation.removeFunctionalitiesWithEntityIDs(
				decomposition,
				Arrays.stream(operation.getEntities().split(",")).map(Short::valueOf).collect(Collectors.toSet())
		)
		);
	}

	@Override
	public void undo(TransferOperation operation, PartitionsDecomposition decomposition) {
		apply(new TransferOperation(operation.getToCluster(), operation.getFromCluster(), operation.getEntities()), decomposition);
	}

	private void transfer(TransferOperation operation, PartitionsDecomposition decomposition) {
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

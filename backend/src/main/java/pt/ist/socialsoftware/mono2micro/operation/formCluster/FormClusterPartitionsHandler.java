package pt.ist.socialsoftware.mono2micro.operation.formCluster;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.granularity.Element;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandlers;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gathers entities from several partitions into a new one, dropping the partitions it empties; undo sends each back, splitting a dropped
 * partition out again.
 */
public class FormClusterPartitionsHandler implements ManualOperationHandler<FormClusterOperation, PartitionsDecomposition> {

	@Override
	public Class<FormClusterOperation> operation() {
		return FormClusterOperation.class;
	}

	@Override
	public Class<PartitionsDecomposition> clusterKind() {
		return PartitionsDecomposition.class;
	}

	@Override
	public void apply(FormClusterOperation operation, PartitionsDecomposition decomposition) {
		formCluster(operation, decomposition);
		decomposition.getRepresentationInformations().forEach(representationInformation -> representationInformation.removeFunctionalitiesWithEntityIDs(
				decomposition,
				operation.getEntities().values().stream().flatMap(Collection::stream).collect(Collectors.toSet())
		)
		);
	}

	@Override
	public void undo(FormClusterOperation operation, PartitionsDecomposition decomposition) {
		operation.getEntities().forEach((clusterName, entitiesID) -> {
			Cluster toCluster = decomposition.getClusters().get(clusterName);
			String entities = entitiesID.stream().map(Object::toString).collect(Collectors.joining(","));
			if (toCluster == null) // If there is no cluster, the operation is a split, if there is, it is a transfer
				ManualOperationHandlers.scanned().apply(new SplitOperation(operation.getNewCluster(), clusterName, entities), decomposition);
			else
				ManualOperationHandlers.scanned().apply(new TransferOperation(operation.getNewCluster(), toCluster.getName(), entities), decomposition);
		});

		decomposition.removeCluster(operation.getNewCluster());
	}

	private void formCluster(FormClusterOperation operation, PartitionsDecomposition decomposition) {
		String newCluster = operation.getNewCluster();
		List<Short> entitiesIDs = operation.getEntities().values().stream().flatMap(Collection::stream).collect(Collectors.toList());

		if (decomposition.clusterNameExists(newCluster)) {
			Cluster previousCluster = decomposition.getClusters().values().stream().filter(cluster -> cluster.getName().equals(newCluster)).findFirst()
					.orElseThrow(() -> new KeyAlreadyExistsException("Cluster with name: " + newCluster + " already exists"));
			if (!entitiesIDs.containsAll(previousCluster.getElementsIDs()))
				throw new KeyAlreadyExistsException("Cluster with name: " + newCluster + " already exists");
		}

		Cluster createdCluster = new Partition(newCluster);

		for (Short entityID : entitiesIDs) {
			Cluster currentCluster = decomposition.getClusters().values().stream()
					.filter(cluster -> cluster.containsElement(entityID)).findFirst()
					.orElseThrow(() -> new RuntimeException("No cluster contains entity " + entityID));

			Element entity = currentCluster.getElementByID(entityID);
			createdCluster.addElement(entity);
			currentCluster.removeElement(entityID);
			for (Cluster cluster : decomposition.getClusters().values()) {
				Partition partition = (Partition) cluster;
				partition.transferCouplingDependencies(Collections.singleton(entityID), currentCluster.getName(), createdCluster.getName());
			}

			if (currentCluster.getElements().size() == 0)
				decomposition.removeCluster(currentCluster.getName());
		}
		decomposition.addCluster(createdCluster);
	}
}

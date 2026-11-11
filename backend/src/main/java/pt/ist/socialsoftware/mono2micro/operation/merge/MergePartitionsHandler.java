package pt.ist.socialsoftware.mono2micro.operation.merge;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.granularity.Element;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandlers;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Merges two partitions into one; undo splits the second one's entities back out and restores the first one's name. */
public class MergePartitionsHandler implements ManualOperationHandler<MergeOperation, PartitionsDecomposition> {

	@Override
	public Class<MergeOperation> operation() {
		return MergeOperation.class;
	}

	@Override
	public Class<PartitionsDecomposition> clusterKind() {
		return PartitionsDecomposition.class;
	}

	@Override
	public void apply(MergeOperation operation, PartitionsDecomposition decomposition) {
		merge(operation, decomposition);
		decomposition.getRepresentationInformations().forEach(representationInformation -> representationInformation.removeFunctionalitiesWithEntityIDs(
				decomposition,
				decomposition.getCluster(operation.getNewName()).getElements().stream().map(Element::getId).collect(Collectors.toSet())
		)
		);
	}

	@Override
	public void undo(MergeOperation operation, PartitionsDecomposition decomposition) {
		ManualOperationHandlers handlers = ManualOperationHandlers.scanned();
		if (operation.getCluster1Name().equals(operation.getNewName())) {
			handlers.apply(new SplitOperation(operation.getNewName(), operation.getCluster2Name(), operation.getCluster2Entities()), decomposition);
			handlers.apply(new RenameOperation(operation.getNewName(), operation.getCluster1Name()), decomposition);
		} else {
			handlers.apply(new SplitOperation(operation.getNewName(), operation.getCluster1Name(), operation.getCluster1Entities()), decomposition);
			handlers.apply(new RenameOperation(operation.getNewName(), operation.getCluster2Name()), decomposition);
		}
	}

	private void merge(MergeOperation operation, PartitionsDecomposition decomposition) {
		String newName = operation.getNewName();
		Cluster cluster1 = decomposition.getCluster(operation.getCluster1Name());
		Cluster cluster2 = decomposition.getCluster(operation.getCluster2Name());
		if (decomposition.clusterNameExists(newName) && !cluster1.getName().equals(newName) && !cluster2.getName().equals(newName))
			throw new KeyAlreadyExistsException("Cluster with name: " + newName + " already exists");

		Cluster mergedCluster = new Partition(newName);

		Set<Element> allEntities = new HashSet<>(cluster1.getElements());
		allEntities.addAll(cluster2.getElements());
		mergedCluster.setElements(allEntities);

		for (Cluster cluster : decomposition.getClusters().values()) {
			Partition partition = (Partition) cluster;
			partition.transferCouplingDependencies(cluster1.getElementsIDs(), cluster1.getName(), mergedCluster.getName());
			partition.transferCouplingDependencies(cluster2.getElementsIDs(), cluster2.getName(), mergedCluster.getName());
		}

		decomposition.removeCluster(operation.getCluster1Name());
		decomposition.removeCluster(operation.getCluster2Name());
		decomposition.addCluster(mergedCluster);
	}
}

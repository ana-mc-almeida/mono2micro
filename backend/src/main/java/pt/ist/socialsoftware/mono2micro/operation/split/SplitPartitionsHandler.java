package pt.ist.socialsoftware.mono2micro.operation.split;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.granularity.Element;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandlers;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.stream.Collectors;

/** Moves some of a partition's entities into a new partition; undo merges it back under the original name. */
public class SplitPartitionsHandler implements ManualOperationHandler<SplitOperation, PartitionsDecomposition> {

	@Override
	public Class<SplitOperation> operation() {
		return SplitOperation.class;
	}

	@Override
	public Class<PartitionsDecomposition> clusterKind() {
		return PartitionsDecomposition.class;
	}

	@Override
	public void apply(SplitOperation operation, PartitionsDecomposition decomposition) {
		split(operation, decomposition);
		decomposition.getRepresentationInformations().forEach(representationInformation -> representationInformation.removeFunctionalitiesWithEntityIDs(
				decomposition,
				decomposition.getCluster(operation.getNewCluster()).getElements().stream().map(Element::getId).collect(Collectors.toSet())
		)
		);
	}

	@Override
	public void undo(SplitOperation operation, PartitionsDecomposition decomposition) {
		ManualOperationHandlers.scanned().apply(
				new MergeOperation(operation.getOriginalCluster(), operation.getNewCluster(), operation.getOriginalCluster()), decomposition);
	}

	private void split(SplitOperation operation, PartitionsDecomposition decomposition) {
		String[] entitiesList = operation.getEntities().split(",");
		if (decomposition.clusterNameExists(operation.getNewCluster()))
			throw new KeyAlreadyExistsException("Cluster with name: " + operation.getNewCluster() + " already exists");

		Cluster currentCluster = decomposition.getCluster(operation.getOriginalCluster());
		Cluster createdCluster = new Partition(operation.getNewCluster());

		for (String stringifiedEntityID : entitiesList) {
			Element entity = currentCluster.getElementByID(Short.parseShort(stringifiedEntityID));

			if (entity != null) {
				createdCluster.addElement(entity);
				currentCluster.removeElement(entity);
			}
		}
		for (Cluster cluster : decomposition.getClusters().values()) {
			Partition partition = (Partition) cluster;
			partition.transferCouplingDependencies(createdCluster.getElementsIDs(), currentCluster.getName(), createdCluster.getName());
		}

		decomposition.addCluster(createdCluster);
	}
}

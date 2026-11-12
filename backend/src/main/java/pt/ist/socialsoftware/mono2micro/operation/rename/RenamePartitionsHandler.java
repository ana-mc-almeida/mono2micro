package pt.ist.socialsoftware.mono2micro.operation.rename;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Set;

/** Renames a partition and the coupling dependencies and functionalities that name it. */
public class RenamePartitionsHandler implements ManualOperationHandler<RenameOperation, PartitionsDecomposition> {

	@Override
	public Class<RenameOperation> operation() {
		return RenameOperation.class;
	}

	@Override
	public Class<PartitionsDecomposition> clusterKind() {
		return PartitionsDecomposition.class;
	}

	@Override
	public void apply(RenameOperation operation, PartitionsDecomposition decomposition) {
		rename(operation.getClusterName(), operation.getNewClusterName(), decomposition);
		decomposition.getRepresentationInformations()
				.forEach(representationInformation -> representationInformation.renameClusterInFunctionalities(operation.getClusterName(),
						operation.getNewClusterName())
				);
	}

	@Override
	public void undo(RenameOperation operation, PartitionsDecomposition decomposition) {
		apply(new RenameOperation(operation.getNewClusterName(), operation.getClusterName()), decomposition);
	}

	private void rename(String clusterName, String newClusterName, PartitionsDecomposition decomposition) {
		if (decomposition.clusterNameExists(newClusterName) && !clusterName.equals(newClusterName))
			throw new KeyAlreadyExistsException("Cluster with name: " + newClusterName + " already exists");

		Cluster oldCluster = decomposition.removeCluster(clusterName);
		oldCluster.setName(newClusterName);
		decomposition.addCluster(oldCluster);

		// Change coupling dependencies
		decomposition.getClusters().forEach((s, cluster) -> {
			Partition partition = (Partition) cluster;
			Set<Short> dependencies = partition.getCouplingDependencies().get(clusterName);
			if (dependencies != null) {
				partition.getCouplingDependencies().remove(clusterName);
				partition.addCouplingDependencies(newClusterName, dependencies);
			}
		});
	}
}

package pt.ist.socialsoftware.mono2micro.operation.rename;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.Cluster;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.Partition;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.operation.ManualOperationHandler;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Set;

/** Renames a partition and the coupling dependencies and functionalities that name it. */
public class RenamePartitionsHandler implements ManualOperationHandler<RenameOperation, PartitionsClusterRepresentation> {

	@Override
	public Class<RenameOperation> operation() {
		return RenameOperation.class;
	}

	@Override
	public Class<PartitionsClusterRepresentation> clusterKind() {
		return PartitionsClusterRepresentation.class;
	}

	@Override
	public void apply(RenameOperation operation, PartitionsClusterRepresentation decomposition) {
		rename(operation.getClusterName(), operation.getNewClusterName(), decomposition);
		decomposition.getRepresentationInformations()
				.forEach(representationInformation -> representationInformation.renameClusterInFunctionalities(operation.getClusterName(),
						operation.getNewClusterName())
				);
	}

	@Override
	public void undo(RenameOperation operation, PartitionsClusterRepresentation decomposition) {
		apply(new RenameOperation(operation.getNewClusterName(), operation.getClusterName()), decomposition);
	}

	private void rename(String clusterName, String newClusterName, PartitionsClusterRepresentation decomposition) {
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

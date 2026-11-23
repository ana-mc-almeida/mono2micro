import ClusterRepresentation from "./ClusterRepresentation";
import PartitionsClusterRepresentation, {PARTITIONS_CLUSTER_REPRESENTATION} from "./PartitionsClusterRepresentation";

export abstract class ClusterRepresentationFactory {
    static getClusterRepresentation(decomposition: any) : ClusterRepresentation {
        switch (decomposition.type) {
            case PARTITIONS_CLUSTER_REPRESENTATION:
                return new PartitionsClusterRepresentation(decomposition);
            default:
                throw new Error('Type ' + decomposition.type + ' unknown.');
        }
    }
}
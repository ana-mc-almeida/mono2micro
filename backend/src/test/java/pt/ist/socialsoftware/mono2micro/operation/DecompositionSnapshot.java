package pt.ist.socialsoftware.mono2micro.operation;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.element.Element;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The shape of a {@link Decomposition}, reduced to two comparable maps.
 *
 * <p>Exists because {@code Cluster} declares no {@code equals}, so comparing two
 * {@code Map<String, Cluster>} values compares references and passes even when the contents
 * differ. Undo round-trip tests need a real comparison, so this projects a decomposition onto
 * the two things the operations actually change:
 *
 * <ul>
 *   <li>cluster name → the entity IDs it holds
 *   <li>cluster name → (the cluster it reaches → the entity IDs it reaches there)
 * </ul>
 *
 * <p>Sorted collections throughout, for two reasons: AssertJ's failure message on a mismatched
 * {@code TreeMap} reads as an ordered diff, and {@code HashSet} iteration order otherwise adds
 * noise that has nothing to do with the operation under test.
 *
 * <p><b>Deliberately not compared.</b> {@code Cluster.metrics}, because {@code merge} builds a
 * fresh {@code Partition} and so drops the metrics of both inputs — real lossiness, but a
 * separate finding, and including it here would make every merge round-trip fail for a reason
 * unrelated to the cluster arithmetic being tested. Also {@code Decomposition.outdated}, which
 * only {@code AccessesInformation} sets and which no operation restores on undo.
 */
final class DecompositionSnapshot {

    private final Map<String, Set<Short>> clusters;
    private final Map<String, Map<String, Set<Short>>> dependencies;

    private DecompositionSnapshot(Map<String, Set<Short>> clusters,
                                  Map<String, Map<String, Set<Short>>> dependencies) {
        this.clusters = clusters;
        this.dependencies = dependencies;
    }

    static DecompositionSnapshot of(Decomposition decomposition) {
        Map<String, Set<Short>> clusters = new TreeMap<>();
        Map<String, Map<String, Set<Short>>> dependencies = new TreeMap<>();

        for (Map.Entry<String, Cluster> entry : decomposition.getClusters().entrySet()) {
            Cluster cluster = entry.getValue();

            clusters.put(entry.getKey(), cluster.getElements().stream()
                    .map(Element::getId)
                    .collect(Collectors.toCollection(TreeSet::new)));

            // Deep-copied: the maps and sets below are live in the decomposition, and an
            // operation run after the snapshot would otherwise mutate it retroactively.
            Map<String, Set<Short>> copied = new TreeMap<>();
            ((Partition) cluster).getCouplingDependencies()
                    .forEach((toCluster, entityIDs) -> copied.put(toCluster, new TreeSet<>(entityIDs)));
            dependencies.put(entry.getKey(), copied);
        }

        return new DecompositionSnapshot(clusters, dependencies);
    }

    /** Cluster name → the entity IDs it holds. */
    Map<String, Set<Short>> clusters() {
        return clusters;
    }

    /** Cluster name → (reached cluster → the entity IDs reached there). */
    Map<String, Map<String, Set<Short>>> dependencies() {
        return dependencies;
    }
}

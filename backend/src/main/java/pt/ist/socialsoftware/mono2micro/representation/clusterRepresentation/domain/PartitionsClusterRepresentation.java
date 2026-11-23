package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.fileManager.ContextManager;
import pt.ist.socialsoftware.mono2micro.granularity.DomainEntity;
import pt.ist.socialsoftware.mono2micro.history.domain.PositionHistory;
import pt.ist.socialsoftware.mono2micro.history.service.HistoryService;
import pt.ist.socialsoftware.mono2micro.history.service.PositionHistoryService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.*;

@JsonTypeName(PartitionsClusterRepresentation.PARTITIONS_CLUSTER_REPRESENTATION)
@Document("representation")
public class PartitionsClusterRepresentation extends ClusterRepresentation {
	public static final String PARTITIONS_CLUSTER_REPRESENTATION = "Partitions Cluster Representation";

	public PartitionsClusterRepresentation() { this.type = PARTITIONS_CLUSTER_REPRESENTATION; }

	public PartitionsClusterRepresentation(Similarity similarity) {
		super(similarity);
		this.type = PARTITIONS_CLUSTER_REPRESENTATION;
	}

	public PartitionsClusterRepresentation(PartitionsClusterRepresentation decomposition, String snapshotName) throws Exception {
		this.type = PARTITIONS_CLUSTER_REPRESENTATION;
		this.name = snapshotName;
		setSimilarity(decomposition.getSimilarity());
		this.metrics = decomposition.getMetrics();
		this.outdated = decomposition.isOutdated();
		this.expert = decomposition.isExpert();
		this.clusters = decomposition.getClusters();
		List<RepresentationInformation> representationInformations = decomposition.getSimilarity().informationBlocks();
		for (RepresentationInformation representationInformation : representationInformations)
			representationInformation.snapshot(this, decomposition);
	}

	/**
	 * Adds a cluster for each entry of {@code clustersJSON}, which maps a cluster's name to its entities' ids, in name order. An imported
	 * representation keeps the names it was given; otherwise each name is prefixed with "Cluster".
	 */
	public void addClusters(JSONObject clustersJSON, Map<Short, String> idToEntity) throws JSONException {
		Iterator<String> names = clustersJSON.sortedKeys();
		List<String> clusterNames = new ArrayList<>();
		while (names.hasNext())
			clusterNames.add(names.next());
		Collections.sort(clusterNames);

		for (String name : clusterNames) {
			JSONArray entities = clustersJSON.getJSONArray(name);
			Partition cluster = new Partition(isExpert() ? name : "Cluster" + name);
			for (int i = 0; i < entities.length(); i++) {
				short entityID = (short) entities.getInt(i);
				cluster.addElement(new DomainEntity(entityID, idToEntity.get(entityID)));
			}
			addCluster(cluster);
		}
	}

	@Override
	public void calculateMetrics() {
		this.representationInformations.stream()
				.map(RepresentationInformation::getDecompositionMetrics)
				.flatMap(Collection::stream)
				.forEach(metric -> this.metrics.put(metric.getType(), metric.calculateMetric(this)));
	}

	@Override
	public void setup() throws Exception {
		List<RepresentationInformation> representationInformations = getSimilarity().informationBlocks();
		for (RepresentationInformation representationInformation : representationInformations)
			representationInformation.setup(this);
		this.history = new PositionHistory(this);
	}

	@Override
	public void update() throws Exception {
		for (RepresentationInformation representationInformation : representationInformations)
			representationInformation.update(this);
	}

	@Override
	public void deleteProperties() {
		representationInformations.forEach(RepresentationInformation::deleteProperties);
	}

	@Override
	public ClusterRepresentation snapshotDecomposition(String snapshotName) throws Exception {
		HistoryService historyService = ContextManager.get().getBean(HistoryService.class);
		PositionHistoryService positionHistoryService = ContextManager.get().getBean(PositionHistoryService.class);

		PartitionsClusterRepresentation snapshotDecomposition = new PartitionsClusterRepresentation(this, snapshotName);

		PositionHistory snapshotHistory = new PositionHistory(snapshotDecomposition);
		snapshotDecomposition.setHistory(snapshotHistory);
		historyService.saveHistory(snapshotHistory);
		positionHistoryService.saveGraphPositions(snapshotDecomposition, positionHistoryService.getGraphPositions(this));

		return snapshotDecomposition;
	}
}

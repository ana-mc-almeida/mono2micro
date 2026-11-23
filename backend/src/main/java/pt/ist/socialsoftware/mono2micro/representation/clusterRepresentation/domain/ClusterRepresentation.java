package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.granularity.DomainEntity;
import pt.ist.socialsoftware.mono2micro.granularity.Element;
import pt.ist.socialsoftware.mono2micro.history.domain.History;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.*;

/**
 * A representation made of clusters, such as a candidate microservices decomposition (D-010). It is one kind of {@link Representation}, stored with
 * the others, and is either imported (an expert's) or produced by a clustering algorithm.
 *
 * <p>It is measured with a Similarity of its own software system, which it keeps for good (D-015): its metrics, functionalities and edge weights
 * read their settings through it.
 */
@Document("representation")
public abstract class ClusterRepresentation extends Representation {
	String type;
	boolean expert;
	boolean outdated; // Used to avoid long waiting times during interaction
	Map<String, Object> metrics = new HashMap<>(); // Map<MetricCalculator type, MetricCalculator value>
	Map<String, Cluster> clusters = new HashMap<>();
	/** Eager: the information blocks downcast it, and a lazy reference to an abstract type cannot be downcast (D-024). */
	@DBRef
	Similarity similarity;
	@DBRef
	History history;

	List<RepresentationInformation> representationInformations = new ArrayList<>();

	public ClusterRepresentation() {}

	/** A cluster representation of {@code similarity}'s software system, measured with it. */
	public ClusterRepresentation(Similarity similarity) {
		setSimilarity(similarity);
	}

	/** A cluster representation is imported or produced, never read from an uploaded representation file. */
	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) {
		throw new UnsupportedOperationException("A cluster representation is not read from a representation file.");
	}

	/**
	 * Fails unless it is measured with a Similarity of its own software system (D-015). Checked whenever it is saved.
	 *
	 * @throws IllegalStateException
	 *                               if it has no Similarity or no software system, or its Similarity belongs to another software system
	 */
	public void requireSimilarityOfItsSoftwareSystem() {
		if (similarity == null)
			throw new IllegalStateException("Cluster representation " + id + " is measured with no Similarity.");
		if (softwareSystem == null)
			throw new IllegalStateException("Cluster representation " + id + " belongs to no software system.");
		String similaritySoftwareSystemId = similarity.getSoftwareSystem().getId();
		if (!softwareSystem.getId().equals(similaritySoftwareSystemId))
			throw new IllegalStateException("Cluster representation " + id + " belongs to software system " + softwareSystem.getId()
					+ ", but its Similarity " + similarity.getId() + " to " + similaritySoftwareSystemId + ".");
	}

	public abstract void setup() throws Exception;

	public abstract void update() throws Exception;

	public abstract void deleteProperties();

	public abstract void calculateMetrics();

	public abstract ClusterRepresentation snapshotDecomposition(String decompositionName) throws Exception;

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isExpert() {
		return expert;
	}

	public void setExpert(boolean expert) {
		this.expert = expert;
	}

	public boolean isOutdated() {
		return outdated;
	}

	public void setOutdated(boolean outdated) {
		this.outdated = outdated;
	}

	public Map<String, Object> getMetrics() {
		return metrics;
	}

	public void setMetrics(Map<String, Object> metrics) {
		this.metrics = metrics;
	}

	public void addMetric(String metricType, Object metricValue) {
		this.metrics.put(metricType, metricValue);
	}

	public List<RepresentationInformation> getRepresentationInformations() {
		return representationInformations;
	}

	public void setRepresentationInformations(List<RepresentationInformation> representationInformations) {
		this.representationInformations = representationInformations;
	}

	public void addRepresentationInformation(RepresentationInformation representationInformation) {
		this.representationInformations.add(representationInformation);
	}

	public RepresentationInformation getRepresentationInformationByType(String type) {
		return this.representationInformations.stream().filter(r -> r.getType().equals(type)).findFirst().orElse(null);
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	/** Sets the Similarity it is measured with, and so its software system, which is the Similarity's. */
	public void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
		this.softwareSystem = similarity.getSoftwareSystem();
	}

	public History getHistory() {
		return history;
	}

	public void setHistory(History history) {
		this.history = history;
	}

	public Map<String, Cluster> getClusters() { return this.clusters; }

	public void setClusters(Map<String, Cluster> clusters) { this.clusters = clusters; }

	public void addCluster(Cluster cluster) {
		Cluster c = this.clusters.putIfAbsent(cluster.getName(), cluster);

		if (c != null) throw new Error("Cluster with name: " + cluster.getName() + " already exists");
	}

	public Cluster removeCluster(String clusterName) {
		Cluster c = this.clusters.remove(clusterName);

		if (c == null) throw new Error("Cluster with name: " + clusterName + " not found");

		return c;
	}

	public Cluster getCluster(String clusterName) {
		Cluster c = this.clusters.get(clusterName);

		if (c == null) throw new Error("Cluster with name: " + clusterName + " not found");

		return c;
	}

	public boolean clusterNameExists(String clusterName) {
		for (String cluster : this.clusters.keySet())
			if (cluster.equals(clusterName))
				return true;
		return false;
	}

	public int maxClusterSize() {
		int max = 0;

		for (Cluster cluster : this.clusters.values()) {
			if (cluster.getElements().size() > max)
				max = cluster.getElements().size();
		}

		return max;
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

	public Map<Short, String> getEntityIDToClusterName() {
		Map<Short, String> entityIDToClusterName = new HashMap<>();
		for (Cluster cluster : this.clusters.values())
			for (Element element : cluster.getElements())
				entityIDToClusterName.put(element.getId(), cluster.getName());
		return entityIDToClusterName;
	}

	public String getEdgeWeights(String representationInfo) throws Exception {
		RepresentationInformation representationInformation = getRepresentationInformationByType(representationInfo);
		return representationInformation.getEdgeWeights(this);
	}

	public String getSearchItems(String representationInfo) throws Exception {
		RepresentationInformation representationInformation = getRepresentationInformationByType(representationInfo);
		return representationInformation.getSearchItems(this);
	}
}
package pt.ist.socialsoftware.mono2micro.decomposition;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Drives the pipeline over the REST API, exactly as the frontend does.
 *
 * <p>This is the regression net. It is pinned only to HTTP paths, JSON shapes and type
 * strings, so an internal refactor of the backend should leave it untouched. When a
 * restructure ticket changes an endpoint, it changes that endpoint's path and payload here,
 * and never the assertions of the tests that call it. It is also the
 * only implementation that exercises the polymorphic {@code @JsonSubTypes} dispatch on the
 * {@code type} discriminator — the fan-out that adding a new similarity variant has to get
 * right in five registries at once.
 */
class HttpDecompositionPipeline implements DecompositionPipeline {

	private final TestRestTemplate rest;
	private final String base;

	/** The fixture folder to read representation files from, e.g. {@code quizzes-tutor}. */
	private final String caseName;

	HttpDecompositionPipeline(TestRestTemplate rest, int port, String caseName) {
		this.rest = rest;
		this.base = "http://localhost:" + port + "/mono2micro";
		this.caseName = caseName;
	}

	@Override
	public String layer() {
		return "http";
	}

	@Override
	public String createSoftwareSystem(String softwareSystemName) {
		ResponseEntity<JsonNode> response = rest.postForEntity(
				base + "/softwareSystems/create?softwareSystemName={name}", null, JsonNode.class, softwareSystemName);

		require(response, HttpStatus.CREATED, "create software system " + softwareSystemName);
		return response.getBody().path("id").asText();
	}

	@Override
	public void addRepresentations(String softwareSystemId, StrategyCase strategyCase) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

		// Index-aligned repeated parts: representationTypes[i] describes representations[i].
		// LinkedHashMap keeps the group's declared order, which the backend relies on only in
		// that it pairs the two lists by position.
		for (Map.Entry<String, String> representation : strategyCase.representations().entrySet()) {
			byte[] content = RepresentationFiles.read(caseName, representation.getValue());
			form.add("representationTypes", representation.getKey());
			form.add("representations", filePart(content, caseName + representation.getValue()));
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity<String> response = rest.exchange(
				base + "/softwareSystems/{id}/addRepresentations/{group}",
				HttpMethod.POST,
				new HttpEntity<>(form, headers),
				String.class,
				softwareSystemId,
				strategyCase.representationGroup());

		require(response, HttpStatus.CREATED, "add representations to " + softwareSystemId);
	}

	@Override
	public String createStrategy(String softwareSystemId, StrategyCase strategyCase) {
		// strategyTypes is a repeated query parameter, so a multi-type strategy such as
		// Repository sends it twice rather than as one comma-joined value.
		StringBuilder url = new StringBuilder(
				base + "/softwareSystems/{id}/createStrategy?algorithmType={algorithm}");
		List<Object> uriVariables = new ArrayList<>();
		uriVariables.add(softwareSystemId);
		uriVariables.add("SciPy Clustering");

		for (String strategyType : strategyCase.strategyTypes()) {
			url.append("&strategyTypes={type").append(uriVariables.size()).append("}");
			uriVariables.add(strategyType);
		}

		ResponseEntity<JsonNode> response = rest.postForEntity(
				url.toString(), null, JsonNode.class, uriVariables.toArray());

		require(response, HttpStatus.CREATED, "create strategy for " + softwareSystemId);

		return response.getBody().path("id").asText();
	}

	@Override
	public String createSimilarity(String strategyId, StrategyCase strategyCase) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", strategyCase.similarityType());
		body.put("strategyId", strategyId);
		body.put("linkageType", "average");
		body.putAll(strategyCase.extraSimilarityFields());

		// Class and Entity Vectorization take no weights: their DTOs have no such field, and
		// sending one would be silently dropped rather than rejected.
		if (!strategyCase.weights().isEmpty())
			body.put("weightsList", strategyCase.weights());

		ResponseEntity<JsonNode> response = rest.exchange(
				base + "/similarity/create",
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				JsonNode.class);

		require(response, HttpStatus.CREATED, "create similarity for " + strategyId);

		// A duplicate request also returns 201, with the existing similarity.
		return response.getBody().path("id").asText();
	}

	/** The status of a Similarity request, for a test that expects it refused. */
	HttpStatus requestSimilarity(String strategyId, StrategyCase strategyCase) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", strategyCase.similarityType());
		body.put("strategyId", strategyId);
		body.put("linkageType", "average");
		body.putAll(strategyCase.extraSimilarityFields());
		if (!strategyCase.weights().isEmpty())
			body.put("weightsList", strategyCase.weights());

		return rest.exchange(base + "/similarity/create", HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), String.class).getStatusCode();
	}

	@Override
	public String createDecomposition(String similarityId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "SciPy Clustering");
		body.put("similarityId", similarityId);
		body.put("cutType", "N");
		body.put("cutValue", 3);

		ResponseEntity<String> response = rest.exchange(
				base + "/similarity/createDecomposition",
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				String.class);

		// 200, not 201: DecompositionController returns OK here while every other create
		// endpoint in the pipeline returns CREATED.
		require(response, HttpStatus.OK, "create decomposition for " + similarityId);

		JsonNode decompositions = getJson("/similarity/{id}/decompositions", similarityId);
		return newestId(decompositions, "decomposition", similarityId);
	}

	@Override
	public Map<String, List<String>> getClusters(String decompositionId) {
		return clusterNames(getJson("/decomposition/{id}/getClusters", decompositionId));
	}

	/**
	 * Cluster name → sorted entity names, from any JSON whose elements are clusters with a
	 * {@code name} and {@code elements} — the clusters endpoint's object and the export's array
	 * alike, so a change to that shape is one change here.
	 */
	static Map<String, List<String>> clusterNames(JsonNode clusters) {
		Map<String, List<String>> names = new TreeMap<>();
		for (JsonNode cluster : clusters) {
			List<String> entities = new ArrayList<>();
			cluster.path("elements").forEach(element -> entities.add(element.path("name").asText()));
			Collections.sort(entities);
			names.put(cluster.path("name").asText(), entities);
		}
		return names;
	}

	/** Entity name → id, read from the decomposition's clusters. Edit requests address entities by id. */
	Map<String, Short> entityIds(String decompositionId) {
		Map<String, Short> ids = new TreeMap<>();
		for (JsonNode cluster : getJson("/decomposition/{id}/getClusters", decompositionId))
			for (JsonNode element : cluster.path("elements")) {
				Short previous = ids.put(element.path("name").asText(), (short) element.path("id").asInt());
				if (previous != null)
					throw new AssertionError("Entity " + element.path("name").asText() + " appears more than once in " + decompositionId);
			}
		return ids;
	}

	// Manual editing. Each body is the one ClusterView.js sends. The entity lists of split and
	// transfer are comma-joined id strings, while form cluster takes cluster name → id array.

	void merge(String decompositionId, String cluster1, String cluster2, String newName) {
		Map<String, Object> body = operation("MergeOperation");
		body.put("cluster1Name", cluster1);
		body.put("cluster2Name", cluster2);
		body.put("newName", newName);
		edit(decompositionId, "merge", body);
	}

	void split(String decompositionId, String originalCluster, String newCluster, List<Short> entityIds) {
		Map<String, Object> body = operation("SplitOperation");
		body.put("originalCluster", originalCluster);
		body.put("newCluster", newCluster);
		body.put("entities", joined(entityIds));
		edit(decompositionId, "split", body);
	}

	void transfer(String decompositionId, String fromCluster, String toCluster, List<Short> entityIds) {
		Map<String, Object> body = operation("TransferOperation");
		body.put("fromCluster", fromCluster);
		body.put("toCluster", toCluster);
		body.put("entities", joined(entityIds));
		edit(decompositionId, "transfer", body);
	}

	void rename(String decompositionId, String clusterName, String newClusterName) {
		Map<String, Object> body = operation("RenameOperation");
		body.put("clusterName", clusterName);
		body.put("newClusterName", newClusterName);
		edit(decompositionId, "rename", body);
	}

	void formCluster(String decompositionId, String newCluster, Map<String, List<Short>> entityIdsByCluster) {
		Map<String, Object> body = operation("FormClusterOperation");
		body.put("newCluster", newCluster);
		body.put("entities", entityIdsByCluster);
		edit(decompositionId, "formCluster", body);
	}

	/** Returns the status, since an undo with nothing to undo is itself characterized. */
	HttpStatus undo(String decompositionId) {
		return rest.getForEntity(base + "/history/{id}/undoOperation", String.class, decompositionId).getStatusCode();
	}

	/** Returns the status, since a redo with nothing to redo is itself characterized. */
	HttpStatus redo(String decompositionId) {
		return rest.getForEntity(base + "/history/{id}/redoOperation", String.class, decompositionId).getStatusCode();
	}

	/** {@code {"undo": bool, "redo": bool}}. */
	JsonNode canUndoRedo(String decompositionId) {
		return getJson("/history/{id}/canUndoRedo", decompositionId);
	}

	// Quality assessment.

	/** The decomposition as stored: metrics are those of its last calculation. */
	JsonNode getDecomposition(String decompositionId) {
		return getJson("/decomposition/{id}", decompositionId);
	}

	/** The decomposition after recalculating its metrics if an edit made them outdated. */
	JsonNode updateDecomposition(String decompositionId) {
		return getJson("/decomposition/{id}/update", decompositionId);
	}

	/** Functionality metrics, keyed by functionality name under {@code functionalities}. */
	JsonNode getFunctionalities(String decompositionId) {
		return getJson("/accesses/{id}/getFunctionalitiesAndFunctionalitiesClusters", decompositionId);
	}

	/** The names of the decomposition's functionalities, sorted. */
	List<String> functionalityNames(String decompositionId) {
		List<String> names = new ArrayList<>();
		getFunctionalities(decompositionId).path("functionalities").forEach(functionality -> names.add(functionality.path("name").asText()));
		Collections.sort(names);
		return names;
	}

	/** MoJo and Purity of the second decomposition against the first, under {@code analysisList}. */
	JsonNode compare(String decomposition1Id, String decomposition2Id) {
		ResponseEntity<JsonNode> response = rest.postForEntity(
				base + "/comparison/{one}/{two}", null, JsonNode.class, decomposition1Id, decomposition2Id);
		require(response, HttpStatus.OK, "compare " + decomposition1Id + " with " + decomposition2Id);
		return response.getBody();
	}

	// Expert import.

	/**
	 * Uploads an expert clustering for a similarity, and returns the decomposition it creates.
	 *
	 * <p>The file is {@code {"clusters": {name: [entity id, ...]}}}.
	 */
	String createExpertDecomposition(String similarityId, String expertName, byte[] expertFile) {
		ResponseEntity<String> response = importExpertClustering(similarityId, expertName, expertFile);

		require(response, HttpStatus.OK, "import expert clustering " + expertName + " for " + similarityId);

		for (JsonNode decomposition : getDecompositions(similarityId))
			if (decomposition.path("expert").asBoolean() && decomposition.path("name").asText().endsWith(" " + expertName))
				return decomposition.path("id").asText();

		throw new AssertionError("The expert import of " + expertName + " returned success, but no expert decomposition exists for "
				+ similarityId + ".");
	}

	/** Uploads an expert clustering and returns the response as it is, for tests of a refused import. */
	ResponseEntity<String> importExpertClustering(String similarityId, String expertName, byte[] expertFile) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("expertName", expertName);
		form.add("expertFile", filePart(expertFile, expertName + ".json"));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		return rest.exchange(
				base + "/similarity/{id}/createExpertDecomposition",
				HttpMethod.POST,
				new HttpEntity<>(form, headers),
				String.class,
				similarityId);
	}

	JsonNode getDecompositions(String similarityId) {
		return getJson("/similarity/{id}/decompositions", similarityId);
	}

	// Export to Context Mapper.

	/** The raw response: the status is the export gate, and the body is a zip when it opens. */
	ResponseEntity<byte[]> export(String decompositionId) {
		return rest.getForEntity(base + "/decomposition/{id}/export", byte[].class, decompositionId);
	}

	/**
	 * Asks the functionality refactoring tool to refactor every functionality of a
	 * decomposition, which is what the export reads alongside the Structure representation.
	 *
	 * <p>The tool answers at once and keeps working in the background, so the export is not
	 * ready until it finishes. The body is what the frontend's {@code refactorSoftwareSystem} sends.
	 */
	void refactorFunctionalities(String refactorToolAddress, String decompositionId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("decomposition_id", decompositionId);
		body.put("functionality_names", new ArrayList<>());
		body.put("data_dependence_threshold", 0);
		body.put("minimize_sum_of_complexities", false);
		body.put("refactor_time_out_secs", 5);
		body.put("override_previous", true);

		ResponseEntity<String> response = rest.exchange(
				refactorToolAddress + "/api/v1/refactor",
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				String.class);

		require(response, HttpStatus.OK, "refactor the functionalities of " + decompositionId);
	}

	String representationId(String softwareSystemId, String representationType) {
		return getJson("/softwareSystems/{id}/representation/{type}/getSoftwareSystemRepresentation", softwareSystemId, representationType)
				.path("id").asText();
	}

	JsonNode getRepresentation(String representationId) {
		return getJson("/representation/{id}/getRepresentation", representationId);
	}

	/** Returns the new profile's id. */
	String createProfile(String representationId, String profileName) {
		ResponseEntity<JsonNode> response = rest.postForEntity(
				base + "/representation/{id}/addProfile?profile={name}", null, JsonNode.class, representationId, profileName);

		require(response, HttpStatus.CREATED, "create profile " + profileName);
		return response.getBody().path("id").asText();
	}

	/** {@code {profiles: [{id, name, units}], unassigned: [...]}} */
	JsonNode getProfiles(String representationId) {
		return getJson("/representation/{id}/getProfiles", representationId);
	}

	/** A null target moves the units to Unassigned. */
	void moveUnits(String representationId, List<String> units, String targetProfileId) {
		String url = base + "/representation/{id}/moveUnits" + (targetProfileId == null ? "" : "?targetProfileId=" + targetProfileId);
		ResponseEntity<String> response = rest.exchange(url, HttpMethod.POST, new HttpEntity<>(units, jsonHeaders()), String.class, representationId);

		require(response, HttpStatus.OK, "move " + units + " to " + targetProfileId);
	}

	void deleteProfile(String profileId) {
		ResponseEntity<String> response = rest.exchange(base + "/profile/{id}/delete", HttpMethod.DELETE, null, String.class, profileId);

		require(response, HttpStatus.OK, "delete profile " + profileId);
	}

	@Override
	public int representationCount(String softwareSystemId) {
		return getJson("/softwareSystems/{id}/getRepresentations", softwareSystemId).size();
	}

	@Override
	public void deleteSoftwareSystem(String softwareSystemId) {
		rest.delete(base + "/softwareSystems/{id}/delete", softwareSystemId);
	}

	private Map<String, Object> operation(String type) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", type);
		return body;
	}

	private void edit(String decompositionId, String endpoint, Map<String, Object> body) {
		ResponseEntity<String> response = rest.exchange(
				base + "/decomposition/{id}/" + endpoint,
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				String.class,
				decompositionId);

		require(response, HttpStatus.OK, endpoint + " on " + decompositionId + " with " + body);
	}

	private static String joined(List<Short> ids) {
		return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	private HttpEntity<ByteArrayResource> filePart(byte[] content, String filename) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ByteArrayResource resource = new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
		return new HttpEntity<>(resource, headers);
	}

	private HttpHeaders jsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	private JsonNode getJson(String path, Object... uriVariables) {
		ResponseEntity<JsonNode> response = rest.getForEntity(base + path, JsonNode.class, uriVariables);
		require(response, HttpStatus.OK, "GET " + path);
		return response.getBody();
	}

	private String newestId(JsonNode array, String what, String parent) {
		if (array == null || !array.isArray() || array.size() == 0)
			throw new AssertionError("No " + what + " was created for " + parent
					+ ". The request returned success, but nothing exists.");

		// The query has no order. ObjectIds sort by creation, so the greatest is the newest.
		String newest = "";
		for (JsonNode element : array)
			if (element.path("id").asText().compareTo(newest) > 0)
				newest = element.path("id").asText();
		return newest;
	}

	private void require(ResponseEntity<?> response, HttpStatus expected, String what) {
		if (response.getStatusCode() != expected)
			throw new AssertionError("Could not " + what + ": expected " + expected
					+ " but got " + response.getStatusCode()
					+ (response.getBody() == null ? "" : "\nBody: " + response.getBody()));
	}
}

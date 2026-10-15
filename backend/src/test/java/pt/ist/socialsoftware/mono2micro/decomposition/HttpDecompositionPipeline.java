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
	public void createSoftwareSystem(String softwareSystemName) {
		ResponseEntity<String> response = rest.postForEntity(
				base + "/softwareSystems/create?softwareSystemName={name}", null, String.class, softwareSystemName);

		require(response, HttpStatus.CREATED, "create software system " + softwareSystemName);
	}

	@Override
	public void addRepresentations(String softwareSystemName, StrategyCase strategyCase) {
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
				base + "/softwareSystems/{name}/addRepresentations/{group}",
				HttpMethod.POST,
				new HttpEntity<>(form, headers),
				String.class,
				softwareSystemName,
				strategyCase.representationGroup());

		require(response, HttpStatus.CREATED, "add representations to " + softwareSystemName);
	}

	@Override
	public String createStrategy(String softwareSystemName, StrategyCase strategyCase) {
		// strategyTypes is a repeated query parameter, so a multi-type strategy such as
		// Repository sends it twice rather than as one comma-joined value.
		StringBuilder url = new StringBuilder(
				base + "/softwareSystems/{name}/createStrategy?algorithmType={algorithm}");
		List<Object> uriVariables = new ArrayList<>();
		uriVariables.add(softwareSystemName);
		uriVariables.add("SciPy Clustering");

		for (String strategyType : strategyCase.strategyTypes()) {
			url.append("&strategyTypes={type").append(uriVariables.size()).append("}");
			uriVariables.add(strategyType);
		}

		ResponseEntity<String> response = rest.postForEntity(
				url.toString(), null, String.class, uriVariables.toArray());

		require(response, HttpStatus.CREATED, "create strategy for " + softwareSystemName);

		return strategyCase.strategyName(softwareSystemName);
	}

	@Override
	public String createSimilarity(String strategyName, StrategyCase strategyCase) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", strategyCase.similarityType());
		body.put("strategyName", strategyName);
		body.put("linkageType", "average");
		body.putAll(strategyCase.extraSimilarityFields());

		// Class and Entity Vectorization take no weights: their DTOs have no such field, and
		// sending one would be silently dropped rather than rejected.
		if (!strategyCase.weights().isEmpty())
			body.put("weightsList", strategyCase.weights());

		ResponseEntity<String> response = rest.exchange(
				base + "/similarity/create",
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				String.class);

		require(response, HttpStatus.CREATED, "create similarity for " + strategyName);

		// A duplicate request also returns 201 without creating anything, so the name is read
		// back rather than assumed.
		JsonNode similarities = getJson("/strategy/{name}/getStrategySimilarities", strategyName);
		return onlyName(similarities, "similarity", strategyName);
	}

	@Override
	public String createDecomposition(String similarityName) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "SciPy Clustering");
		body.put("similarityName", similarityName);
		body.put("cutType", "N");
		body.put("cutValue", 3);

		ResponseEntity<String> response = rest.exchange(
				base + "/similarity/createDecomposition",
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				String.class);

		// 200, not 201: DecompositionController returns OK here while every other create
		// endpoint in the pipeline returns CREATED.
		require(response, HttpStatus.OK, "create decomposition for " + similarityName);

		JsonNode decompositions = getJson("/similarity/{name}/decompositions", similarityName);
		return onlyName(decompositions, "decomposition", similarityName);
	}

	@Override
	public Map<String, List<String>> getClusters(String decompositionName) {
		return clusterNames(getJson("/decomposition/{name}/getClusters", decompositionName));
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
	Map<String, Short> entityIds(String decompositionName) {
		Map<String, Short> ids = new TreeMap<>();
		for (JsonNode cluster : getJson("/decomposition/{name}/getClusters", decompositionName))
			for (JsonNode element : cluster.path("elements")) {
				Short previous = ids.put(element.path("name").asText(), (short) element.path("id").asInt());
				if (previous != null)
					throw new AssertionError("Entity " + element.path("name").asText() + " appears more than once in " + decompositionName);
			}
		return ids;
	}

	// Manual editing. Each body is the one ClusterView.js sends. The entity lists of split and
	// transfer are comma-joined id strings, while form cluster takes cluster name → id array.

	void merge(String decompositionName, String cluster1, String cluster2, String newName) {
		Map<String, Object> body = operation("MergeOperation");
		body.put("cluster1Name", cluster1);
		body.put("cluster2Name", cluster2);
		body.put("newName", newName);
		edit(decompositionName, "merge", body);
	}

	void split(String decompositionName, String originalCluster, String newCluster, List<Short> entityIds) {
		Map<String, Object> body = operation("SplitOperation");
		body.put("originalCluster", originalCluster);
		body.put("newCluster", newCluster);
		body.put("entities", joined(entityIds));
		edit(decompositionName, "split", body);
	}

	void transfer(String decompositionName, String fromCluster, String toCluster, List<Short> entityIds) {
		Map<String, Object> body = operation("TransferOperation");
		body.put("fromCluster", fromCluster);
		body.put("toCluster", toCluster);
		body.put("entities", joined(entityIds));
		edit(decompositionName, "transfer", body);
	}

	void rename(String decompositionName, String clusterName, String newClusterName) {
		Map<String, Object> body = operation("RenameOperation");
		body.put("clusterName", clusterName);
		body.put("newClusterName", newClusterName);
		edit(decompositionName, "rename", body);
	}

	void formCluster(String decompositionName, String newCluster, Map<String, List<Short>> entityIdsByCluster) {
		Map<String, Object> body = operation("FormClusterOperation");
		body.put("newCluster", newCluster);
		body.put("entities", entityIdsByCluster);
		edit(decompositionName, "formCluster", body);
	}

	/** Returns the status, since an undo with nothing to undo is itself characterized. */
	HttpStatus undo(String decompositionName) {
		return rest.getForEntity(base + "/history/{name}/undoOperation", String.class, decompositionName).getStatusCode();
	}

	/** Returns the status, since a redo with nothing to redo is itself characterized. */
	HttpStatus redo(String decompositionName) {
		return rest.getForEntity(base + "/history/{name}/redoOperation", String.class, decompositionName).getStatusCode();
	}

	/** {@code {"undo": bool, "redo": bool}}. */
	JsonNode canUndoRedo(String decompositionName) {
		return getJson("/history/{name}/canUndoRedo", decompositionName);
	}

	// Quality assessment.

	/** The decomposition as stored: metrics are those of its last calculation. */
	JsonNode getDecomposition(String decompositionName) {
		return getJson("/decomposition/{name}", decompositionName);
	}

	/** The decomposition after recalculating its metrics if an edit made them outdated. */
	JsonNode updateDecomposition(String decompositionName) {
		return getJson("/decomposition/{name}/update", decompositionName);
	}

	/** Functionality metrics, keyed by functionality name under {@code functionalities}. */
	JsonNode getFunctionalities(String decompositionName) {
		return getJson("/accesses/{name}/getFunctionalitiesAndFunctionalitiesClusters", decompositionName);
	}

	/** MoJo and Purity of the second decomposition against the first, under {@code analysisList}. */
	JsonNode compare(String decomposition1Name, String decomposition2Name) {
		ResponseEntity<JsonNode> response = rest.postForEntity(
				base + "/comparison/{one}/{two}", null, JsonNode.class, decomposition1Name, decomposition2Name);
		require(response, HttpStatus.OK, "compare " + decomposition1Name + " with " + decomposition2Name);
		return response.getBody();
	}

	// Expert import.

	/**
	 * Uploads an expert clustering for a similarity, and returns the decomposition it creates.
	 *
	 * <p>The file is {@code {"clusters": {name: [entity id, ...]}}}.
	 */
	String createExpertDecomposition(String similarityName, String expertName, byte[] expertFile) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("expertName", expertName);
		form.add("expertFile", filePart(expertFile, expertName + ".json"));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity<String> response = rest.exchange(
				base + "/similarity/{name}/createExpertDecomposition",
				HttpMethod.POST,
				new HttpEntity<>(form, headers),
				String.class,
				similarityName);

		require(response, HttpStatus.OK, "import expert clustering " + expertName + " for " + similarityName);

		for (JsonNode decomposition : getJson("/similarity/{name}/decompositions", similarityName))
			if (decomposition.path("expert").asBoolean() && decomposition.path("name").asText().endsWith(" " + expertName))
				return decomposition.path("name").asText();

		throw new AssertionError("The expert import of " + expertName + " returned success, but no expert decomposition exists for "
				+ similarityName + ".");
	}

	// Export to Context Mapper.

	/** The raw response: the status is the export gate, and the body is a zip when it opens. */
	ResponseEntity<byte[]> export(String decompositionName) {
		return rest.getForEntity(base + "/decomposition/{name}/export", byte[].class, decompositionName);
	}

	/**
	 * Asks the functionality refactoring tool to refactor every functionality of a
	 * decomposition, which is what the export reads alongside the Structure representation.
	 *
	 * <p>The tool answers at once and keeps working in the background, so the export is not
	 * ready until it finishes. The body is what the frontend's {@code refactorSoftwareSystem} sends.
	 */
	void refactorFunctionalities(String refactorToolAddress, String decompositionName) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("decomposition_name", decompositionName);
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

		require(response, HttpStatus.OK, "refactor the functionalities of " + decompositionName);
	}

	@Override
	public int representationCount(String softwareSystemName) {
		return getJson("/softwareSystems/{name}/getRepresentations", softwareSystemName).size();
	}

	@Override
	public void deleteSoftwareSystem(String softwareSystemName) {
		rest.delete(base + "/softwareSystems/{name}/delete", softwareSystemName);
	}

	private Map<String, Object> operation(String type) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", type);
		return body;
	}

	private void edit(String decompositionName, String endpoint, Map<String, Object> body) {
		ResponseEntity<String> response = rest.exchange(
				base + "/decomposition/{name}/" + endpoint,
				HttpMethod.POST,
				new HttpEntity<>(body, jsonHeaders()),
				String.class,
				decompositionName);

		require(response, HttpStatus.OK, endpoint + " on " + decompositionName + " with " + body);
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

	private String onlyName(JsonNode array, String what, String parent) {
		if (array == null || !array.isArray() || array.size() == 0)
			throw new AssertionError("No " + what + " was created for " + parent
					+ ". The request returned success, but nothing exists — see the silent-duplicate"
					+ " paths in SimilarityService and StrategyService.");

		return array.get(array.size() - 1).path("name").asText();
	}

	private void require(ResponseEntity<?> response, HttpStatus expected, String what) {
		if (response.getStatusCode() != expected)
			throw new AssertionError("Could not " + what + ": expected " + expected
					+ " but got " + response.getStatusCode()
					+ (response.getBody() == null ? "" : "\nBody: " + response.getBody()));
	}
}

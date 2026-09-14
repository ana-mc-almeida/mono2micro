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
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.element.DomainEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Drives the pipeline over the REST API, exactly as the frontend does.
 *
 * <p>This is the regression net. It is pinned only to HTTP paths, JSON shapes and type
 * strings, so an internal refactor of the backend should leave it untouched. It is also the
 * only implementation that exercises the polymorphic {@code @JsonSubTypes} dispatch on the
 * {@code type} discriminator — the fan-out that adding a new similarity variant has to get
 * right in five registries at once.
 */
class HttpDecompositionPipeline implements DecompositionPipeline {

    private final TestRestTemplate rest;
    private final String base;

    HttpDecompositionPipeline(TestRestTemplate rest, int port) {
        this.rest = rest;
        this.base = "http://localhost:" + port + "/mono2micro";
    }

    @Override
    public String layer() {
        return "http";
    }

    @Override
    public void createCodebase(String codebaseName) {
        ResponseEntity<String> response = rest.postForEntity(
                base + "/codebase/create?codebaseName={name}", null, String.class, codebaseName);

        require(response, HttpStatus.CREATED, "create codebase " + codebaseName);
    }

    @Override
    public void addAccessesRepresentations(String codebaseName, byte[] idToEntity, byte[] accesses) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();

        // Index-aligned repeated parts: representationTypes[i] describes representations[i].
        form.add("representationTypes", "IDToEntity");
        form.add("representations", filePart(idToEntity, "idToEntity.json"));
        form.add("representationTypes", "Accesses");
        form.add("representations", filePart(accesses, "accesses.json"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = rest.exchange(
                base + "/codebase/{name}/addRepresentations/{group}",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class,
                codebaseName,
                "Accesses Based");

        require(response, HttpStatus.CREATED, "add representations to " + codebaseName);
    }

    @Override
    public String createStrategy(String codebaseName) {
        ResponseEntity<String> response = rest.postForEntity(
                base + "/codebase/{name}/createStrategy?algorithmType={algorithm}&strategyTypes={types}",
                null, String.class, codebaseName, "SciPy Clustering", "Accesses");

        require(response, HttpStatus.CREATED, "create strategy for " + codebaseName);

        // Deterministic: Strategy's constructor builds "<codebase> - <initials> Strategy",
        // and the initials of the single strategy type "Accesses" are "A".
        return codebaseName + " - A Strategy";
    }

    @Override
    public String createSimilarity(String strategyName) {
        Map<String, Object> weights = new LinkedHashMap<>();
        weights.put("type", "ACCESSES_WEIGHTS");
        weights.put("accessMetricWeight", 25);
        weights.put("writeMetricWeight", 25);
        weights.put("readMetricWeight", 25);
        weights.put("sequenceMetricWeight", 25);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "SIMILARITY_SCIPY_ACCESSES_REPOSITORY");
        body.put("strategyName", strategyName);
        body.put("profile", "Generic");          // the only profile AccessesRepresentation creates
        body.put("linkageType", "average");
        body.put("tracesMaxLimit", 0);
        body.put("traceType", "ALL");
        body.put("weightsList", new Object[]{weights});

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
    public Map<String, Cluster> getClusters(String decompositionName) {
        JsonNode body = getJson("/decomposition/{name}/getClusters", decompositionName);

        // Deserialized by hand: Cluster is abstract and its Jackson subtype info is not set up
        // for reading, so only the two fields the assertion needs are read.
        Map<String, Cluster> clusters = new HashMap<>();
        body.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            Partition partition = new Partition();
            partition.setName(value.path("name").asText(entry.getKey()));

            value.path("elements").forEach(element -> partition.addElement(
                    new DomainEntity((short) element.path("id").asInt(), element.path("name").asText())));

            clusters.put(entry.getKey(), partition);
        });
        return clusters;
    }

    @Override
    public int representationCount(String codebaseName) {
        return getJson("/codebase/{name}/getRepresentations", codebaseName).size();
    }

    @Override
    public void deleteCodebase(String codebaseName) {
        rest.delete(base + "/codebase/{name}/delete", codebaseName);
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

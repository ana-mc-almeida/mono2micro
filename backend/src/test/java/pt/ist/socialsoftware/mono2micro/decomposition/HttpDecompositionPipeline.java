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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    public void createCodebase(String codebaseName) {
        ResponseEntity<String> response = rest.postForEntity(
                base + "/codebase/create?codebaseName={name}", null, String.class, codebaseName);

        require(response, HttpStatus.CREATED, "create codebase " + codebaseName);
    }

    @Override
    public void addRepresentations(String codebaseName, StrategyCase strategyCase) {
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
                base + "/codebase/{name}/addRepresentations/{group}",
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class,
                codebaseName,
                strategyCase.representationGroup());

        require(response, HttpStatus.CREATED, "add representations to " + codebaseName);
    }

    @Override
    public String createStrategy(String codebaseName, StrategyCase strategyCase) {
        // strategyTypes is a repeated query parameter, so a multi-type strategy such as
        // Repository sends it twice rather than as one comma-joined value.
        StringBuilder url = new StringBuilder(
                base + "/codebase/{name}/createStrategy?algorithmType={algorithm}");
        List<Object> uriVariables = new ArrayList<>();
        uriVariables.add(codebaseName);
        uriVariables.add("SciPy Clustering");

        for (String strategyType : strategyCase.strategyTypes()) {
            url.append("&strategyTypes={type").append(uriVariables.size()).append("}");
            uriVariables.add(strategyType);
        }

        ResponseEntity<String> response = rest.postForEntity(
                url.toString(), null, String.class, uriVariables.toArray());

        require(response, HttpStatus.CREATED, "create strategy for " + codebaseName);

        return strategyCase.strategyName(codebaseName);
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

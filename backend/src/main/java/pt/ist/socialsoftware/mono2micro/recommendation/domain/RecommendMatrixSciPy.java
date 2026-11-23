package pt.ist.socialsoftware.mono2micro.recommendation.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.fileManager.ContextManager;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.JsonSubtypeConfiguration;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendMatrixSciPyDto;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDto;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.EditingChainService;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms.SciPyOperation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.*;
import pt.ist.socialsoftware.mono2micro.similarity.domain.dendrogram.Dendrogram;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.WeightsFactory;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.Constants;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_SCIPY;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.utils.Constants.SCRIPTS_ADDRESS;

@Document("recommendation")
public class RecommendMatrixSciPy extends Recommendation {
	private static final int MIN_CLUSTERS = 3, CLUSTER_STEP = 1;
	/**
	 * Reads a candidate's request as the controller reads the frontend's: with the self-registered subtypes, ignoring the settings a type lacks.
	 * Its own mapper, because a domain object cannot be handed Spring's.
	 */
	private static final ObjectMapper JSON = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.registerModule(JsonSubtypeConfiguration.subtypesModule());

	private int tracesMaxLimit;
	private Constants.TraceType traceType;
	private String linkageType;
	private List<Weights> weightsList;
	private Set<String> similarityMatricesNames;

	public RecommendMatrixSciPy() {}

	public RecommendMatrixSciPy(RecommendMatrixSciPyDto dto) {
		this.tracesMaxLimit = dto.getTracesMaxLimit();
		this.traceType = dto.getTraceType();
		this.linkageType = dto.getLinkageType();
		this.isCompleted = dto.isCompleted();
		this.weightsList = dto.getWeightsList();
		this.similarityMatricesNames = new HashSet<>();
	}

	public String getType() {
		return RECOMMEND_MATRIX_SCIPY;
	}

	public boolean requiresClusterTypeConversion() {
		return SimilarityKind.getByName(getKindName()).requiresClusterTypeConversion();
	}

	public int getTracesMaxLimit() {
		return tracesMaxLimit;
	}

	public void setTracesMaxLimit(int tracesMaxLimit) {
		this.tracesMaxLimit = tracesMaxLimit;
	}

	public Constants.TraceType getTraceType() {
		return traceType;
	}

	public void setTraceType(Constants.TraceType traceType) {
		this.traceType = traceType;
	}

	public String getLinkageType() {
		return linkageType;
	}

	public void setLinkageType(String linkageType) {
		this.linkageType = linkageType;
	}

	public Set<String> getSimilarityMatricesNames() {
		return similarityMatricesNames;
	}

	public void setSimilarityMatricesNames(Set<String> similarityMatricesNames) {
		this.similarityMatricesNames = similarityMatricesNames;
	}

	public List<Weights> getWeightsList() {
		return weightsList;
	}

	public void setWeightsList(List<Weights> weightsList) {
		this.weightsList = weightsList;
	}

	public List<String> getWeightsNames() {
		List<String> weightsNames = new ArrayList<>();
		for (Weights weights : getWeightsList())
			weightsNames.addAll(weights.getWeightsNames());
		return weightsNames;
	}

	@Override
	public void deleteProperties() {
		GridFsService gridFsService = ContextManager.get().getBean(GridFsService.class);
		gridFsService.deleteFiles(getSimilarityMatricesNames());
	}

	@Override
	public boolean equalsDto(RecommendationDto dto) {
		if (!(dto instanceof RecommendMatrixSciPyDto))
			return false;

		RecommendMatrixSciPyDto recommendMatrixSciPyDto = (RecommendMatrixSciPyDto) dto;

		return Objects.equals(recommendMatrixSciPyDto.getKindName(), getKindName()) &&
				Objects.equals(recommendMatrixSciPyDto.getProfileId(), getProfileId()) &&
				(recommendMatrixSciPyDto.getLinkageType().equals(this.linkageType)) &&
				(recommendMatrixSciPyDto.getTraceType() == this.traceType) &&
				(recommendMatrixSciPyDto.getTracesMaxLimit() == this.tracesMaxLimit);
	}

	public Set<Short> fillElements(GridFsService gridFsService) throws IOException {
		IDToEntityRepresentation idToEntityRepresentation = (IDToEntityRepresentation) Representation.ofSoftwareSystem(getSoftwareSystem(),
				ID_TO_ENTITY);
		Map<Short, String> idToEntity = new ObjectMapper().readValue(gridFsService.getFileAsString(idToEntityRepresentation.getId()),
				new TypeReference<Map<Short, String>>() {});
		return new TreeSet<>(idToEntity.keySet());
	}

	private int getTotalNumberOfWeights() {
		return getWeightsList().stream().reduce(0, (totalNumberOfWeights, weights) -> totalNumberOfWeights + weights.getNumberOfWeights(), Integer::sum);
	}

	private List<String> getAllWeightsTypes() {
		return getWeightsList().stream().map(Weights::getType).collect(Collectors.toList());
	}

	public Map<Short, String> getIDToEntityName(GridFsService gridFsService) throws IOException {
		IDToEntityRepresentation idToEntity = (IDToEntityRepresentation) Representation.ofSoftwareSystem(getSoftwareSystem(), ID_TO_ENTITY);
		return new ObjectMapper().readValue(gridFsService.getFileAsString(idToEntity.getId()), new TypeReference<Map<Short, String>>() {});
	}

	@Override
	public void generateRecommendation(RecommendationRepository recommendationRepository) {
		// Reads the scripts' address on the request's thread: the fork below has seen it unset
		System.out.println("Preparing to contact " + SCRIPTS_ADDRESS);
		// Executes the request in a fork to avoid blocking the user
		ForkJoinPool.commonPool().submit(() -> {
			try {
				GridFsService gridFsService = ContextManager.get().getBean(GridFsService.class);
				// One Similarity of this recommendation's own settings computes every candidate's matrix, named after this recommendation
				SimilarityScipy matrices = (SimilarityScipy) candidateRequest(getWeightsList()).newSimilarity(getRepresentations());
				matrices.setId(getId());
				matrices.setProfile(getProfile());
				setSimilarityMatricesNames(matrices.generateMultipleMatrices(gridFsService, this, fillElements(gridFsService), getTotalNumberOfWeights()));
				generateCandidates(gridFsService);

				recommendationRepository.save(this);
				System.out.println("recommendation ended");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Cuts every matrix into 3 up to the maximum number of clusters, through {@link SciPyOperation}, and writes each candidate's weights, size and
	 * metrics to the result file. A candidate is measured with an unsaved Similarity of its own weights, whose id is its matrix's name, and neither
	 * is kept: saving a candidate is {@link #createDecompositions}.
	 */
	private void generateCandidates(GridFsService gridFsService) throws Exception {
		JSONArray recommendationJSON = new JSONArray();

		List<String> similarityMatricesNames = new ArrayList<>(getSimilarityMatricesNames());

		Map<Short, String> idToEntity = getIDToEntityName(gridFsService); // read once: every candidate shares it
		int maxClusters = getMaxClusters(idToEntity.size());
		int numberOfTotalSteps = similarityMatricesNames.size() * (1 + maxClusters - MIN_CLUSTERS) / CLUSTER_STEP;
		System.out.println("Number of decompositions to be made: " + numberOfTotalSteps);

		if (requiresClusterTypeConversion()) {
			similarityMatricesNames.parallelStream().forEach(similarityMatrixName -> {
				int numberOfClusters = MIN_CLUSTERS;
				int numberOfEntitiesClusters = MIN_CLUSTERS;

				do {
					try {
						JSONObject decompositionJSON = measureCandidate(similarityMatrixName, numberOfClusters, idToEntity);
						numberOfEntitiesClusters = decompositionJSON.getInt("numberOfClusters");

						addRecommendationToJSON(recommendationJSON, decompositionJSON);

						System.out.println("Decomposition " + recommendationJSON.length() + "/" + numberOfEntitiesClusters);

						numberOfClusters++;

						// Every 20 candidates, updates the recommendation results file
						if (recommendationJSON.length() % 20 == 0)
							setRecommendationResult(gridFsService, recommendationJSON);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} while (numberOfEntitiesClusters <= maxClusters);
			});
		} else {
			similarityMatricesNames.parallelStream().forEach(similarityMatrixName -> {

				for (int numberOfClusters = MIN_CLUSTERS; numberOfClusters <= maxClusters; numberOfClusters += CLUSTER_STEP) {
					try {
						addRecommendationToJSON(recommendationJSON, measureCandidate(similarityMatrixName, numberOfClusters, idToEntity));

						System.out.println("Decomposition " + recommendationJSON.length() + "/" + numberOfTotalSteps);

						// Every 20 candidates, updates the recommendation results file
						if (recommendationJSON.length() % 20 == 0)
							setRecommendationResult(gridFsService, recommendationJSON);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}

		setRecommendationResult(gridFsService, recommendationJSON);
		setCompleted(true);
	}

	/** Cuts the matrix into {@code numberOfClusters} and describes the candidate: its name, weights, number of clusters, size and metrics. */
	private JSONObject measureCandidate(String similarityMatrixName, int numberOfClusters, Map<Short, String> idToEntity) throws Exception {
		String[] weights = (similarityMatrixName + "," + numberOfClusters).split(",");
		SimilarityScipy similarity = (SimilarityScipy) candidateRequest(weightsOf(weights)).newSimilarity(getRepresentations());
		similarity.setId(similarityMatrixName);
		similarity.setProfile(getProfile());
		similarity.useIDToEntityName(idToEntity);

		ClusterRepresentation candidate = new SciPyOperation("N", numberOfClusters).apply(similarity);
		candidate.setName(similarityMatrixName + "," + numberOfClusters);
		candidate.setup();
		candidate.calculateMetrics();

		JSONObject decompositionJSON = new JSONObject();
		decompositionJSON.put("name", candidate.getName());
		int i = 1;
		for (String weightName : getWeightsNames())
			decompositionJSON.put(weightName, weights[i++]);
		decompositionJSON.put("numberOfClusters", weights[i]);
		decompositionJSON.put("maxClusterSize", candidate.maxClusterSize());
		for (Map.Entry<String, Object> metric : candidate.getMetrics().entrySet())
			decompositionJSON.put(metric.getKey(), metric.getValue());
		return decompositionJSON;
	}

	// This function was created to be sure that no writes are made at the same time
	private synchronized void setRecommendationResult(GridFsService gridFsService, JSONArray recommendationResult) {
		gridFsService.replaceFile(new ByteArrayInputStream(recommendationResult.toString().getBytes()), getRecommendationResultName());
	}

	private synchronized void addRecommendationToJSON(JSONArray arrayJSON, JSONObject decompositionJSON) {
		arrayJSON.put(decompositionJSON);
	}

	private static int getMaxClusters(int totalNumberOfEntities) {
		if (totalNumberOfEntities >= 20)
			return 10;
		else if (totalNumberOfEntities >= 10)
			return 5;
		else if (totalNumberOfEntities >= 4)
			return 3;
		else throw new RuntimeException("Number of entities is too small (less than 4)");
	}

	/**
	 * Saves the chosen candidates, each as a {@link SciPyOperation} on the Similarity of its weights and the operation's output (D-016). The
	 * Similarity is the equal one among {@code similarities}, or a new one that reuses the candidate's matrix.
	 */
	@Override
	public void createDecompositions(List<Similarity> similarities, List<String> decompositionNames, EditingChainService editingChainService)
			throws Exception {
		GridFsService gridFsService = ContextManager.get().getBean(GridFsService.class);

		for (String name : decompositionNames) {
			String[] properties = name.split(",");

			if (properties.length < 2)
				continue;

			System.out.println("Creating decomposition with name: " + name);

			SimilarityDto similarityInformation = getSimilarityInformationDto(weightsOf(properties));

			// Find or create the candidate's Similarity
			SimilarityScipy similarity = (SimilarityScipy) similarities.stream()
					.filter(possibleSimilarity -> possibleSimilarity.equalsDto(similarityInformation)).findFirst().orElse(null);

			if (similarity == null) {
				similarity = (SimilarityScipy) similarityInformation.newSimilarity(getRepresentations());
				similarity.setProfile(getProfile());

				InputStream inputStream = gridFsService.getFile(name.substring(0, name.lastIndexOf(","))); // Gets the previously produced similarity matrix
				gridFsService.saveFile(inputStream, similarity.getId()); // And saves it under the new similarity's id

				// generate dendrogram image
				similarity.setDendrogram(new Dendrogram(similarity.getId(), similarity.getId(), similarity.getLinkageType()));
				similarities.add(similarity); // a later candidate with the same weights reuses it
			}

			editingChainService.run(new SciPyOperation("N", Float.parseFloat(properties[properties.length - 1])), similarity);
		}
	}

	/** The weights a candidate's name gives after its matrix's prefix, in this recommendation's weights order. */
	private List<Weights> weightsOf(String[] properties) {
		List<Weights> localWeightsList = WeightsFactory.getWeightsList(getAllWeightsTypes());
		int i = 1;
		for (Weights weights : localWeightsList) {
			float[] w = new float[weights.getNumberOfWeights()];
			for (int j = 0; j < w.length; j++)
				w[j] = Float.parseFloat(properties[i++]);
			weights.setWeightsFromArray(w);
		}
		return localWeightsList;
	}

	/**
	 * The request for the Similarity a saved candidate with {@code localWeightsList} is found or created with.
	 *
	 * @throws UnsupportedOperationException
	 * for a kind whose candidates cannot be saved: a known defect inherited from upstream (Gap 9, D-027), fixed in Phase 2
	 */
	public SimilarityDto getSimilarityInformationDto(List<Weights> localWeightsList) {
		if (!SimilarityKind.getByName(getKindName()).savesRecommendationCandidates())
			throw new UnsupportedOperationException("A " + getKindName() + " recommendation's candidates cannot be saved (Gap 9, D-027).");
		return candidateRequest(localWeightsList);
	}

	/**
	 * The request for a Similarity of this recommendation's kind, representations, profile and settings, weighed by {@code weightsList}. It is the
	 * JSON the frontend would send, read through the self-registered request types, so the type that computes the kind picks the request's class
	 * and this recommendation names none.
	 */
	SimilarityDto candidateRequest(List<Weights> weightsList) {
		ObjectNode request = JSON.createObjectNode();
		request.put("type", SimilarityKind.getByName(getKindName()).similarityType());
		request.put("kindName", getKindName());
		request.put("profileId", getProfileId());
		request.put("linkageType", linkageType);
		request.put("traceType", traceType == null ? null : traceType.name());
		request.put("tracesMaxLimit", tracesMaxLimit);
		request.set("weightsList", JSON.valueToTree(weightsList));
		try {
			SimilarityDto candidate = JSON.treeToValue(request, SimilarityDto.class);
			candidate.setSoftwareSystemId(getSoftwareSystem().getId());
			candidate.setRepresentationIds(getRepresentationIds());
			candidate.setProfileName(Profile.nameOf(getProfile())); // the generated name shows the profile
			return candidate;
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("A " + getKindName() + " recommendation cannot describe its candidates' Similarity.", e);
		}
	}
}

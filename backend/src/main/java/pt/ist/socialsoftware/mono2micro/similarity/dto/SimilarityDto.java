package pt.ist.socialsoftware.mono2micro.similarity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Its subclasses are JSON subtypes by their {@code @JsonTypeName}, found by the self-registration scan (D-019 point 8). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class SimilarityDto {
	String id;
	String softwareSystemId;
	// FIXME (ticket 09): the strategy fields go with Strategy
	String strategyId;
	String strategyName;
	String name;
	/** The type of the Similarity class that computes it, which picks this DTO's subclass. */
	String type;
	/** The name of its Similarity kind, which fixes its criteria and the representations it reads. */
	String kindName;
	/** The ids of the representations it reads. Set by the backend, so a request never chooses them. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	List<String> representationIds;
	/** Null for no profile: the Similarity is built on every unit (D-017). */
	String profileId;
	String profileName;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getKindName() {
		return kindName;
	}

	public void setKindName(String kindName) {
		this.kindName = kindName;
	}

	public List<String> getRepresentationIds() {
		return representationIds;
	}

	public void setRepresentationIds(List<String> representationIds) {
		this.representationIds = representationIds;
	}

	/** The types of the weights the request sends, in order, which must be the ones its kind is computed with (D-013). */
	public List<String> weightsTypes() {
		return Collections.emptyList();
	}

	protected static List<String> typesOf(List<Weights> weightsList) {
		return weightsList == null ? Collections.emptyList() : weightsList.stream().map(Weights::getType).collect(Collectors.toList());
	}

	/** Copies where {@code similarity} comes from: its strategy, or its kind and representations. */
	protected void copySource(Similarity similarity) {
		if (similarity.getStrategy() != null) { // FIXME (ticket 09): the strategy branch goes with Strategy
			this.strategyId = similarity.getStrategy().getId();
			this.strategyName = similarity.getStrategy().getName();
		}
		this.kindName = similarity.getKindName();
		this.representationIds = similarity.getRepresentationIds();
	}

	/**
	 * Fills this request with the Similarity a recommendation's candidate with {@code weightsList} is saved with.
	 *
	 * @throws UnsupportedOperationException
	 * for a type whose candidates cannot be saved: a known defect inherited from upstream (Gap 9, D-027), fixed in Phase 2
	 */
	public void describeCandidate(RecommendMatrixSciPy recommendation, List<Weights> weightsList) {
		throw new UnsupportedOperationException("A " + recommendation.getKindName() + " recommendation's candidates cannot be saved (Gap 9, D-027).");
	}

	/** Copies where a recommendation's candidate comes from: the recommendation's kind and representations. */
	protected void copyCandidateSource(RecommendMatrixSciPy recommendation) {
		this.softwareSystemId = recommendation.getSoftwareSystem().getId();
		this.kindName = recommendation.getKindName();
		this.representationIds = recommendation.getRepresentationIds();
	}

	/** What a generated name starts with: the kind, or for a Similarity made through a strategy, the strategy's name. */
	protected String namePrefix() {
		return strategyName != null ? strategyName : kindName; // FIXME (ticket 09): the strategy name goes with Strategy
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	/** Not a setter, so that a request's JSON never binds to it. */
	protected void copyProfile(Profile profile) {
		this.profileId = Profile.idOf(profile);
		this.profileName = Profile.nameOf(profile);
	}

	/**
	 * The representation kind whose units a profile of this Similarity selects, or null when the kind takes no profile.
	 * Not a getter, so that it never appears in the JSON.
	 */
	// FIXME (ticket 09): only the strategy path reads it; the kind declares it, so it goes with Strategy
	public String scopedRepresentationType() {
		return null;
	}

	/** How the generated name shows the profile. */
	protected String profileLabel() {
		return profileName == null ? "No profile" : profileName;
	}

	public String getSoftwareSystemId() {
		return softwareSystemId;
	}

	public void setSoftwareSystemId(String softwareSystemId) {
		this.softwareSystemId = softwareSystemId;
	}
}

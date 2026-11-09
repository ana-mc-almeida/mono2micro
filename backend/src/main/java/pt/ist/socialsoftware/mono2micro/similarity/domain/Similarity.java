package pt.ist.socialsoftware.mono2micro.similarity.domain;

import org.bson.types.ObjectId;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A similarity between the elements of one software system. It holds its kind and the representations it reads (D-010, D-016), and is
 * unique on them, its profile, settings and weights.
 */
public abstract class Similarity {
	@Id
	protected String id = new ObjectId().toHexString();
	protected String name;
	/**
	 * The name of this Similarity's {@link SimilarityKind}, which fixes its criteria.
	 *
	 * <p>Possible future improvement: hold the {@link SimilarityKind} itself, stored as its name through a Mongo
	 * {@code @WritingConverter}/{@code @ReadingConverter} pair registered as {@code MongoCustomConversions}, so the field is typed and
	 * callers no longer look the kind up by name. Not done yet (Ana, 2026-11-08).
	 */
	private String kindName;
	/** The collected representations it reads, which are those its kind requires, all of one software system. Never changed after creation. */
	@DBRef
	private List<Representation> representations;
	/** The profile that scopes this Similarity, or null for none: then it is built on every unit (D-017). */
	@DBRef(lazy = true)
	private Profile profile;

	public Similarity() {}

	public Similarity(String kindName, List<Representation> representations, String name) {
		this.kindName = kindName;
		this.representations = representations;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public abstract String getType();

	/** The software system it belongs to: its representations' (D-016). */
	public SoftwareSystem getSoftwareSystem() {
		return representations.get(0).getSoftwareSystem();
	}

	public String getKindName() {
		return kindName;
	}

	public List<Representation> getRepresentations() {
		return representations;
	}

	public List<String> getRepresentationIds() {
		return representations == null ? null : representations.stream().map(Representation::getId).collect(Collectors.toList());
	}

	/** The information blocks of a decomposition clustered from this Similarity, as its kind declares them. */
	public List<RepresentationInformation> informationBlocks() {
		return SimilarityKind.getByName(kindName).informationBlocks();
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public String getProfileId() {
		return Profile.idOf(profile);
	}

	public abstract Clustering getClustering();

	public abstract Map<Short, String> getIDToEntityName() throws Exception;

	public abstract void generate() throws Exception;

	public abstract void removeProperties();

	/** Whether this Similarity is the one {@code dto} requests, so that the request reuses it. */
	public abstract boolean equalsDto(SimilarityDto dto);

	/** The part of {@link #equalsDto} every Similarity shares: the same kind and representations. */
	protected boolean sameSource(SimilarityDto dto) {
		return Objects.equals(dto.getKindName(), kindName) && Objects.equals(dto.getRepresentationIds(), getRepresentationIds());
	}
}

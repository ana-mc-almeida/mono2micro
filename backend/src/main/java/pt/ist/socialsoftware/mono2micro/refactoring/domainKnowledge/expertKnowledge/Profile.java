package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import java.util.List;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A named, user-chosen selection of a representation's units that scopes a Similarity: some of an Accesses
 * representation's functionalities, or some of a Structure representation's entities (see {@link Representation#readUnits}). It
 * is one kind of {@link ExpertKnowledge}, and is not representation data (D-017).
 *
 * <p>A profile never changes once made: a move saves a new version, and a profile that is no longer offered is
 * {@link #isRetired() retired} rather than edited.
 */
@Document("profile")
@JsonTypeName(Profile.PROFILE)
public class Profile implements ExpertKnowledge {
	public static final String PROFILE = "Profile";

	@Id
	private String id = new ObjectId().toHexString();
	private String name;
	private Set<String> units;
	@DBRef // eager: Representation is abstract, and a lazy proxy of it cannot be downcast (D-024)
	private Representation representation;
	private boolean retired;

	public Profile() {}

	/**
	 * A profile is offered only for a Similarity kind it can scope (D-017, D-025 point 4): the input must be one of the kinds that declare a
	 * {@link SimilarityKind#scopedRepresentationType()}. The alternatives come from those declarations, so a new kind joins by declaring one.
	 */
	@Override
	public List<Requirement> requirements() {
		return Collections.singletonList(Requires.anyOf(SelfRegistration.prototypes(SimilarityKind.class).values().stream()
				.filter(kind -> kind.scopedRepresentationType() != null)
				.map(kind -> Requires.of(kind.getClass()))
				.toArray(Requirement[]::new)));
	}

	public Profile(String name, Set<String> units, Representation representation) {
		this.name = name;
		this.units = new HashSet<>(units);
		this.representation = representation;
	}

	/**
	 * The units a Similarity scoped by the given profile is built on. A null profile means no profile: every unit of
	 * the representation (D-017).
	 */
	public static Set<String> unitsOf(Profile profile, Representation representation, byte[] representationFile) throws JSONException {
		return profile == null ? representation.readUnits(representationFile) : profile.getUnits();
	}

	/** The profile's id, or null for no profile. */
	public static String idOf(Profile profile) {
		return profile == null ? null : profile.getId();
	}

	/** The profile's name, or null for no profile. */
	public static String nameOf(Profile profile) {
		return profile == null ? null : profile.getName();
	}

	public boolean belongsTo(String softwareSystemId) {
		return representation.getSoftwareSystem().getId().equals(softwareSystemId);
	}

	/** A new version of this profile, under the same name, that selects the given units instead. */
	public Profile withUnits(Set<String> units) {
		return new Profile(name, units, representation);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Set<String> getUnits() {
		return Collections.unmodifiableSet(units);
	}

	public Representation getRepresentation() {
		return representation;
	}

	public boolean isRetired() {
		return retired;
	}

	/** No longer offered, but kept for the Similarities and recommendations that use it. */
	public void retire() {
		this.retired = true;
	}
}

package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.bson.types.ObjectId;
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
 * realizes Expert knowledge in part, and is not representation data (D-017).
 *
 * <p>A profile never changes once made: a move saves a new version, and a profile that is no longer offered is
 * {@link #isRetired() retired} rather than edited.
 */
@Document("profile")
public class Profile {
	@Id
	private String id = new ObjectId().toHexString();
	private String name;
	private Set<String> units;
	@DBRef(lazy = true)
	private Representation representation;
	private boolean retired;

	public Profile() {}

	public Profile(String name, Set<String> units, Representation representation) {
		this.name = name;
		this.units = new HashSet<>(units);
		this.representation = representation;
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
}

package pt.ist.socialsoftware.mono2micro.representation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.fileManager.ContextManager;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation.AUTHOR;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation.CODE_EMBEDDINGS;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation.COMMIT;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

/**
 * Represents an imported file during the creation of the software system
 * Inheritors might contain additional information relevant to the imported file
 */
public abstract class Representation {
	public static final String ACCESSES_TYPE = "Accesses Based";
	public static final String REPOSITORY_TYPE = "Repository Based";
	public static final String CODE_EMBEDDINGS_TYPE = "Code Embeddings Based";
	public static final String STRUCTURE_TYPE = "Structure Based";

	public static final Map<String, List<String>> representationGroupToRepresentations = Stream.of(
			new AbstractMap.SimpleImmutableEntry<>(ACCESSES_TYPE, new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ACCESSES))),
			new AbstractMap.SimpleImmutableEntry<>(REPOSITORY_TYPE, new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ACCESSES, AUTHOR, COMMIT))),
			new AbstractMap.SimpleImmutableEntry<>(CODE_EMBEDDINGS_TYPE, new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS))),
			new AbstractMap.SimpleImmutableEntry<>(STRUCTURE_TYPE, new ArrayList<>(Arrays.asList(ENTITY_TO_ID, ID_TO_ENTITY, ACCESSES, STRUCTURE)))
	).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));

	@Id
	protected String id = new ObjectId().toHexString();
	protected String name;

	@DBRef(lazy = true)
	protected SoftwareSystem softwareSystem;

	public abstract String init(SoftwareSystem softwareSystem, byte[] representationFile) throws Exception;

	@JsonIgnore
	public abstract String getType();

	/**
	 * Every unit in this representation's file that a profile can select (D-017), or null when no profile can scope this
	 * representation. Units are the representation's own elements, by name, such as an Accesses representation's
	 * functionalities or a Structure representation's entities.
	 *
	 * <p>A null here is not a Similarity's null profile, which means every unit. Units are not Granularity elements in
	 * general: Structure clusters the entities a profile selects, but Accesses clusters entities while its profile's
	 * functionalities only scope the traces the similarity matrix is built from.
	 */
	public Set<String> readUnits(byte[] representationFile) throws JSONException {
		return null;
	}

	/**
	 * The representation of the given kind that a software system collected, or null if it has none. A software system
	 * holds at most one per kind, and keeps no list of them: this is a repository query (D-016).
	 */
	public static Representation ofSoftwareSystem(SoftwareSystem softwareSystem, String type) {
		return ContextManager.get().getBean(RepresentationRepository.class)
				.findBySoftwareSystemIdAndType(softwareSystem.getId(), type)
				.orElse(null);
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

	public SoftwareSystem getSoftwareSystem() {
		return softwareSystem;
	}

	public void setSoftwareSystem(SoftwareSystem softwareSystem) {
		this.softwareSystem = softwareSystem;
	}
}

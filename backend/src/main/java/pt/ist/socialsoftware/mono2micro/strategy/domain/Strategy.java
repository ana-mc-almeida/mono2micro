package pt.ist.socialsoftware.mono2micro.strategy.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
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
import static pt.ist.socialsoftware.mono2micro.representation.domain.Representation.*;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

@Document("strategy")
public class Strategy {

	// A structural based decomposition strategy is not yet implemented.
	// Only the minimum functionality was added for two reasons:
	// 1. Allow a user to input structural data from the structure collector
	// 2. Allow a user to be able to export decompositions if this data is present (implicates having a strategy defined)

	// Feel free to modify the STRUCTURE_STRATEGY as needed.

	public static final String ACCESSES_STRATEGY = "Accesses";
	public static final String REPOSITORY_STRATEGY = "Repository";
	public static final String STRUCTURE_STRATEGY = "Structure";
	public static final String CLASS_VECTORIZATION_STRATEGY = "Class Vectorization";
	public static final String ENTITY_VECTORIZATION_STRATEGY = "Entity Vectorization";
	public static final String FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY = "Functionality Vectorization Call Graph";
	public static final String FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY = "Functionality Vectorization Sequence Accesses";

	// No longer read for availability: each Similarity kind declares its own requirements (similarity/kind/, D-019).
	// Kept only because the representation delete cascade still reads it, until Strategy goes (ticket 09).
	public static final Map<String, List<String>> strategiesToRepresentations = Stream.of(
			new AbstractMap.SimpleImmutableEntry<>(ACCESSES_STRATEGY, new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ACCESSES))),
			new AbstractMap.SimpleImmutableEntry<>(REPOSITORY_STRATEGY, new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ACCESSES, AUTHOR, COMMIT))),
			new AbstractMap.SimpleImmutableEntry<>(STRUCTURE_STRATEGY, new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ACCESSES, STRUCTURE))),
			new AbstractMap.SimpleImmutableEntry<>(CLASS_VECTORIZATION_STRATEGY,
					new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS))),
			new AbstractMap.SimpleImmutableEntry<>(ENTITY_VECTORIZATION_STRATEGY,
					new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS))),
			new AbstractMap.SimpleImmutableEntry<>(FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY,
					new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS))),
			new AbstractMap.SimpleImmutableEntry<>(FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY,
					new ArrayList<>(Arrays.asList(ID_TO_ENTITY, ENTITY_TO_ID, ACCESSES, CODE_EMBEDDINGS)))
	).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));

	public static final Map<String, List<String>> strategiesToRepresentationTypes = Stream.of(
			new AbstractMap.SimpleImmutableEntry<>(ACCESSES_STRATEGY, new ArrayList<>(Arrays.asList(ACCESSES_TYPE))),
			new AbstractMap.SimpleImmutableEntry<>(REPOSITORY_STRATEGY, new ArrayList<>(Arrays.asList(ACCESSES_TYPE, REPOSITORY_TYPE))),
			new AbstractMap.SimpleImmutableEntry<>(STRUCTURE_STRATEGY, new ArrayList<>(Arrays.asList(ACCESSES_TYPE, STRUCTURE_TYPE))),
			new AbstractMap.SimpleImmutableEntry<>(CLASS_VECTORIZATION_STRATEGY, new ArrayList<>(Arrays.asList(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE))),
			new AbstractMap.SimpleImmutableEntry<>(ENTITY_VECTORIZATION_STRATEGY, new ArrayList<>(Arrays.asList(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE))),
			new AbstractMap.SimpleImmutableEntry<>(FUNCTIONALITY_VECTORIZATION_CALLGRAPH_STRATEGY,
					new ArrayList<>(Arrays.asList(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE))),
			new AbstractMap.SimpleImmutableEntry<>(FUNCTIONALITY_VECTORIZATION_ACCESSES_STRATEGY,
					new ArrayList<>(Arrays.asList(ACCESSES_TYPE, CODE_EMBEDDINGS_TYPE)))
	).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));
	@Id
	private String id = new ObjectId().toHexString();
	private String name;
	private String algorithmType;
	@DBRef
	private SoftwareSystem softwareSystem;
	private List<String> strategyTypes;

	public Strategy() {}

	public Strategy(SoftwareSystem softwareSystem, String algorithmType, List<String> strategyTypes) {
		StringBuilder shortForm = new StringBuilder();
		for (String word : strategyTypes.get(0).split(" ")) {
			shortForm.append(word.charAt(0));
		}
		for (int i = 1; i < strategyTypes.size(); i++) {
			shortForm.append("+");
			for (String word : strategyTypes.get(i).split(" ")) {
				shortForm.append(word.charAt(0));
			}
		}
		this.name = softwareSystem.getName() + " - " + shortForm + " Strategy";
		this.algorithmType = algorithmType;
		this.strategyTypes = strategyTypes;
	}

	public String getAlgorithmType() {
		return algorithmType;
	}

	public void setAlgorithmType(String algorithmType) {
		this.algorithmType = algorithmType;
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

	public List<String> getStrategyTypes() {
		return strategyTypes;
	}

	public void setStrategyTypes(List<String> strategyTypes) {
		this.strategyTypes = strategyTypes;
	}
}
package pt.ist.socialsoftware.mono2micro.representation.domain;

import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;

import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation.AUTHOR;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation.CODE_EMBEDDINGS;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation.COMMIT;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.EntityToIDRepresentation.ENTITY_TO_ID;
import static pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

public class RepresentationFactory {
	public static Representation getRepresentation(String representationType) {
		switch (representationType) {
		case ACCESSES:
			return new AccessesRepresentation();
		case ID_TO_ENTITY:
			return new IDToEntityRepresentation();
		case AUTHOR:
			return new AuthorRepresentation();
		case COMMIT:
			return new CommitRepresentation();
		case ENTITY_TO_ID:
			return new EntityToIDRepresentation();
		case CODE_EMBEDDINGS:
			return new CodeEmbeddingsRepresentation();
		case STRUCTURE:
			return new StructureRepresentation();
		default:
			throw new RuntimeException("The type \"" + representationType + "\" is not a valid representation type.");
		}
	}
}

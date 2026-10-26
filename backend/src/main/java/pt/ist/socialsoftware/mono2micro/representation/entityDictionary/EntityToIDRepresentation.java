package pt.ist.socialsoftware.mono2micro.representation.entityDictionary;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

@Document("representation")
@JsonTypeName(EntityToIDRepresentation.ENTITY_TO_ID)
public class EntityToIDRepresentation extends Representation {

	public static final String ENTITY_TO_ID = "EntityToID";

	public EntityToIDRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return id;
	}

	@Override
	public String getType() {
		return ENTITY_TO_ID;
	}
}

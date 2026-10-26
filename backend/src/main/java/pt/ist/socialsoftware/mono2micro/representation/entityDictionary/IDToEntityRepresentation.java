package pt.ist.socialsoftware.mono2micro.representation.entityDictionary;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

@Document("representation")
@JsonTypeName(IDToEntityRepresentation.ID_TO_ENTITY)
public class IDToEntityRepresentation extends Representation {

	public static final String ID_TO_ENTITY = "IDToEntity";

	public IDToEntityRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return id;
	}

	@Override
	public String getType() {
		return ID_TO_ENTITY;
	}
}

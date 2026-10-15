package pt.ist.socialsoftware.mono2micro.representation.analytical.domain;

import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

@Document("representation")
public class AuthorRepresentation extends Representation {
	public static final String AUTHOR = "Changes Authorship";

	public AuthorRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return name;
	}

	@Override
	public String getType() {
		return AUTHOR;
	}
}

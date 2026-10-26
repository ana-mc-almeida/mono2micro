package pt.ist.socialsoftware.mono2micro.representation.analytical.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

@Document("representation")
@JsonTypeName(CommitRepresentation.COMMIT)
public class CommitRepresentation extends Representation {
	public static final String COMMIT = "File Changes";

	public CommitRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return id;
	}

	@Override
	public String getType() {
		return COMMIT;
	}
}

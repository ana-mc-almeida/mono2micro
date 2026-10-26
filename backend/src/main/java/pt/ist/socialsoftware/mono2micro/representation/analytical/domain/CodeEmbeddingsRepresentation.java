package pt.ist.socialsoftware.mono2micro.representation.analytical.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

@Document("representation")
@JsonTypeName(CodeEmbeddingsRepresentation.CODE_EMBEDDINGS)
public class CodeEmbeddingsRepresentation extends Representation {

	public static final String CODE_EMBEDDINGS = "Code Embeddings";

	public CodeEmbeddingsRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return id;
	}

	@Override
	public String getType() {
		return CODE_EMBEDDINGS;
	}
}

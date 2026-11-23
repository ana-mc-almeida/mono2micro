package pt.ist.socialsoftware.mono2micro.representation.analytical.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.Set;

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

	// FIXME (Phase 2, Gap 3): no Similarity kind built on code embeddings takes a profile (D-017), so this has no units.
	// The file does hold packages, classes and methods; whether a profile should select them is open (open-questions.md,
	// "Should a profile's units be typed by Granularity?").
	@Override
	public Set<String> readUnits(byte[] representationFile) {
		return null;
	}
}

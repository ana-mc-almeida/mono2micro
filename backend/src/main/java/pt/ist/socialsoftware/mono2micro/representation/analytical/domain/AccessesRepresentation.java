package pt.ist.socialsoftware.mono2micro.representation.analytical.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.*;

@Document("representation")
@JsonTypeName(AccessesRepresentation.ACCESSES)
public class AccessesRepresentation extends Representation {

	public static final String ACCESSES = "Accesses";

	public AccessesRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) throws Exception {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return id;
	}

	@Override
	public String getType() {
		return ACCESSES;
	}

	@Override
	public Set<String> readUnits(byte[] representationFile) throws JSONException {
		// getFunctionalitiesNamesFromRepresentationFile
		JSONObject representationFileJSON = new JSONObject(new String(representationFile));
		Set<String> functionalitiesNames = new HashSet<>();
		for (Iterator<String> it = representationFileJSON.keys(); it.hasNext();)
			functionalitiesNames.add(it.next());
		return functionalitiesNames;
	}
}
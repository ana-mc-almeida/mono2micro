package pt.ist.socialsoftware.mono2micro.representation.analytical.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;

import java.util.*;

@Document("representation")
@JsonTypeName(StructureRepresentation.STRUCTURE)
public class StructureRepresentation extends Representation {
	public static final String STRUCTURE = "Structure";

	public StructureRepresentation() {}

	@Override
	public String init(SoftwareSystem softwareSystem, byte[] representationFile) throws Exception {
		this.name = softwareSystem.getName() + " & " + getType();
		this.softwareSystem = softwareSystem;
		return id;
	}

	@Override
	public String getType() {
		return STRUCTURE;
	}

	@Override
	public Set<String> readUnits(byte[] representationFile) throws JSONException {
		// getEntitiesNamesFromRepresentationFile
		JSONObject representationFileJSON = new JSONObject(new String(representationFile));
		Set<String> entitiesNames = new HashSet<>();

		JSONArray entitiesArray = representationFileJSON.getJSONArray("entities");
		for (int i = 0; i < entitiesArray.length(); i++) {
			JSONObject entityObject = entitiesArray.getJSONObject(i);
			String entityName = entityObject.getString("name");
			entitiesNames.add(entityName);
		}

		return entitiesNames;
	}
}

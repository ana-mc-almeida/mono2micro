package pt.ist.socialsoftware.mono2micro.softwareSystem.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The software system under decomposition (D-016). It holds no list of its children:
 * representations and strategies each reference their software system, and are found by
 * repository query.
 */
@Document("softwareSystem")
public class SoftwareSystem {
	@Id
	private String id = new ObjectId().toHexString();
	private String name;

	public SoftwareSystem() {}

	public SoftwareSystem(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

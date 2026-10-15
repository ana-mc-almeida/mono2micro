package pt.ist.socialsoftware.mono2micro.representation.dto;

import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

public class RepresentationDto {
	private String name;

	private String type;

	private String softwareSystemId;

	protected RepresentationDto(Representation representation) {
		this.name = representation.getName();
		this.type = representation.getType();
		this.softwareSystemId = representation.getSoftwareSystem().getId();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSoftwareSystemId() {
		return softwareSystemId;
	}

	public void setSoftwareSystemId(String softwareSystemId) {
		this.softwareSystemId = softwareSystemId;
	}
}

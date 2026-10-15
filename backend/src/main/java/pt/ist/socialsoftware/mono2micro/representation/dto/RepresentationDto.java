package pt.ist.socialsoftware.mono2micro.representation.dto;

import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

public class RepresentationDto {
	private String name;

	private String type;

	private String softwareSystemName;

	protected RepresentationDto(Representation representation) {
		this.name = representation.getName();
		this.type = representation.getType();
		this.softwareSystemName = representation.getSoftwareSystem().getName();
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

	public String getSoftwareSystemName() {
		return softwareSystemName;
	}

	public void setSoftwareSystemName(String softwareSystemName) {
		this.softwareSystemName = softwareSystemName;
	}
}

package pt.ist.socialsoftware.mono2micro.representation.dto;

import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

public class RepresentationDto {
	private String id;

	private String name;

	private String type;

	private String softwareSystemId;

	protected RepresentationDto(Representation representation) {
		this.id = representation.getId();
		this.name = representation.getName();
		this.type = representation.getType();
		this.softwareSystemId = representation.getSoftwareSystem().getId();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

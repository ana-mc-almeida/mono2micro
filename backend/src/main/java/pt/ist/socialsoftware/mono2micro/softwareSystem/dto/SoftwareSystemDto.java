package pt.ist.socialsoftware.mono2micro.softwareSystem.dto;

import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDto;
import pt.ist.socialsoftware.mono2micro.representation.dto.RepresentationDtoFactory;

import java.util.List;

public class SoftwareSystemDto {
	private String id;

	private String name;

	private List<RepresentationDto> representations;

	public SoftwareSystemDto(SoftwareSystem softwareSystem) {
		this.id = softwareSystem.getId();
		this.name = softwareSystem.getName();
		this.representations = RepresentationDtoFactory.getFactory().getRepresentationDtos(softwareSystem.getRepresentations());
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

	public List<RepresentationDto> getRepresentations() {
		return representations;
	}

	public void setRepresentations(List<RepresentationDto> representations) {
		this.representations = representations;
	}
}

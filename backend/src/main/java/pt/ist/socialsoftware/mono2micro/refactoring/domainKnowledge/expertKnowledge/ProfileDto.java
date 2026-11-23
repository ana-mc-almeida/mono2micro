package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class ProfileDto {
	private String id;
	private String name;
	private List<String> units;

	public ProfileDto() {}

	public ProfileDto(Profile profile) {
		this.id = profile.getId();
		this.name = profile.getName();
		this.units = new ArrayList<>(new TreeSet<>(profile.getUnits()));
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<String> getUnits() {
		return units;
	}
}

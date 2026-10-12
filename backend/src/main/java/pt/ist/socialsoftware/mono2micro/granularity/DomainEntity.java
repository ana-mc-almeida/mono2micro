package pt.ist.socialsoftware.mono2micro.granularity;

import static pt.ist.socialsoftware.mono2micro.granularity.ElementType.DOMAIN_ENTITY;

public class DomainEntity extends Element {

	public DomainEntity() {}

	public DomainEntity(Short id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String getType() { return DOMAIN_ENTITY.toString(); }
}

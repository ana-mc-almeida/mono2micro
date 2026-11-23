package pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.deserializer.ReducedTraceElementDtoDeserializer;

@JsonDeserialize(using = ReducedTraceElementDtoDeserializer.class)
public abstract class ReducedTraceElementDto {
	protected int occurrences;

	public int getOccurrences() { return occurrences; }

	public void setOccurrences(int occurrences) { this.occurrences = occurrences; }
}

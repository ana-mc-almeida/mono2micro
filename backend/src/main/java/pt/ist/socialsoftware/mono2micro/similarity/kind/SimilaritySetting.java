package pt.ist.socialsoftware.mono2micro.similarity.kind;

/**
 * A setting a Similarity request can take besides its linkage type, its profile and its weights, which every kind's form shows (D-029). A
 * kind declares the ones it takes with {@link SimilarityKind#settings()}, and the form shows only those.
 */
public enum SimilaritySetting {
	/** How many traces of each functionality are read; 0 reads them all. */
	TRACES_MAX_LIMIT,
	/** Which of a functionality's traces are read: all, the longest, or the one with more different accesses. */
	TRACE_TYPE,
	/** How deep the call graph is followed from each functionality. */
	MAX_DEPTH
}

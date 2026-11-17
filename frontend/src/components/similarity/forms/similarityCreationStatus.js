import HttpStatus from "http-status-codes";

// What a Similarity form shows next to its button while, and after, the screen creates the Similarity.
export const UPLOADING = "Uploading...";

// The message for a refused or failed create. Only 422 says why: the software system does not offer the kind. The backend answers every
// other failure, a request that does not match the kind or a computation that failed, with 400 (SimilarityController.createSimilarity).
export function creationFailure(error) {
    const status = error.response !== undefined ? error.response.status : undefined;
    if (status === HttpStatus.UNPROCESSABLE_ENTITY)
        return "Upload failed. The software system does not offer this kind.";
    return "Upload failed.";
}

// What a form shows when the kind it renders could not be loaded from the backend.
export const KIND_NOT_LOADED = "Could not load this kind's settings.";

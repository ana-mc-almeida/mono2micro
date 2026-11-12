import HttpStatus from "http-status-codes";

// What a Similarity form shows next to its button while, and after, the screen creates the Similarity.
export const UPLOADING = "Uploading...";

// The message for a refused or failed create: 422 when the software system does not offer the kind, 400 when the request does not match the
// kind (SimilarityController.createSimilarity).
export function creationFailure(error) {
    const status = error.response !== undefined ? error.response.status : undefined;
    if (status === HttpStatus.UNPROCESSABLE_ENTITY)
        return "Upload failed. The software system does not offer this kind.";
    if (status === HttpStatus.BAD_REQUEST)
        return "Upload failed. The request does not match the kind.";
    return "Upload failed.";
}

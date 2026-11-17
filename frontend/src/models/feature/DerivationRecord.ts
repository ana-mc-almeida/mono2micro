// The backend's derivation record (D-013): every feature of a group, with whether it is available and, if not, the requirements it failed.
export interface FeatureAvailability {
    name: string;
    available: boolean;
    failedRequirements: string[];
}

export interface DerivationRecord {
    features: FeatureAvailability[];
}

// The names of the record's available features, as the backend's DerivationRecord.availableNames() gives them.
export function availableNames(record: DerivationRecord): string[] {
    return record.features.filter(feature => feature.available).map(feature => feature.name);
}

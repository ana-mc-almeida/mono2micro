import Representation from "../representation/Representation";
import {RepresentationFactory} from "../representation/RepresentationFactory";

export default class SoftwareSystem {
    name!: string;
    representations: Representation[] | null;

    public constructor(softwareSystem: any) {
        this.name = softwareSystem.name;
        this.representations = RepresentationFactory.getRepresentations(softwareSystem.representations);
    }
}

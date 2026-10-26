import Representation from "../representation/Representation";
import {RepresentationFactory} from "../representation/RepresentationFactory";

export default class SoftwareSystem {
    id!: string;
    name!: string;
    representations: Representation[] | null;

    public constructor(softwareSystem: any) {
        this.id = softwareSystem.id;
        this.name = softwareSystem.name;
        this.representations = RepresentationFactory.getRepresentations(softwareSystem.representations);
    }
}

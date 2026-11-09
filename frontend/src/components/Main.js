import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { Home } from './Home';
import { Decompositions } from './decomposition/Decompositions';
import { SoftwareSystems } from './softwareSystem/SoftwareSystems';
import { SoftwareSystemPage } from './softwareSystem/SoftwareSystemPage';
import { ComparisonTool } from './comparisonTool/ComparisonTool';
import { Profiles } from './profile/Profiles';
import {FunctionalityRefactorToolMenu} from "./view/accessesViews/FunctionalityRefactorToolMenu";
import {Recommendations} from "./recommendation/Recommendations";
import {RepositoryView} from "./view/repositoryView/RepositoryView";
import {AccessesViews} from "./view/accessesViews/AccessesViews";
import {StructureView} from "./view/structureView/StructureView";

export const Main = () => (
  <main>
    <Routes>
      <Route exact path='/' element={<Home/>}/>

      <Route exact path='/softwareSystems' element={<SoftwareSystems/>} />
      <Route exact path='/softwareSystems/:softwareSystemId' element={<SoftwareSystemPage/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/recommendation' element={<Recommendations/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/similarities/:similarityId/decomposition' element={<Decompositions/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/similarities/:similarityId/:decompositionId/accessesViews' element={<AccessesViews/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/similarities/:similarityId/:decompositionId/repositoryView' element={<RepositoryView/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/similarities/:similarityId/:decompositionId/structureView' element={<StructureView/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/similarities/:similarityId/:decompositionId/functionalityRefactor' element={<FunctionalityRefactorToolMenu/>} />

      <Route exact path='/softwareSystems/:softwareSystemId/:representationId/profiles' element={<Profiles/>} />

      <Route exact path='/comparisonTool' element={<ComparisonTool/>} />
    </Routes>
  </main>
)

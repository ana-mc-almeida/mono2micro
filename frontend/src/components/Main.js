import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { Home } from './Home';
import { Decompositions } from './decomposition/Decompositions';
import { SoftwareSystems } from './softwareSystem/SoftwareSystems';
import { Strategies } from './strategy/Strategies';
import { ComparisonTool } from './comparisonTool/ComparisonTool';
import { Profiles } from './profile/Profiles';
import {FunctionalityRefactorToolMenu} from "./view/accessesViews/FunctionalityRefactorToolMenu";
import {Similarities} from "./similarity/Similarities";
import {Recommendations} from "./recommendation/Recommendations";
import {RepositoryView} from "./view/repositoryView/RepositoryView";
import {AccessesViews} from "./view/accessesViews/AccessesViews";
import {StructureView} from "./view/structureView/StructureView";

export const Main = () => (
  <main>
    <Routes>
      <Route exact path='/' element={<Home/>}/>

      <Route exact path='/softwareSystems' element={<SoftwareSystems/>} />
      <Route exact path='/softwareSystems/:softwareSystemId' element={<Strategies/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/similarity' element={<Similarities/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/recommendation' element={<Recommendations/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/:similarityName/decomposition' element={<Decompositions/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/:similarityName/:decompositionName/accessesViews' element={<AccessesViews/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/:similarityName/:decompositionName/repositoryView' element={<RepositoryView/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/:similarityName/:decompositionName/structureView' element={<StructureView/>} />
      <Route exact path='/softwareSystems/:softwareSystemId/:strategyName/:similarityName/:decompositionName/functionalityRefactor' element={<FunctionalityRefactorToolMenu/>} />

      <Route exact path='/softwareSystems/:softwareSystemId/:representationId/profiles' element={<Profiles/>} />

      <Route exact path='/comparisonTool' element={<ComparisonTool/>} />
    </Routes>
  </main>
)

/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.grails.tests

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.plugins.grails.structure.GrailsApplication

/**
 * Collects the tests of a Grails 3+ artefact. Tests are matched by short name only:
 * they are not required to live in the artefact's package, and any of the
 * [GrailsTestUtils.TEST_SUFFIXES] is accepted, so that projects separating unit from
 * integration tests by suffix (`FooServiceSpec` vs `FooServiceIntegrationSpec`) are covered.
 */
fun getTestsForArtifact(application: GrailsApplication, artefact: PsiClass, result: MutableCollection<in PsiClass>): Unit {
  val shortName = StringUtil.getShortName(artefact.qualifiedName ?: return)
  val project = application.project

  // the application scope unites the "test" and "integrationTest" source sets, which the
  // Gradle import exposes as separate modules; the project test scope is the same fallback
  // GrailsUtils.isInGrailsTests uses when the source sets cannot be resolved
  val scope = application.getScope(false, true).uniteWith(GlobalSearchScopesCore.projectTestScope(project))
  val shortNamesCache = PsiShortNamesCache.getInstance(project)

  for (suffix in GrailsTestUtils.TEST_SUFFIXES) {
    result.addAll(shortNamesCache.getClassesByName(shortName + suffix, scope))
  }
}
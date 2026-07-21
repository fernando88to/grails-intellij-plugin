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

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.actions.ArtefactData
import org.jetbrains.plugins.grails.editor.DECORATED_ARTEFACT_TYPES
import org.jetbrains.plugins.grails.editor.GenerateTestsAction
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager
import org.jetbrains.plugins.grails.tests.GrailsTestUtils
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition

class GoToDomainAction : GrailsGoToArtefactActionBase(GrailsArtifact.DOMAIN) {
  override fun createGenerateActions(artefactData: ArtefactData): List<AnAction> = listOf(
    ActionManager.getInstance().getAction("Grails.DomainClass")
  )
}

class GoToServiceAction : GrailsGoToArtefactActionBase(GrailsArtifact.SERVICE) {
  override fun createGenerateActions(artefactData: ArtefactData): List<AnAction> = listOf(
    ActionManager.getInstance().getAction("Grails.Service")
  )
}

/**
 * Navigates to services following the `<Artefact>DataService` naming convention.
 * [GrailsArtifact.SERVICE] indexes `OrgaoJudiciarioDataService` under the artefact
 * name `orgaoJudiciarioData`, so the lookup only has to append [DATA_INFIX].
 */
class GoToDataServiceAction : GrailsGoToArtefactActionBase(GrailsArtifact.SERVICE) {

  @NlsSafe override fun getTitle(artefactData: ArtefactData): String =
    dataArtefactName(artefactData).capitalize() + GrailsArtifact.SERVICE.suffix

  override fun getNavigateTargets(artefactData: ArtefactData): MutableCollection<GrClassDefinition> =
    super.getNavigateTargets(withArtefactName(artefactData, dataArtefactName(artefactData)))

  // already inside a data service: its artefact name carries the infix
  private fun dataArtefactName(artefactData: ArtefactData): String = artefactData.artefactName.let {
    if (it.endsWith(DATA_INFIX)) it else it + DATA_INFIX
  }

  private fun withArtefactName(artefactData: ArtefactData, artefactName: String) = ArtefactData(
    artefactData.project, artefactData.module, artefactData.file, artefactData.packageName,
    artefactName, artefactData.application, artefactData.isView
  )

  private companion object {
    const val DATA_INFIX = "Data"
  }
}

class GoToControllerAction : GrailsGoToArtefactActionBase(GrailsArtifact.CONTROLLER) {

  @NlsSafe override fun getTitle(artefactData: ArtefactData): String =
      super.getTitle(artefactData) + (getActionName(artefactData)?.let { ":$it" } ?: "")

  override fun navigate(artefactData: ArtefactData, target: PsiClass): Unit = GrailsUtils.getControllerActions(
    artefactData.artefactName, artefactData.module
  )[getActionName(artefactData)]?.navigate(true) ?: super.navigate(artefactData, target)

  override fun createGenerateActions(artefactData: ArtefactData): List<AnAction> = listOf(
    ActionManager.getInstance().getAction("Grails.Controller"),
    GenerateControllerAction(),
    GenerateAsyncControllerAction()
  )

  private fun getActionName(artefactData: ArtefactData) = if (artefactData.isView) {
    artefactData.file.nameWithoutExtension.let { if (it.startsWith("_")) null else it }
  }
  else {
    null
  }
}

class GoToViewAction : GrailsToolbarVfileAction() {

  override fun isOpenSingle(): Boolean = false

  @ActionText override fun getTitle(artefactData: ArtefactData): String =
    GrailsBundle.message("action.text.go.to.views", artefactData.artefactName.replaceFirstChar { it.uppercaseChar() })

  override fun getNavigateTargets(artefactData: ArtefactData): Collection<VirtualFile> {
    // Views for an artefact may live in a different module than the artefact itself (multi-project
    // build): e.g. a service in an upstream project whose views are defined by a downstream app.
    // Search every Grails application belonging to the related-module set (the same set artefact
    // instance discovery uses) rather than only this artefact's own application.
    val related = GrailsArtifact.getRelatedModules(artefactData.module)
    val fileIndex = ProjectFileIndex.getInstance(artefactData.project)
    val result = LinkedHashSet<VirtualFile>()
    for (application in GrailsApplicationManager.getInstance(artefactData.project).applications) {
      if (fileIndex.getModuleForFile(application.appRoot) !in related) continue
      val viewDir = application.appRoot
        .findChild(GrailsUtils.VIEWS_DIRECTORY)
        ?.findChild(artefactData.artefactName) ?: continue
      viewDir.children?.filterTo(result) {
        val name = it.nameSequence
        name.endsWith(".gsp") || name.endsWith(".jsp") || name.endsWith(GsonConstants.FILE_SUFFIX)
      }
    }
    return result
  }


  override fun createGenerateActions(artefactData: ArtefactData): Collection<AnAction> = listOf(
    GenerateViewsAction()
  )
}

class GoToTestAction : GrailsToolbarVfileAction() {

  override fun isOpenSingle(): Boolean = false

  @ActionText override fun getTitle(artefactData: ArtefactData): String =
    GrailsBundle.message("action.text.go.to.tests", artefactData.artefactName.replaceFirstChar { it.uppercaseChar() })

  override fun getNavigateTargets(artefactData: ArtefactData): Collection<VirtualFile> {
    val result = mutableListOf<VirtualFile>()

    for (artefactType in DECORATED_ARTEFACT_TYPES) {
      val artefacts = artefactType.getInstances(
        artefactData.module, artefactData.packageName, artefactData.artefactName
      ).ifEmpty {
        // the artefacts of a name need not share a package, so retry without the filter
        artefactType.getInstances(artefactData.module, null, artefactData.artefactName)
      }
      for (artifact in artefacts) {
        for (testClass in GrailsTestUtils.getTestsForArtifact(artifact, true)) {
          ContainerUtil.addIfNotNull(result, testClass.containingFile.virtualFile)
        }
      }
    }

    return result.distinct()
  }

  override fun createGenerateActions(artefactData: ArtefactData): Collection<AnAction> = artefactData.artefactName.replaceFirstChar { it.uppercaseChar() }.let {
    listOf(
      GenerateTestsAction(false, artefactData.artefactName, GrailsArtifact.DOMAIN).apply {
        templatePresentation.text = GrailsBundle.message("action.text.generate.tests.unit", it)
      },
      GenerateTestsAction(true, artefactData.artefactName, GrailsArtifact.DOMAIN).apply {
        templatePresentation.text = GrailsBundle.message("action.text.generate.tests.integration", it)
      },
      GenerateTestsAction(false, artefactData.artefactName, GrailsArtifact.CONTROLLER).apply {
        templatePresentation.text = GrailsBundle.message("action.text.generate.controller.tests.unit", it)
      },
      GenerateTestsAction(true, artefactData.artefactName, GrailsArtifact.CONTROLLER).apply {
        templatePresentation.text = GrailsBundle.message("action.text.generate.controller.tests.integration", it)
      }
    ).apply {
      forEach { it.templatePresentation.icon = GroovyMvcIcons.Grails_test }
    }
  }
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.plugins.grails.actions.getArtefactData

/**
 * Keyboard entry point for the editor toolbar groups ([GrailsToolbarActionBase]).
 * The toolbar buttons are popup action groups, and the plugin descriptor does not
 * support keyboard shortcuts on groups, so each shortcut is a plain action that
 * mirrors the button's click behavior: navigate directly when there is a single
 * target, otherwise show the group's popup.
 */
abstract class GrailsToolbarShortcutAction(private val groupId: String) : AnAction(), DumbAware {

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = getArtefactData(e.dataContext) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val group = ActionManager.getInstance().getAction(groupId) as? GrailsToolbarActionBase ?: return
    val artefactData = getArtefactData(e.dataContext) ?: return
    if (group.isOpenSingle()) {
      group.createNavigateActions(artefactData).singleOrNull()?.let {
        it.actionPerformed(e)
        return
      }
    }
    JBPopupFactory.getInstance()
      .createActionGroupPopup(group.getTitle(artefactData), group, e.dataContext,
                              JBPopupFactory.ActionSelectionAid.NUMBERING, false)
      .showInBestPositionFor(e.dataContext)
  }
}

class GoToDomainShortcutAction : GrailsToolbarShortcutAction("grails.goto.domain")
class GoToControllerShortcutAction : GrailsToolbarShortcutAction("grails.goto.controller")
class GoToServiceShortcutAction : GrailsToolbarShortcutAction("grails.goto.service")
class GoToViewShortcutAction : GrailsToolbarShortcutAction("grails.goto.view")
class GoToTestShortcutAction : GrailsToolbarShortcutAction("grails.goto.test")

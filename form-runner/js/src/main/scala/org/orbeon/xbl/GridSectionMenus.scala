/**
  * Copyright (C) 2017 Orbeon, Inc.
  *
  * This program is free software you can redistribute it and/or modify it under the terms of the
  * GNU Lesser General Public License as published by the Free Software Foundation either version
  *  2.1 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  */
package org.orbeon.xbl

import enumeratum.EnumEntry.Hyphencase
import enumeratum._
import org.orbeon.jquery.Offset
import org.orbeon.oxf.util.CoreUtils.asUnit
import org.orbeon.xforms.facade.Utils
import org.orbeon.xforms.{$, DocumentAPI}
import org.scalajs.dom.raw.KeyboardEvent
import org.scalajs.dom.{document, html}
import org.scalajs.jquery.JQueryEventObject

import scala.scalajs.js

trait GridSectionMenus {

  def componentName: String

  // Keep pointing to menu so we can move it around as needed
  // Old comment: NOTE: When scripts are in the head, this returns undefined. Should be fixed!
  val globalMenuElem: js.UndefOr[html.Element] = $(s".fr-$componentName-dropdown-menu")(0)

  val ListenerSuffix = s".orbeon-$componentName"

  sealed trait Operation extends EnumEntry with Hyphencase
  object Operation extends Enum[Operation] {
    val values = findValues
    case object MoveUp      extends Operation
    case object MoveDown    extends Operation
    case object InsertAbove extends Operation
    case object InsertBelow extends Operation
    case object Remove      extends Operation
  }

  case class CurrentComponent(currentComponentId: String, currentIteration: Int)

  private var currentComponentOpt: Option[CurrentComponent] = None

  // Initialization
  globalMenuElem foreach { _ ⇒
    // Click on our own button moves and shows the menu
    $(document).on(s"click$ListenerSuffix",   s".fr-$componentName-dropdown-button", moveAndShowMenuHandler _)
    $(document).on(s"keydown$ListenerSuffix", s".fr-$componentName-dropdown-button", delegateKeyEventToBootstrapButtonHandler _)

    // Listeners for all menu actions
    Operation.values foreach { op ⇒
      $(document).on(s"click$ListenerSuffix", s".fr-$componentName-dropdown-menu .fr-${op.entryName}", actionFunction(op))
    }
  }

  def moveAndShowMenuHandler(e: JQueryEventObject): Unit = {

    moveMenu(e)

    // NOTE: Don"t use dropdown("toggle") as that registers a new handler further down the DOM!
    $(globalMenuElem).find(".dropdown-toggle").trigger("click")

    // Prevent "propagation". In fact, with jQuery, "delegated" handlers are handled first, and if a delegated
    // event calls stopPropagation(), then "directly-bound" handlers are not called. Yeah. So here, we prevent
    // propagation as Dropdown.toggle() does, which will prevent the catch-all handler for clearMenus() from
    // running.
    // NOTE: Updated not to use just `false` for clarity and compatibility with DOM event handlers. See:
    // https://stackoverflow.com/questions/1357118/event-preventdefault-vs-return-false
    e.preventDefault()
    e.stopPropagation()
  }

  // Move the menu just below the button
  // Both callers are in response to  events flowing through `.fr-$componentName-dropdown-button`.
  def moveMenu(e: JQueryEventObject): Unit = {

    val jTarget = $(e.target)

    val jButton   = jTarget.closest(s".fr-$componentName-dropdown-button")
    val button    = jButton(0)
    val jDropdown = jTarget.closest(".dropdown")

    val dropdownOffset = Offset(jDropdown)

    $(globalMenuElem).css("position", "absolute")
    Offset.offset($(globalMenuElem), Offset(dropdownOffset.left, dropdownOffset.top + jButton.outerHeight()))

    Operation.values foreach { op ⇒
      $(globalMenuElem).find(".dropdown-menu").children(s".fr-${op.entryName}").toggleClass(
        "disabled",
        ! $(iteration(e)).is(s".can-${op.entryName}")
      )
    }

    componentId(e).zip(findIterationsForElemWithId(button)) foreach {
      case (currentComponentId, currentIteration) ⇒ currentComponentOpt = Some(CurrentComponent(currentComponentId, currentIteration))
    }
  }

  // Handle `keydown` events that arrive on our button and delegate the to the Bootstrap menu button
  def delegateKeyEventToBootstrapButtonHandler(e: JQueryEventObject): Unit = {
    moveMenu(e)
    $(globalMenuElem).find(".dropdown-toggle").trigger(
      $.asInstanceOf[js.Dynamic].Event( // `Event` constructor is not present in the jQuery facade
        e.`type`,
        new js.Object {
          val charCode = e.asInstanceOf[KeyboardEvent].charCode

          // Putting these to be complete, but `charCode` above does the trick for the menu
          val keyCode  = e.asInstanceOf[KeyboardEvent].keyCode
          val which    = e.asInstanceOf[KeyboardEvent].asInstanceOf[js.Dynamic].which
          val ctrlKey  = e.asInstanceOf[KeyboardEvent].ctrlKey
          val shiftKey = e.asInstanceOf[KeyboardEvent].shiftKey
          val altKey   = e.asInstanceOf[KeyboardEvent].altKey
          val metaKey  = e.asInstanceOf[KeyboardEvent].metaKey
        }
      ).asInstanceOf[JQueryEventObject]
    )
  }

  def iteration(e: JQueryEventObject): js.UndefOr[html.Element] =
    $(e.target).closest(s".fr-$componentName-repeat-iteration")(0)

  def findIterationsForElemWithId(elemWithId: html.Element): Option[Int] =
    $(elemWithId).attr("id").toOption map Utils.getRepeatIndexes flatMap (_.lastOption) map (_.toInt)

  def componentElem(e: JQueryEventObject): js.UndefOr[html.Element] =
    $(e.target).closest(s".xbl-fr-$componentName")(0)

  def componentId(e: JQueryEventObject): Option[String] =
      $(componentElem(e)).attr("id").toOption

  def actionFunction(op: Operation): JQueryEventObject ⇒ js.Any = e ⇒ asUnit {
    currentComponentOpt foreach {
      case CurrentComponent(currentComponentId, currentIteration) ⇒
        DocumentAPI.dispatchEvent(
          targetId   = currentComponentId,
          eventName  = s"fr-${op.entryName}",
          properties = js.Dictionary("row" → currentIteration.toString)
        )
    }
    e.preventDefault()
  }
}
/**
 * Copyright (C) 2017 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.builder

import autowire._
import org.orbeon.builder.rpc.FormBuilderRpcApi
import org.orbeon.xbl.{Dragula, DragulaOptions}
import org.orbeon.xforms.$
import org.orbeon.xforms.rpc.RpcClient
import org.scalajs.dom.html.Element
import org.scalajs.dom.raw.KeyboardEvent
import org.scalajs.dom.{document, html}
import org.scalajs.jquery.JQueryEventObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

private object ControlDnD {

  locally {
    val CopyClass = "fb-dnd-copy"
    val MoveClass = "fb-dnd-move"

    var shiftPressed = false

    $(document).on(
      "keyup keydown",
      (event: JQueryEventObject) ⇒ {
        // Surprisingly, `event.shiftKey` can be `undefined`
        val keyboardEventDyn = event.asInstanceOf[js.Dynamic]
        val shiftKeyOpt      = keyboardEventDyn.shiftKey.asInstanceOf[js.UndefOr[Boolean]]
        shiftPressed         = shiftKeyOpt.getOrElse(false)
      }
    )

    val drake = Dragula(
      js.Array(),
      new DragulaOptions {

        // Create the mirror inside the first container, so the proper CSS applies to the mirror
        override val mirrorContainer: js.UndefOr[html.Element] =
          $(".fb-body .fr-grid-td").get(0).asInstanceOf[html.Element]

        def isContainer(el: html.Element): Boolean =
          el.classList.contains("fr-grid-td")

        def moves(el: html.Element, source: html.Element, handle: html.Element, sibling: html.Element): Boolean =
          handle.classList.contains("fb-control-handle")

        // Can only drop into an empty cell
        def accepts(el: html.Element, target: html.Element, source: html.Element, sibling: html.Element): Boolean =
          $(target)
            .find("> :not(.gu-mirror, .gu-transit, .fb-control-editor-left)")
            .length == 0

        def copy(el: html.Element, source: html.Element): Boolean = {
          val cursorClass = if (shiftPressed) CopyClass else MoveClass
          $(el).addClass(cursorClass)
          shiftPressed
        }

        def invalid(el: Element, handle: Element) = false
      }
    )

    drake.onDrop((el: html.Element, target: html.Element, source: html.Element, sibling: html.Element) ⇒ {
      // It seems Dragula calls `onDrop` even if the target doesn't accept a drop, but in that case `target` is `null`
      if (target ne null)
        RpcClient[FormBuilderRpcApi].controlDnD(el.id, target.id, $(el).hasClass(CopyClass)).call()
    })
  }
}

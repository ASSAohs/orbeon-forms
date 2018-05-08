/**
  * Copyright (C) 2018 Orbeon, Inc.
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the
  * GNU Lesser General Public License as published by the Free Software Foundation; either version
  *  2.1 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  */
package org.orbeon.fr

import org.orbeon.xforms.$
import org.scalajs.jquery.JQueryEventObject

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("ORBEON.fr.private.API")
@JSExportAll
object DataSafe {

  private val ListenerSuffix = ".orbeon-beforeunload"
  private val ListenerEvents = s"beforeunload$ListenerSuffix"

  def setDataStatus(safe: Boolean): Unit = {
    // 2018-05-07: Some browsers, including Firefox and Chrome, no longer use the message provided here.
    if (safe)
      $(global).off(
        ListenerEvents
      )
    else
      $(global).on(
        ListenerEvents,
        ((_: JQueryEventObject) ⇒ "You may lose some unsaved changes."): js.Function
      )
  }

}

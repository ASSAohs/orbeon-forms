/**
  * Copyright (C) 2019 Orbeon, Inc.
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
package org.orbeon.xforms

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.orbeon.xforms.facade.Properties
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("ORBEON.xforms.StateHandling")
object StateHandling {

  import Private._

  case class ClientState(
    uuid     : String,
    sequence : Int
  )

  import enumeratum._

  sealed abstract class StateResult extends EnumEntry

  object StateResult extends Enum[StateResult] {

    val values = findValues

    case class  Uuid(uuid: String)    extends StateResult
    case class  Restore(uuid: String) extends StateResult
    case object Reload                extends StateResult
  }

  def initializeState(formId: String, initialUuid: String): StateResult = {

    def setInitialState(uuid: String): Unit =
      StateHandling.updateClientState(
        formId,
        ClientState(
          uuid     = uuid,
          sequence = 1
        )
      )

    StateHandling.findClientState(formId) match {
      case None =>

        scribe.debug("no state found, setting initial state")

        val uuid = initialUuid
        setInitialState(uuid)
        StateResult.Uuid(uuid)

      case Some(_) if BrowserUtils.getNavigationType == BrowserUtils.NavigationType.Reload =>

        scribe.debug("state found upon reload, setting initial state")

        val uuid = initialUuid
        setInitialState(uuid)
        StateResult.Uuid(uuid)

      case Some(_) if Properties.revisitHandling.get() == "reload" =>

        scribe.debug("state found with `revisitHandling` set to `reload`, reloading page")

        StateHandling.clearClientState(formId)
        StateResult.Reload

      case Some(state) =>

        scribe.debug("state found, assuming back/forward/navigate, requesting all events")

        StateResult.Restore(state.uuid)
    }
  }

  @JSExport
  def getFormUuid(formId: String): String =
    getClientStateOrThrow(formId).uuid

  @JSExport
  def getSequence(formId: String): String =
    getClientStateOrThrow(formId).sequence.toString

  @JSExport
  def updateSequence(formId: String, newSequence: Int): Unit =
    updateClientState(
      formId      = formId,
      clientState = getClientStateOrThrow(formId).copy(sequence = newSequence)
    )

  def getClientStateOrThrow(formId: String): ClientState =
    findClientState(formId) getOrElse (throw new IllegalStateException(s"client state not found for form `$formId`"))

  def findClientState(formId: String): Option[ClientState] =
    findRawState flatMap (_.get(formId)) flatMap { serialized =>
      decode[ClientState](serialized) match {
        case Left(_)  =>
          scribe.debug(s"error parsing state for form `$formId` and value `$serialized`")
          None
        case Right(state) =>
          scribe.trace(s"found state for form `$formId` and value `$state`")
          Some(state)
      }
    }

  def updateClientState(formId: String, clientState: ClientState): Unit = {

    val dict       = findRawState getOrElse js.Dictionary[String]()
    val serialized = clientState.asJson.noSpaces

    scribe.debug(s"updating client state for form `$formId` with value `$serialized`")

    dict(formId) = serialized

    dom.window.history.replaceState(
      statedata = dict,
      title     = "",
      url       = null
    )
  }

  def clearClientState(formId: String): Unit =
    findRawState foreach { state =>
      dom.window.history.replaceState(
        statedata = state -= formId,
        title     = "",
        url       = null
      )
    }

  private object Private {

    // Assume the state is a `js.Dictionary[String]` mapping form ids to serialized state
    def findRawState: Option[Dictionary[String]] =
      Option(dom.window.history.state) map
        (_.asInstanceOf[js.Dictionary[String]])
  }
}

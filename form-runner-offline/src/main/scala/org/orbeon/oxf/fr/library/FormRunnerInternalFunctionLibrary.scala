package org.orbeon.oxf.fr.library

import org.orbeon.macros.XPathFunction
import org.orbeon.oxf.fr.FormRunner
import org.orbeon.oxf.util.CoreCrossPlatformSupport
import org.orbeon.oxf.util.StaticXPath.DocumentNodeInfoType
import org.orbeon.oxf.xml.OrbeonFunctionLibrary
import org.orbeon.saxon.om
import org.orbeon.xbl.DateSupportJava

import scala.jdk.CollectionConverters._


object FormRunnerInternalFunctionLibrary extends OrbeonFunctionLibrary {

  lazy val namespaces = List("java:org.orbeon.oxf.fr.FormRunner" -> "frf")

  @XPathFunction(name = "sendError")
  def sendError(status: Int): Unit =
    FormRunner.sendError(status)

  @XPathFunction(name = "isAutosaveSupported")
  def isAutosaveSupported(app: String, form: String): Boolean =
    FormRunner.isAutosaveSupported(app, form)

  @XPathFunction(name = "xpathAllAuthorizedOperations")
  def xpathAllAuthorizedOperations(
    permissionsElement : om.NodeInfo,
    dataUsername       : String,
    dataGroupname      : String
  ): Iterable[String] =
    FormRunner.xpathAllAuthorizedOperations(permissionsElement, dataUsername, dataGroupname)

  @XPathFunction(name = "xpathFormRunnerStringProperty")
  def xpathFormRunnerStringProperty(name: String): Option[String] =
    FormRunner.xpathFormRunnerStringProperty(name)

  @XPathFunction(name = "xpathOrbeonRolesFromCurrentRequest")
  def xpathOrbeonRolesFromCurrentRequest: Iterable[String] =
    FormRunner.orbeonRolesFromCurrentRequest

  @XPathFunction(name = "allAuthorizedOperationsAssumingOwnerGroupMember")
  def allAuthorizedOperationsAssumingOwnerGroupMember(permissionsElement: om.NodeInfo): Iterable[String] =
    FormRunner.allAuthorizedOperationsAssumingOwnerGroupMember(permissionsElement)

  @XPathFunction(name = "authorizedOperationsBasedOnRolesXPath")
  def authorizedOperationsBasedOnRolesXPath(permissionsEl: Option[om.NodeInfo]): List[String] =
    FormRunner.authorizedOperationsBasedOnRolesXPath(permissionsEl.orNull)

  @XPathFunction(name = "selectFormLang")
  def selectFormLang(app: String, form: String, requestedLang: String, formLangs: Iterable[String]): String =
    FormRunner.selectFormLang(app, form, requestedLang, formLangs.toList.asJava) // TODO: change `.toList.asJava` once JVM side uses Saxon 10

  @XPathFunction(name = "selectFormRunnerLang")
  def selectFormRunnerLang(app: String, form: String, requestedLang: String, formRunnerLangs: Iterable[String]): String =
    FormRunner.selectFormLang(app, form, requestedLang, formRunnerLangs.toList.asJava) // TODO: change `.toList.asJava` once JVM side uses Saxon 10

  @XPathFunction(name = "getFormLangSelection")
  def getFormLangSelection(app: String, form: String, formLanguages: Iterable[String]): List[String] =
    FormRunner.getFormLangSelection(app, form, formLanguages.toList.asJava) // TODO: change `.toList.asJava` once JVM side uses Saxon 10

}

object FormRunnerDateSupportFunctionLibrary extends OrbeonFunctionLibrary {

  lazy val namespaces = List("java:org.orbeon.xbl.DateSupportJava" -> "DateSupport")

  @XPathFunction(name = "serializeExternalValueJava")
  def serializeExternalValueJava(
    binding : om.Item,
    format  : String
  ): String =
    DateSupportJava.serializeExternalValueJava(binding, format)

  @XPathFunction(name = "deserializeExternalValueJava")
  def deserializeExternalValueJava(externalValue: String): String =
    DateSupportJava.deserializeExternalValueJava(externalValue)
}

object FormRunnerErrorSummaryFunctionLibrary extends OrbeonFunctionLibrary {

  lazy val namespaces = List("java:org.orbeon.xbl.ErrorSummary" -> "ErrorSummary")

  @XPathFunction(name = "updateForMovedIteration")
  def updateForMovedIteration(
    errorsInstanceDoc : DocumentNodeInfoType,
    absoluteTargetId  : String,
    fromIterations    : Iterable[Int],
    toIterations      : Iterable[Int]
  ): Unit =
    org.orbeon.xbl.ErrorSummary.updateForMovedIteration(
      errorsInstanceDoc,
      absoluteTargetId,
      fromIterations.toArray,
      toIterations.toArray
    )

 @XPathFunction(name = "removeUpdateOrInsertError")
  def removeUpdateOrInsertError(
   errorsInstanceDoc : DocumentNodeInfoType,
    stateInstanceDoc  : DocumentNodeInfoType
  ): Unit =
   org.orbeon.xbl.ErrorSummary.removeUpdateOrInsertError(
     errorsInstanceDoc,
     stateInstanceDoc
   )
}

object FormRunnerSecureUtilsFunctionLibrary extends OrbeonFunctionLibrary {

  lazy val namespaces = List("java:org.orbeon.oxf.util.SecureUtils" -> "secure")

  @XPathFunction(name = "randomHexId")
  def randomHexId: String =
    CoreCrossPlatformSupport.randomHexId

}
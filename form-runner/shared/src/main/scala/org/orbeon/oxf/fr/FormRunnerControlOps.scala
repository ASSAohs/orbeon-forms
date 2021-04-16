/**
 * Copyright (C) 2013 Orbeon, Inc.
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
package org.orbeon.oxf.fr

import cats.syntax.option._
import org.orbeon.dom.saxon.DocumentWrapper
import org.orbeon.dom.QName
import org.orbeon.oxf.fr.XMLNames._
import org.orbeon.oxf.fr.datamigration.MigrationSupport.{findMigrationForVersion, findMigrationOps}
import org.orbeon.oxf.fr.datamigration.PathElem
import org.orbeon.oxf.util.CollectionUtils._
import org.orbeon.oxf.util.CoreUtils._
import org.orbeon.oxf.util.StaticXPath.DocumentNodeInfoType
import org.orbeon.oxf.util.StringUtils._
import org.orbeon.oxf.util.Whitespace
import org.orbeon.oxf.xforms.NodeInfoFactory.namespaceInfo
import org.orbeon.oxf.xforms.action.XFormsAPI.insert
import org.orbeon.oxf.xforms.analysis.controls.LHHA
import org.orbeon.oxf.xforms.analysis.model.ModelDefs
import org.orbeon.oxf.xml.SaxonUtils.parseQName
import org.orbeon.oxf.xml.XMLConstants
import org.orbeon.saxon.om.{Item, NodeInfo}
import org.orbeon.scaxon.Implicits._
import org.orbeon.scaxon.SimplePath._
import org.orbeon.scaxon.XPath._
import org.orbeon.xforms.XFormsNames
import org.orbeon.xml.NamespaceMapping

import scala.collection.compat._


trait FormRunnerControlOps extends FormRunnerBaseOps {

  import Private._

  val TrueExpr : String = "true()"
  val FalseExpr: String = "false()"

  // Extensible records would be cool here. see:
  //
  // - https://github.com/lampepfl/dotty/issues/964
  // - https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#extensible-records
  //
  case class BindPath                       (                   bind: NodeInfo, path: List[PathElem]                         )
  case class BindPathHolders                (                   bind: NodeInfo, path: List[PathElem], holders: Option[List[NodeInfo]])
  case class ControlBindPathHoldersResources(control: NodeInfo, bind: NodeInfo, path: List[PathElem], holders: Option[List[NodeInfo]], resources: Seq[(String, NodeInfo)])

  def hasAllClassesPredicate(classNamesList: List[String])(control: NodeInfo): Boolean = {
    val controlClasses = control.attClasses
    classNamesList forall controlClasses.contains
  }

  def hasAnyClassPredicate(classNamesList: Set[String])(control: NodeInfo): Boolean = {
    val controlClasses = control.attClasses
    classNamesList exists controlClasses.contains
  }

  val LHHAInOrder = LHHA.values map (_.entryName) toList

  // Get the control name based on the control, bind, grid, section or template id
  //@XPathFunction
  def controlNameFromId(controlOrBindId: String): String =
    ControlOps.controlNameFromIdOpt(controlOrBindId).orNull

  //@XPathFunction
  def controlNameFromIdOpt(controlOrBindId: String) =
    ControlOps.controlNameFromIdOpt(controlOrBindId)

  // Whether the given id is for a control (given its reserved suffix)
  def isIdForControl(controlOrBindId: String): Boolean =
    ControlOps.controlNameFromIdOpt(controlOrBindId).isDefined

  // Whether the given node corresponds to a control
  // TODO: should be more restrictive
  val IsControl: NodeInfo => Boolean = hasName

  // Find a control by name (less efficient than searching by id)
  def findControlByName(inDoc: NodeInfo, controlName: String): Option[NodeInfo] = (
    for {
      suffix  <- PossibleControlSuffixes.iterator
      control <- findInViewTryIndex(inDoc, controlName + '-' + suffix).iterator
    } yield
      control
  ).nextOption()

  // Find a control id by name
  def findControlIdByName(inDoc: NodeInfo, controlName: String): Option[String] =
    findControlByName(inDoc, controlName) map (_.id)

  // Find a control element by name or null (the empty sequence)
  //@XPathFunction
  def findControlByNameOrEmpty(inDoc: NodeInfo, controlName: String): NodeInfo =
    findControlByName(inDoc, controlName).orNull

  // Get the control's name based on the control element
  def getControlName(control: NodeInfo): String = getControlNameOpt(control).get

  // Get the control's name based on the control element
  def getControlNameOpt(control: NodeInfo): Option[String] =
    control.idOpt flatMap controlNameFromIdOpt

  def hasName(control: NodeInfo): Boolean = getControlNameOpt(control).isDefined

  // Return a bind ref or nodeset attribute value if present
  def bindRefOpt(bind: NodeInfo): Option[String] =
    bind attValueOpt "ref"

  // Find a bind by name
  def findBindByName(inDoc: NodeInfo, name: String): Option[NodeInfo] =
    findInBindsTryIndex(inDoc, bindId(name))

  // Find a bind by name or null (the empty sequence)
  //@XPathFunction
  def findBindByNameOrEmpty(inDoc: NodeInfo, name: String): NodeInfo =
    findBindByName(inDoc, name).orNull

  // NOTE: Not sure why we search for anything but id or name, as a Form Runner bind *must* have an id and a name
  def isBindForName(bind: NodeInfo, name: String): Boolean =
    bind.hasIdValue(bindId(name)) || bindRefOpt(bind).contains(name) // also check ref/nodeset in case id is not present

  // Canonical way: use the `name` attribute
  def getBindNameOrEmpty(bind: NodeInfo): String =
    findBindName(bind).orNull

  def findBindName(bind: NodeInfo): Option[String] =
    bind attValueOpt "name"

  def findBindAndPathStatically(inDoc: NodeInfo, controlName: String): Option[BindPath] =
    findBindByName(inDoc, controlName) flatMap { bindNode =>
      buildBindPath(bindNode) map (BindPath(bindNode, _))
    }

  // Find data holders (there can be more than one with repeats)
  def findDataHolders(controlName: String)(implicit ctx: FormRunnerDocContext): List[NodeInfo] =
    findBindPathHoldersInDocument(ctx.formDefinitionRootElem, controlName, Some(ctx.dataRootElem)) flatMap (_.holders) getOrElse Nil

  // If `contextItemOpt` is `None`, don't search for holders.
  def findBindPathHoldersInDocument(
    inDoc          : NodeInfo,
    controlName    : String,
    contextItemOpt : Option[Item]
  ): Option[BindPathHolders] =
    findBindAndPathStatically(inDoc, controlName) map { case BindPath(bind, path) =>

      // Assume that namespaces in scope on leaf bind apply to ancestor binds (in theory mappings could be
      // overridden along the way!)
      val namespaces = NamespaceMapping(bind.namespaceMappings.toMap)

      // Evaluate path from instance root element
      // NOTE: Don't pass Reporter as not very useful and some tests don't have a containingDocument scoped.
      BindPathHolders(
        bind,
        path,
        contextItemOpt map { contextItem =>
          eval(
            item       = contextItem,
            expr       = path map (_.value) mkString "/",
            namespaces = namespaces
          ).asInstanceOf[Seq[NodeInfo]].to(List)
        }
      )
    }

  def hasHTMLMediatype(nodes: Seq[NodeInfo]): Boolean =
    nodes exists (element => (element attValue "mediatype") == "text/html")

  //@XPathFunction
  def isSingleSelectionControl(localName: String): Boolean =
    localName == "select1" || localName.endsWith("-select1")

  //@XPathFunction
  def isMultipleSelectionControl(localName: String): Boolean =
    localName == "select" || localName.endsWith("-select")

  def searchControlsInFormByClass(
    formDoc           : DocumentNodeInfoType,
    classes           : Set[String],
    dataFormatVersion : DataFormatVersion
  ): Seq[FormRunner.ControlBindPathHoldersResources] = {
    val headOpt = (formDoc / "*:html" / "*:head").headOption
    val bodyOpt = (formDoc / "*:html" / "*:body").headOption
    val controlBindPathHoldersResourcesList =
      bodyOpt.toList flatMap { body =>

        val topLevelOnly =
          FormRunner.searchControlsTopLevelOnly(
            body      = body,
            data      = None,
            predicate = FormRunner.hasAnyClassPredicate(classes)
          )

        val withSectionTemplatesOpt =
          headOpt map { head =>
            FormRunner.searchControlsUnderSectionTemplates(
              head      = head,
              body      = body,
              data      = None,
              predicate = FormRunner.hasAnyClassPredicate(classes)
            )
          }

        topLevelOnly ++ withSectionTemplatesOpt.toList.flatten
      }
    dataFormatVersion match {
      case DataFormatVersion.Edge => controlBindPathHoldersResourcesList
      case _                      =>
        val (_, migrationOpsToApply) =
          findMigrationOps(
            srcVersion = DataFormatVersion.Edge,
            dstVersion = dataFormatVersion
          )

        // Find the migration functions once and for all
        val pathMigrationFunctions =
          for {
            metadataElem <- FormRunner.metadataInstanceRootOpt(formDoc).toList
            ops          <- migrationOpsToApply
            json         <- findMigrationForVersion(metadataElem, ops.version)
          } yield
            ops.adjustPathTo40(ops.decodeMigrationSetFromJson(json), _)
        controlBindPathHoldersResourcesList map { controlBindPathHoldersResources =>
          val path = controlBindPathHoldersResources.path
          val adjustedBindPathElems =
            (pathMigrationFunctions.iterator flatMap (_.apply(path)) nextOption()) getOrElse path
          controlBindPathHoldersResources.copy(path = adjustedBindPathElems)
        }
    }
  }

  def searchControlsTopLevelOnly(
    body      : NodeInfo,
    data      : Option[NodeInfo],
    predicate : NodeInfo => Boolean
  ): Seq[ControlBindPathHoldersResources] =
    searchControlBindPathHoldersInDoc(
      controlElems   = body descendant * filter IsControl,
      inDoc          = body,
      contextItemOpt = data map (_.rootElement),
      predicate      = predicate
    )

  def searchControlsUnderSectionTemplates(
    head      : NodeInfo,
    body      : NodeInfo,
    data      : Option[NodeInfo],
    predicate : NodeInfo => Boolean
  ): Seq[ControlBindPathHoldersResources] =
    for {
      section         <- FormRunner.findSectionsWithTemplates(body)
      sectionName     <- getControlNameOpt(section).toList

      BindPathHolders(
        _,
        sectionPath,
        sectionHoldersOpt
      )              <- findBindPathHoldersInDocument(body, sectionName, data map (_.rootElement)).toList

      contextItemOpt <- sectionHoldersOpt match {
                         case None | Some(Nil) => List(None)
                         case Some(holders)    => holders map Some.apply
                       }

      xblBinding     <- xblBindingForSection(head, section).toList

      ControlBindPathHoldersResources(
        control,
        bind,
        path,
        holdersOpt,
        labels
      )              <- searchControlBindPathHoldersInDoc(
                         controlElems   = xblBinding.rootElement / XBLTemplateTest descendant * filter IsControl,
                         inDoc          = xblBinding,
                         contextItemOpt = contextItemOpt,
                         predicate      = predicate
                       )
    } yield
      ControlBindPathHoldersResources(control, bind, sectionPath ::: path, holdersOpt, labels)

  // NOTE: Return at most one `ControlBindPathHoldersResources` per incoming control element.
  def searchControlBindPathHoldersInDoc(
    controlElems   : Seq[NodeInfo],
    inDoc          : NodeInfo,
    contextItemOpt : Option[NodeInfo],
    predicate      : NodeInfo => Boolean
  ): Seq[ControlBindPathHoldersResources] =
    for {
      control                              <- controlElems
      if predicate(control)
      bindId                               <- control.attValueOpt(XFormsNames.BIND_QNAME).toList
      controlName                          <- controlNameFromIdOpt(bindId).toList
      BindPathHolders(bind, path, holders) <- findBindPathHoldersInDocument(inDoc, controlName, contextItemOpt).toList
      resourceHoldersWithLang              = FormRunnerResourcesOps.findResourceHoldersWithLangUseDoc(inDoc, controlName)
    } yield
      ControlBindPathHoldersResources(control, bind, path, holders, resourceHoldersWithLang)

  def xblBindingForSection(head: NodeInfo, section: NodeInfo): Option[DocumentWrapper] = {
    val mapping = FormRunner.sectionTemplateXBLBindingsByURIQualifiedName(head / XBLXBLTest)
    FormRunner.sectionTemplateBindingName(section) flatMap mapping.get
  }

  // Return None if no namespace mapping is required OR none can be created
  def valueNamespaceMappingScopeIfNeeded(
    bind       : NodeInfo,
    qNameValue : String)(implicit
    ctx        : FormRunnerDocContext
  ): Option[(String, String)] = {

    val (prefix, _) = parseQName(qNameValue)

    def existingNSMapping =
      bind.namespaceMappings.toMap.get(prefix) map (prefix ->)

    def newNSMapping = {
      // If there is no mapping and the schema prefix matches the prefix and a uri is found for the
      // schema, then insert a new mapping. We place it on the top-level bind so we don't have to insert
      // it repeatedly.
      val newURI =
        if (SchemaOps.findSchemaPrefix(bind).contains(prefix))
          SchemaOps.findSchemaNamespace(bind)
        else
          None

      newURI map { uri =>
        insert(into = ctx.topLevelBindElem.toList, origin = namespaceInfo(prefix, uri))
        prefix -> uri
      }
    }

    if (prefix == "")
      None
    else
      existingNSMapping orElse newNSMapping
  }

  def readDenormalizedCalculatedMip(
    bindElem    : NodeInfo,
    mip         : ModelDefs.ComputedMIP,
    mipAttQName : QName)(implicit // pass `mipAttQName` separately for Form Builder
    ctx         : FormRunnerDocContext
  ): String =
    denormalizeMipValue(
      mip          = mip,
      mipValue     = bindElem attValueOpt mipAttQName,
      hasCalculate = hasCalculate(bindElem),
      isTypeString = isTypeStringUpdateNsIfNeeded(bindElem, _)
    )

  // When *writing* a value to the form definition, return the attribute value if the value doesn't
  // match its default value, otherwise return `None`.
  //
  // This depends on context, as the default for `readonly` depends on whether there is a `calculate`.
  //
  def normalizeMipValue(
    mip          : ModelDefs.MIP,
    mipValue     : String,
    hasCalculate : => Boolean,
    isTypeString : String => Boolean
  ): Option[String] =
    mipValue.trimAllToOpt flatMap { trimmed =>

     // See also https://github.com/orbeon/orbeon-forms/issues/3950

      val isDefault =
        mip match {
          case ModelDefs.Relevant   => trimmed == TrueExpr
          case ModelDefs.Readonly   => trimmed == TrueExpr && hasCalculate || trimmed == FalseExpr && ! hasCalculate
          case ModelDefs.Required   => trimmed == FalseExpr
          case ModelDefs.Constraint => trimmed.isEmpty
          case ModelDefs.Calculate  => trimmed.isEmpty
          case ModelDefs.Default    => trimmed.isEmpty
          case ModelDefs.Type       => isTypeString(trimmed)
          case ModelDefs.Whitespace => trimmed == Whitespace.Policy.Preserve.entryName
        }

      ! isDefault option trimmed
    }

  def hasCalculate(bindElem: NodeInfo): Boolean =
    bindElem.attValueOpt(ModelDefs.Calculate.name).isDefined

  // NOTE: It's hard to remove the namespace mapping once it's there, as in theory lots of
  // expressions and types could use it. So for now the mapping is never garbage collected.
  def isTypeStringUpdateNsIfNeeded(
    bindElem : NodeInfo,
    value    : String)(implicit
    ctx      : FormRunnerDocContext
  ): Boolean =
    valueNamespaceMappingScopeIfNeeded(bindElem, value).isDefined &&
      Set(XMLConstants.XS_STRING_QNAME, XFormsNames.XFORMS_STRING_QNAME)(bindElem.resolveQName(value))

  private object Private {

    val FBLangPredicate         = "[@xml:lang = $fb-lang]"
    val PossibleControlSuffixes = List("control", "grid", "section", "repeat")

    // Find a bind by predicate
    def findBind(inDoc: NodeInfo, p: NodeInfo => Boolean): Option[NodeInfo] =
      findTopLevelBind(inDoc).toSeq descendant "*:bind" find p

    // 2017-04-25: Don't use enclosing parentheses anymore. This now ensures that the `ref` is a single
    // reference to an XML element. See https://github.com/orbeon/orbeon-forms/issues/3174.
    //
    // Also, specific case for Form Builder: drop language predicate, as we want to index/return values
    // for all languages. So far, this is handled as a special case, as this is not something that happens
    // in other forms.
    def buildBindPath(bind: NodeInfo): Option[List[PathElem]] =
      (bind ancestorOrSelf XFBindTest flatMap bindRefOpt).reverse match {
        case _ #:: tail =>
          tail.map(bindRef =>
            PathElem(
              if (bindRef.endsWith(FBLangPredicate))
                bindRef.dropRight(FBLangPredicate.length)
              else
                bindRef
            )
          ).toList.some
        case _ =>
          // https://github.com/orbeon/orbeon-forms/issues/4972
          None
      }

  // When *reading* a value from the form definition, return the denormalized or explicit value since the
  // user interface is not and should not be aware of defaults.
  def denormalizeMipValue(
    mip          : ModelDefs.ComputedMIP,
    mipValue     : Option[String],
    hasCalculate : => Boolean,
    isTypeString : String => Boolean
  ): String = {

    // Start by normalizing
    val normalizedValueOpt =
      mipValue flatMap (_.trimAllToOpt) flatMap { rawMipValue =>
        normalizeMipValue(
          mip,
          rawMipValue,
          hasCalculate,
          isTypeString
        )
      }

      normalizedValueOpt match {
        case Some(value) =>
          value
        case None =>
          mip match {
            case ModelDefs.Relevant   => TrueExpr
            case ModelDefs.Readonly   => if (hasCalculate) TrueExpr else FalseExpr
            case ModelDefs.Required   => FalseExpr
            case ModelDefs.Calculate  => ""
            case ModelDefs.Default    => ""
            case ModelDefs.Whitespace => Whitespace.Policy.Preserve.entryName
          }
      }
    }
  }
}

/**
 * Copyright (C) 2010 Orbeon, Inc.
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
package org.orbeon.oxf.xforms.function.xxforms

import org.orbeon.oxf.util.StringUtils._
import org.orbeon.oxf.xforms.InstanceData
import org.orbeon.oxf.xforms.function.XFormsFunction
import org.orbeon.saxon.expr._
import org.orbeon.saxon.om._


/**
 * xxf:invalid-binds()
 */
class XXFormsInvalidBinds extends XFormsFunction {// don't extend XFormsMIPFunction as addToPathMap returns something different

  override def iterate(xpathContext: XPathContext): SequenceIterator = {

    // First item or context node if any
    val item = argument.headOption map (e ⇒ Option(e.iterate(xpathContext).next())) getOrElse Option(xpathContext.getContextItem)

    item match {
      case Some(nodeInfo: NodeInfo) ⇒
        Option(InstanceData.getInvalidBindIds(nodeInfo)) match {
          case Some(invalidBindIdsString) ⇒
            asIterator(invalidBindIdsString.splitTo[Array]())
          case None ⇒
            // No invalid bind ids
            EmptyIterator.getInstance
        }

      case _ ⇒
        // Return () if we can't access the node
        EmptyIterator.getInstance
    }
  }

  // TODO: something smart
  override def addToPathMap(pathMap: PathMap, pathMapNodeSet: PathMap.PathMapNodeSet): PathMap.PathMapNodeSet =
    super.addToPathMap(pathMap, pathMapNodeSet)

  // Needed otherwise xpathContext.getContextItem doesn't return the correct value
  override def getIntrinsicDependencies =
    if (argument.isEmpty) StaticProperty.DEPENDS_ON_CONTEXT_ITEM else 0
}
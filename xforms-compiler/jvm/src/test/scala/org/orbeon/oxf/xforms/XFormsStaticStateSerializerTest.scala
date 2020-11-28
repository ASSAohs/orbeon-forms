package org.orbeon.oxf.xforms

import org.orbeon.dom
import org.orbeon.oxf.externalcontext.ExternalContext
import org.orbeon.oxf.pipeline.InitUtils
import org.orbeon.oxf.test.{PipelineSupport, ResourceManagerSupport}
import org.orbeon.oxf.xforms.analysis.PartAnalysisBuilder
import org.orbeon.oxf.xml.dom.Converter._
import org.scalatest.funspec.AnyFunSpecLike


class XFormsStaticStateSerializerTest
  extends ResourceManagerSupport
     with AnyFunSpecLike {

  ResourceManagerSupport

  describe("State serialization") {

    val form: dom.Document =
      <xh:html xmlns:xh="http://www.w3.org/1999/xhtml" xmlns:xf="http://www.w3.org/2002/xforms">
        <xh:head>
            <xh:title>XForms Hello</xh:title>
            <xf:model>
                <xf:instance>
                    <first-name xmlns=""/>
                </xf:instance>
            </xf:model>
        </xh:head>
        <xh:body>
            <xh:p>
                <xf:input ref="/*" incremental="true">
                    <xf:label>Please enter your first name:</xf:label>
                </xf:input>
            </xh:p>
            <xh:p>
                <xf:output value="if (normalize-space(string(.)) = '') then '' else concat('Hello, ', string(.), '!')"/>
            </xh:p>
        </xh:body>
    </xh:html>.toDocument

    it("must serialize without errors") {
      withTestExternalContext { _ =>


        val (template, staticState) = PartAnalysisBuilder.createFromDocument(form)

        val s = XFormsStaticStateSerializer.serialize(template, staticState)

        println(s"xxxx $s")
      }
    }
  }

  def withTestExternalContext[T](body: ExternalContext => T): T =
    InitUtils.withPipelineContext { pipelineContext =>
      body(
        PipelineSupport.setExternalContext(
          pipelineContext,
          PipelineSupport.DefaultRequestUrl
        )
      )
    }
}

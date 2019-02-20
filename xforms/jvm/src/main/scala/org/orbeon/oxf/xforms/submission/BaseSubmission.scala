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
package org.orbeon.oxf.xforms.submission

import java.io.IOException
import java.util.concurrent.Callable

import org.orbeon.io.UriScheme
import org.orbeon.oxf.common.OXFException
import org.orbeon.oxf.externalcontext.ExternalContext.Request
import org.orbeon.oxf.externalcontext.{ExternalContext, LocalRequest, LocalResponse, URLRewriter}
import org.orbeon.oxf.http.{EmptyInputStream, Headers, StreamedContent}
import org.orbeon.oxf.util.{Connection, ConnectionResult, IndentedLogger, NetUtils, PathUtils}
import org.orbeon.oxf.xforms.event.events.{ErrorType, XFormsSubmitErrorEvent}
import org.orbeon.oxf.xforms.{XFormsProperties, XFormsUtils}

trait SubmissionProcess {
  def process(request: ExternalContext.Request, response: ExternalContext.Response)
}

abstract class BaseSubmission(val submission: XFormsModelSubmission) extends Submission {

  import BaseSubmission._

  val containingDocument = submission.containingDocument

  protected def getAbsoluteSubmissionURL(
    resolvedActionOrResource : String,
    queryString              : String,
    isNorewrite              : Boolean,
    urlType                  : UrlType
  ): String = {

    // NOTE: For resolveServiceURL: If the resource or service URL does not start with a protocol or with '/', the
    // URL is resolved against  the request path, then against the service base. Example in servlet environment:
    //
    // - action path: my/service
    // - request URL: http://orbeon.com/orbeon/myapp/mypage
    // - request path: /myapp/mypage
    // - service base: http://services.com/myservices/
    // - resulting service URL: http://services.com/myservices/myapp/my/service

    val resolve =
      if (urlType == UrlType.Resource)
        XFormsUtils.resolveResourceURL _
      else
        XFormsUtils.resolveServiceURL _

    resolve(
      containingDocument,
      submission.getSubmissionElement,
      PathUtils.appendQueryString(resolvedActionOrResource, queryString),
      if (isNorewrite) URLRewriter.REWRITE_MODE_ABSOLUTE_NO_CONTEXT else URLRewriter.REWRITE_MODE_ABSOLUTE
    )
  }

  /**
   * Submit the Callable for synchronous or asynchronous execution.
   *
   * @return ConnectionResult or null if asynchronous
   */
  protected def submitCallable(
    p        : SubmissionParameters,
    p2       : SecondPassParameters,
    callable : Callable[SubmissionResult]
  ): SubmissionResult =
    if (p2.isAsynchronous) {
      // Tell XFCD that we have one more async submission
      containingDocument.getAsynchronousSubmissionManager(true).addAsynchronousSubmission(callable)
      // Tell caller he doesn't need to do anything
      null
    }  else if (p.isDeferredSubmissionSecondPass) {
      // Tell XFCD that we have a submission replace="all" ready for a second pass
      // Tell caller he doesn't need to do anything
      containingDocument.setReplaceAllCallable(callable)
      null
    }  else {
      // Just run it now
      callable.call
    }

  protected def getDetailsLogger(
    p  : SubmissionParameters,
    p2 : SecondPassParameters
  ) = submission.getDetailsLogger(p, p2)

  protected def getTimingLogger(
    p  : SubmissionParameters,
    p2 : SecondPassParameters
  ) = submission.getTimingLogger(p, p2)

  /**
   * Perform a local (request dispatcher or portlet) submission.
   */
  protected def openLocalConnection(
    incomingRequest        : Request,
    response               : ExternalContext.Response,
    indentedLogger         : IndentedLogger,
    resource               : String,
    p                      : SubmissionParameters,
    actualRequestMediatype : String,
    encoding               : String,
    messageBodyOrNull      : Array[Byte],
    queryString            : String,
    customHeaderNameValues : Map[String, List[String]],
    submissionProcess      : SubmissionProcess,
    isContextRelative      : Boolean,
    isDefaultContext       : Boolean
  ): ConnectionResult = {

    require(response ne null) // see comment at call site

    // Action must be an absolute path
    if (! resource.startsWith("/"))
      throw new OXFException("Action does not start with a '/': " + resource)

    val httpMethod = p.httpMethod

    // handle case of empty body
    val messageBody = Option(messageBodyOrNull)

    // Destination context path is the context path of the current request, or the context path implied by the new URI
    val destinationContextPath =
      if (isDefaultContext)
        ""
      else if (isContextRelative)
        incomingRequest.getContextPath
      else
        NetUtils.getFirstPathElement(resource)

    // Determine headers
    val requestHeadersCapitalized =
      Connection.buildConnectionHeadersCapitalizedWithSOAPIfNeeded(
        scheme            = UriScheme.Http,
        method            = httpMethod,
        hasCredentials    = false,
        mediatype         = actualRequestMediatype,
        encodingForSOAP   = encoding,
        customHeaders     = customHeaderNameValues,
        headersToForward  = Connection.headersToForwardFromProperty,
        getHeader         = containingDocument.headersGetter)(
        logger            = indentedLogger
      )

    val effectiveResourceURI =  {
      val updatedActionStringBuilder = new StringBuilder(resource)
      if (queryString ne null) {
        if (resource.indexOf('?') == -1)
          updatedActionStringBuilder.append('?')
        else
          updatedActionStringBuilder.append('&')
        updatedActionStringBuilder.append(queryString)
      }
      updatedActionStringBuilder.toString
    }

    val rootAdjustedResourceURI =
      if (isDefaultContext || isContextRelative)
        effectiveResourceURI
      else
        NetUtils.removeFirstPathElement(effectiveResourceURI)

    if (rootAdjustedResourceURI eq null)
      throw new OXFException("Action must start with a servlet context path: " + resource)

    val content =
      messageBody map { bytes ⇒
        if (Connection.requiresRequestBody(httpMethod) && indentedLogger.isDebugEnabled && isLogBody)
          Connection.logRequestBody(actualRequestMediatype, bytes)(indentedLogger)

        StreamedContent.fromBytes(bytes, Headers.firstHeaderIgnoreCase(requestHeadersCapitalized, Headers.ContentType))
      }

    val localRequest =
      new LocalRequest(
        incomingRequest         = incomingRequest,
        contextPath             = destinationContextPath,
        pathQuery               = rootAdjustedResourceURI,
        method                  = httpMethod,
        headersMaybeCapitalized = requestHeadersCapitalized,
        content                 = content
      )

    if (indentedLogger.isDebugEnabled)
      indentedLogger.logDebug(
        "",
        "dispatching request",
        "method", httpMethod.entryName,
        "mediatype", actualRequestMediatype,
        "context path", destinationContextPath,
        "effective resource URI (original)", effectiveResourceURI,
        "effective resource URI (relative to servlet root)", rootAdjustedResourceURI
      )

    if (p.replaceType == ReplaceType.All) {
      val replaceAllResponse = new AllReplacer.ReplaceAllResponse(response)
      submissionProcess.process(localRequest, replaceAllResponse)

      val dummyContent = new StreamedContent(
        EmptyInputStream,
        None,
        None,
        None
      ) {
        override def close(): Unit = {
          super.close()
          // Try to obtain, flush and close the stream to work around WebSphere issue
          try {
            val os = response.getOutputStream
            os.flush()
            os.close()
          } catch {
            case e: IllegalStateException ⇒
              indentedLogger.logDebug("", "IllegalStateException caught while closing OutputStream after forward")
              try {
                val writer = response.getWriter
                writer.flush()
                writer.close()
              }  catch {
                case f: IllegalStateException ⇒
                  indentedLogger.logDebug("", "IllegalStateException caught while closing Writer after forward")
                case f: IOException ⇒
                  indentedLogger.logDebug("", "IOException caught while closing Writer after forward")
              }
            case e: IOException ⇒
              indentedLogger.logDebug("", "IOException caught while closing OutputStream after forward")
          }
        }
      }

      val cxr = ConnectionResult(
        url                = effectiveResourceURI,
        statusCode         = replaceAllResponse.getStatus max 0,
        headers            = org.orbeon.oxf.http.Headers.EmptyHeaders,
        content            = dummyContent,
        dontHandleResponse = true
      )

      // Here we cause dispatch xforms-submit-error upon getting a non-success error code, even though the
      // response has already been written out. This gives the form author a chance to do something in cases
      // the response is buffered, for example do a sendError().
      // HOWEVER: We don't do this
      if (! p.isDeferredSubmissionSecondPass) {
        if (! NetUtils.isSuccessCode(cxr.statusCode) && ! p.isDeferredSubmissionSecondPass)
          throw new XFormsSubmissionException(
            submission       = submission,
            message          = s"xf:submission for submission id: ${submission.getId}, error code received: ${cxr.statusCode}",
            description      = "processing submission response",
            submitErrorEvent = new XFormsSubmitErrorEvent(submission, ErrorType.ResourceError, cxr)
          )
      }  else {
        // Two reasons: 1. We don't want to modify the document state 2. This can be called outside of the document
        // lock, see XFormsServer.
      }
      cxr
    } else {
      // We must intercept the reply
      val responseAdapter = new LocalResponse(response: URLRewriter)
      submissionProcess.process(localRequest, responseAdapter)

      ConnectionResult(
        url         = effectiveResourceURI,
        statusCode  = responseAdapter.statusCode max 0,
        headers     = responseAdapter.capitalizedHeaders,
        content     = responseAdapter.streamedContent
      )
    }
  }
}

object BaseSubmission {

  def isLogBody =
    XFormsProperties.getDebugLogging.contains("submission-body")
}

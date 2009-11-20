/**
 * Copyright (C) 2009 Orbeon, Inc.
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
package org.orbeon.oxf.processor.converter;

import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.ExternalContext.Request;
import org.orbeon.oxf.pipeline.api.ExternalContext.Response;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.portlet.PortletExternalContext;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.ProcessorInputOutputInfo;
import org.orbeon.oxf.processor.ProcessorOutput;
import org.orbeon.oxf.xml.XMLConstants;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.saxrewrite.DocumentRootState;
import org.orbeon.oxf.xml.saxrewrite.FragmentRootState;
import org.orbeon.oxf.xml.saxrewrite.State;
import org.orbeon.oxf.xml.saxrewrite.StatefulHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.StringTokenizer;

/**
 * Java impl of oxf-rewrite.xsl. Uses GOF state pattern + SAX to get the job done. The state machine ad hoc and relies a
 * bit on the simplicity of the transformation that we are performing.
 *
 * Wrt the transformation, here is the comment from oxf-rewrite.xsl :
 *
 * This stylesheet rewrites HTML or XHTML for servlets and portlets. URLs are parsed, so it must be made certain that
 * the URLs are well-formed. Absolute URLs are not modified. Relative or absolute paths are supported, as well as the
 * special case of a URL starting with a query string (e.g. "?name=value"). This last syntax is supported by most Web
 * browsers.
 *
 * A. For portlets, it does the following:
 *
 * 1. Rewrite form/@action to WSRP action URL encoding
 * 2. Rewrite a/@href and link/@href to WSRP render encoding
 * 3. Rewrite img/@src, input[@type='image']/@src and script/@src to WSRP resource URL encoding
 * 4. If no form/@method is supplied, force an HTTP POST
 * 5. Escape any wsrp_rewrite occurrence in text not within a script or
 *
 * SCRIPT element to wsrp_rewritewsrp_rewrite. WSRP 1.0 does not appear to specify a particular escape sequence, but we
 * use this one in Orbeon Forms Portal. The escaped sequence is recognized by the Orbeon Forms Portlet and restored to
 * the original sequence, so it is possible to include the string wsrp_rewrite within documents.
 *
 * 6. Occurrences of wsrp_rewrite found within script or SCRIPT elements, as well as occurrences within attributes,
 * are left untouched. This allows them to be recognized by the Orbeon Forms Portlet and rewritten.
 *
 * Known issues for portlets:
 *
 * o The input document should not contain;
 * o elements and attribute containing wsrp_rewrite
 * o namespace URIs containing wsrp_rewrite
 * o processing instructions containing wsrp_rewrite
 *
 * B. For servlets, it rewrites the URLs to be absolute paths, and prepends the context path.
 */
abstract class AbstractRewrite extends ProcessorImpl {
    /**
     * Name of the input that receives the content that is to be rewritten.
     */
    private static final String REWRITE_IN = "rewrite-in";
    static final String SCRIPT_ELT = "script";
    static final String ACTION_ATT = "action";
    static final String METHOD_ATT = "method";
    static final String HREF_ATT = "href";
    static final String SRC_ATT = "src";
    static final String BACKGROUND_ATT = "background";
    static final String NOREWRITE_ATT = "url-norewrite";

    /**
     * Base state. Simply forwards data to the destination content handler and returns itself. That is unless the
     * (element) depth becomes negative after an end element event. In this case the previous state is returned. This
     * means btw that we are really only considering state changes on start and end element events.
     */
    protected static abstract class State2 extends State {

        protected final Request request;

        /**
         * Performs the URL rewrites.
         */
        protected final Response response;
        /**
         * A sub-state, if you will. Didn't implement this as a sub-class of State as it doesn't change during the
         * course of a transformation.
         */
        protected final boolean isPortlet;
        /**
         * Could have been another State. However since the value is determined in one state and then used by a
         * 'descendant' state doing so would have meant that descendant would have to walk it's ancestors to get the
         * value. So, since making this a field instead of a separate State sub-class was easier to implement and is
         * faster a field was used.
         */
        protected int scriptDepth;

        protected final String rewriteURI;

        /**
         * @param previousState     The previous state.
         * @param contentHandler    The destination for the rewrite transformation.
         * @param request
         * @param response          Used to perform URL rewrites.
         * @param isPortlet         Whether or not the context is a portlet context.
         * @param scriptDepth       How below script elt we are.
         * @param rewriteURI        URI of elements (i.e. xhtml uri or "") of elements that need rewriting.
         */
        State2(final State previousState, final ContentHandler contentHandler, final Request request, final Response response,
               final boolean isPortlet, final int scriptDepth, final String rewriteURI) {
            super(previousState, contentHandler);
            this.request = request;
            this.response = response;
            this.isPortlet = isPortlet;
            this.scriptDepth = scriptDepth;
            this.rewriteURI = rewriteURI;
        }

        /**
         * Adjusts scriptDepth
         */
        final void scriptDepthOnStart(final String ns, final String lnam) {
            if (rewriteURI.equals(ns) && SCRIPT_ELT.equals(lnam)) {
                scriptDepth++;
            }
        }

        /**
         * Adjusts scriptDepth
         */
        final void scriptDepthOnEnd(final String ns, final String lnam) {
            if (rewriteURI.equals(ns) && SCRIPT_ELT.equals(lnam)) {
                scriptDepth--;
            }
        }

        protected void endElementStart(final String ns, final String lnam, final String qnam)
                throws SAXException {
            scriptDepthOnEnd(ns, lnam);
            super.endElementStart(ns, lnam, qnam);
        }
    }

    /**
     * The rewrite state.  Essentially this corresponds to the default mode of oxf-rewrite.xsl.
     * Basically this:
     * <ul>
     * <li>Rewrites attributes in start element event when need be.
     * <li>Accumulates text from characters events so that proper char content rewriting can
     * happen.
     * <li>On an event that indicates the end of potentially rewritable text, e.g. start element,
     * rewrites and forwards the accumulated characters.
     * <li>When explicit no write is indicated, e.g. we see attributes no-rewrite=true, then
     * transition to the NoRewriteState.
     * </ul>
     */
    static class RewriteState extends State2 {

        /**
         * Used to accumulate characters from characters event. Lazily init'd in characters. Btw we use
         * CharacterBuffer instead of StringBuffer because :
         *
         * o We want something that works in JDK 1.4.
         * o We don't want the performance penalty of StringBuffer's synchronization in JDK 1.5.
         *
         * Btw if we didn't care about 1.4 we could use StringBuilder instead.
         */
        private java.nio.CharBuffer charactersBuf;

        /**
         * Calls super( ... ) and initializes wsrprewriteMatcher with "wsrp_rewrite"
         *
         * @param rewriteURI
         */
        RewriteState(final State stt, final ContentHandler contentHandler, final Request request, final Response response
                , final boolean isPortlet, final int scriptDepth, final String rewriteURI) {
            super(stt, contentHandler, request, response, isPortlet, scriptDepth, rewriteURI);
        }

        /**
         * Handler for {http://www.w3.org/1999/xhtml}{elt name}.  Assumes namespace test has already
         * happened.  Implements :
         * <pre>
         *   <xsl:template match="xhtml:{elt name}[@{res attrib name}]" >
         *     <xsl:copy>
         *       <xsl:copy-of select="@*[namespace-uri() = '']"/>
         *         <xsl:attribute name="{res attrib name}">
         *           <xsl:value-of select="context:rewriteResourceURL(@{res attrib name})"/>
         *         </xsl:attribute>
         *         <xsl:apply-templates/>
         *       </xsl:copy>
         *   </xsl:template>
         * </pre>
         *
         * If match is satisfied then modified event is sent to destination contentHandler.
         *
         * @return null if match is not satisfied and this otherwise.
         * @throws SAXException if destination contentHandler throws SAXException
         */
        private State2 handleEltWithResource
                (final String elt, final String resAtt, final String ns, final String lnam
                        , final String qnam, final Attributes atts)
                throws SAXException {
            State2 ret = null;
            done :
            if (elt.equals(lnam)) {

                final String res = atts.getValue("", resAtt);
                if (res == null) break done;

                ret = this;
                final AttributesImpl newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);
                final String newRes = response.rewriteResourceURL(res, false);
                final int idx = newAtts.getIndex("", resAtt);
                newAtts.setValue(idx, newRes);
                contentHandler.startElement(ns, lnam, qnam, newAtts);
            }
            return ret;
        }

        /**
         * Handle xhtml:object
         */
        private State2 handleObject
                (final String ns, final String lnam
                        , final String qnam, final Attributes atts)
                throws SAXException {
            State2 ret = null;
            done :
            if ("object".equals(lnam)) {

                final String codebaseAttribute = atts.getValue("", "codebase");
                final String classidAttribute = atts.getValue("", "classid");
                final String dataAttribute = atts.getValue("", "data");
                final String usemapAttribute = atts.getValue("", "usemap");
                final String archiveAttribute = atts.getValue("", "archive");// space-separated

                if (classidAttribute == null && codebaseAttribute == null && dataAttribute == null && usemapAttribute == null) break done;

                ret = this;
                final AttributesImpl newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);
                if (codebaseAttribute != null) {
                    final String newAttribute = response.rewriteResourceURL(codebaseAttribute, false);
                    final int idx = newAtts.getIndex("", "codebase");
                    newAtts.setValue(idx, newAttribute);
                } else {
                    // We don't rewrite these attributes if there is a codebase
                    if (classidAttribute != null) {
                        final String newAttribute = response.rewriteResourceURL(classidAttribute, false);
                        final int idx = newAtts.getIndex("", "classid");
                        newAtts.setValue(idx, newAttribute);
                    }
                    if (dataAttribute != null) {
                        final String newAttribute = response.rewriteResourceURL(dataAttribute, false);
                        final int idx = newAtts.getIndex("", "data");
                        newAtts.setValue(idx, newAttribute);
                    }
                    if (usemapAttribute != null) {
                        final String newAttribute = response.rewriteResourceURL(usemapAttribute, false);
                        final int idx = newAtts.getIndex("", "usemap");
                        newAtts.setValue(idx, newAttribute);
                    }
                    if (archiveAttribute != null) {
                        final StringTokenizer st = new StringTokenizer(archiveAttribute, " ");
                        final StringBuilder sb = new StringBuilder(archiveAttribute.length() * 2);
                        boolean first = true;
                        while (st.hasMoreTokens()) {
                            final String currentArchive = st.nextToken().trim();
                            final String newArchive = response.rewriteResourceURL(currentArchive, false);
                            if (!first) {
                                sb.append(' ');
                            }
                            sb.append(newArchive);
                            first = false;
                        }
                        final int idx = newAtts.getIndex("", "archive");
                        newAtts.setValue(idx, sb.toString());
                    }
                }

                contentHandler.startElement(ns, lnam, qnam, newAtts);
            }
            return ret;
        }

        /**
         * Handle xhtml:applet
         */
        private State2 handleApplet
                (final String ns, final String lnam
                        , final String qnam, final Attributes atts)
                throws SAXException {
            State2 ret = null;
            done :
            if ("applet".equals(lnam)) {

                final String codebaseAttribute = atts.getValue("", "codebase");
                final String archiveAttribute = atts.getValue("", "archive");// comma-separated
//                final String codeAttribute = atts.getValue("", "code");// not clear whether this needs to be rewritten

                if (archiveAttribute == null && codebaseAttribute == null) break done;

                ret = this;
                final AttributesImpl newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);
                if (codebaseAttribute != null) {
                    final String newAttribute = response.rewriteResourceURL(codebaseAttribute, false);
                    final int idx = newAtts.getIndex("", "codebase");
                    newAtts.setValue(idx, newAttribute);
                } else {
                    // We don't rewrite the @archive attribute if there is a codebase
                    final StringTokenizer st = new StringTokenizer(archiveAttribute, ",");
                    final StringBuilder sb = new StringBuilder(archiveAttribute.length() * 2);
                    boolean first = true;
                    while (st.hasMoreTokens()) {
                        final String currentArchive = st.nextToken().trim();
                        final String newArchive = response.rewriteResourceURL(currentArchive, false);
                        if (!first) {
                            sb.append(' ');
                        }
                        sb.append(newArchive);
                        first = false;
                    }
                    final int idx = newAtts.getIndex("", "archive");
                    newAtts.setValue(idx, sb.toString());
                }

                contentHandler.startElement(ns, lnam, qnam, newAtts);
            }
            return ret;
        }

        /**
         * Handler for {http://www.w3.org/1999/xhtml}a.  Assumes namespace test has already
         * happened.  Implements :
         * <pre>
         *   <xsl:template match="xhtml:a[@href]" >
         *     <xsl:copy>
         *       <xsl:copy-of select="@*[namespace-uri() = '']"/>
         *         <xsl:attribute name="href">
         *           <xsl:choose>
         *             <xsl:when test="not(@f:url-type) or @f:url-type = 'render'">
         *               <xsl:value-of select="context:rewriteRenderURL(@href)"/>
         *             </xsl:when>
         *             <xsl:when test="@f:url-type = 'action'">
         *               <xsl:value-of select="context:rewriteActionURL(@href)"/>
         *             </xsl:when>
         *             <xsl:when test="@f:url-type = 'resource'">
         *               <xsl:value-of select="context:rewriteResourceURL(@href)"/>
         *             </xsl:when>
         *           </xsl:choose>
         *         </xsl:attribute>
         *       <xsl:apply-templates/>
         *     </xsl:copy>
         *   </xsl:template>
         * </pre>
         *
         * If match is satisfied then modified event is sent to destination contentHandler.
         *
         * @return null if match is not satisfied and this otherwise.
         * @throws SAXException if destination contentHandler throws SAXException
         */
        private State2 handleA
                (final String ns, final String lnam, final String qnam, final Attributes atts)
                throws SAXException {
            State2 ret = null;
            done :
            if ("a".equals(lnam)) {

                final String href = atts.getValue("", HREF_ATT);
                if (href == null) break done;

                ret = this;
                final AttributesImpl newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);
                final String urlType = atts.getValue(XMLConstants.OPS_FORMATTING_URI, "url-type");
                final String portletMode = atts.getValue(XMLConstants.OPS_FORMATTING_URI, "portlet-mode");
                final String windowState = atts.getValue(XMLConstants.OPS_FORMATTING_URI, "window-state");

                final String newHref;
                if (urlType == null || "render".equals(urlType)) {
                    newHref = response.rewriteRenderURL(href, portletMode, windowState);
                } else if ("action".equals(urlType)) {
                    newHref = response.rewriteActionURL(href, portletMode, windowState);
                } else if ("resource".equals(urlType)) {
                    newHref = response.rewriteResourceURL(href, false);
                } else {
                    newHref = null;
                }
                final int idx = newAtts.getIndex("", HREF_ATT);
                if (newHref == null && idx != -1) {
                    newAtts.removeAttribute(idx);
                } else {
                    newAtts.setValue(idx, newHref);
                }
                contentHandler.startElement(ns, lnam, qnam, newAtts);
            }
            return ret;
        }

        /**
         * Handler for {http://www.w3.org/1999/xhtml}area.  Assumes namespace test has already
         * happened.  Implements :
         * <pre>
         *   <xsl:template match="xhtml:area[@href]" >
         *     <xsl:copy>
         *       <xsl:copy-of select="@*[namespace-uri() = '']"/>
         *         <xsl:attribute name="href">
         *           <xsl:value-of select="context:rewriteActionURL(@href)"/>
         *         </xsl:attribute>
         *         <xsl:apply-templates/>
         *       </xsl:copy>
         *   </xsl:template>
         * </pre>
         *
         * If match is satisfied then modified event is sent to destination contentHandler.
         *
         * @return null if match is not satisfied and this otherwise.
         * @throws SAXException if destination contentHandler throws SAXException
         */
        private State2 handleArea
                (final String ns, final String lnam, final String qnam, final Attributes atts)
                throws SAXException {
            State2 ret = null;
            done :
            if ("area".equals(lnam)) {

                final String href = atts.getValue("", HREF_ATT);
                if (href == null) break done;

                ret = this;
                final AttributesImpl newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);
                final String newHref = response.rewriteActionURL(href);
                final int idx = newAtts.getIndex("", HREF_ATT);
                newAtts.setValue(idx, newHref);
                contentHandler.startElement(ns, lnam, qnam, newAtts);
            }
            return ret;
        }

        /**
         * Handler for {http://www.w3.org/1999/xhtml}input.  Assumes namespace test has already
         * happened.  Implements :
         * <pre>
         *   <xsl:template match="xhtml:input[@type='image' and @src]" >
         *     <xsl:copy>
         *       <xsl:copy-of select="@*[namespace-uri() = '']"/>
         *         <xsl:attribute name="src">
         *           <xsl:value-of select="context:rewriteActionURL(@src)"/>
         *         </xsl:attribute>
         *         <xsl:apply-templates/>
         *       </xsl:copy>
         *   </xsl:template>
         * </pre>
         *
         * If match is satisfied then modified event is sent to destination contentHandler.
         *
         * @return null if @type='image' test is not satisfied and
         *         handleEltWithResource( "input", "src", ... ) otherwise.
         * @throws SAXException if destination contentHandler throws SAXException
         */
        private State2 handleInput
                (final String ns, final String lnam, final String qnam, final Attributes atts)
                throws SAXException {
            final State2 ret;
            final String typ = atts.getValue("", "type");
            if ("image".equals(typ)) {
                ret = handleEltWithResource("input", SRC_ATT, ns, lnam, qnam, atts);
            } else {
                ret = null;
            }
            return ret;
        }

        /**
         * Handler for {http://www.w3.org/1999/xhtml}form.  Assumes namespace test has already
         * happened.  Implements :
         * <pre>
         *   <xsl:template match="form | xhtml:form">
         *     <xsl:copy>
         *       <xsl:copy-of select="@*[namespace-uri() = '']"/>
         *       <xsl:choose>
         *         <xsl:when test="@action">
         *           <xsl:attribute name="action">
         *             <xsl:value-of select="context:rewriteActionURL(@action)"/>
         *           </xsl:attribute>
         *         </xsl:when>
         *         <xsl:otherwise>
         *           <xsl:attribute name="action">
         *             <xsl:value-of select="context:rewriteActionURL('')"/>
         *           </xsl:attribute>
         *         </xsl:otherwise>
         *       </xsl:choose>
         *       <!-- Default is POST instead of GET for portlets -->
         *       <xsl:if test="not(@method) and $container-type/* = 'portlet'">
         *         <xsl:attribute name="method">post</xsl:attribute>
         *       </xsl:if>
         *       <xsl:choose>
         *         <xsl:when test="@portlet:is-portlet-form = 'true'">
         *           <xsl:apply-templates mode="norewrite"/>
         *         </xsl:when>
         *         <xsl:otherwise>
         *           <xsl:apply-templates/>
         *         </xsl:otherwise>
         *       </xsl:choose>
         *     </xsl:copy>
         *   </xsl:template>
         * </pre>
         *
         * If match is satisfied then modified event is sent to destination contentHandler.
         *
         * @return null match is not satisfied,
         *         new NoRewriteState( this, contentHandler, response, isPortlet
         *         , haveScriptAncestor ) if is-portlet-form='true', and this
         *         otherwise.
         * @throws SAXException if destination contentHandler throws SAXException
         */
        private State2 handleForm
                (final String ns, final String lnam, final String qnam, final Attributes atts)
                throws SAXException {
            final State2 ret;
            if ("form".equals(lnam)) {

                final AttributesImpl newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);

                final String actn = newAtts.getValue("", ACTION_ATT);
                final String newActn;
                if (actn == null) {
                    newActn = response.rewriteActionURL("");
                    newAtts.addAttribute("", ACTION_ATT, ACTION_ATT, "", newActn);
                } else {
                    final int idx = newAtts.getIndex("", ACTION_ATT);
                    newActn = response.rewriteActionURL(actn);
                    newAtts.setValue(idx, newActn);
                }

                if (atts.getValue("", METHOD_ATT) == null && isPortlet) {
                    newAtts.addAttribute("", METHOD_ATT, METHOD_ATT, "", "post");
                }

                final String isPrtltFrm
                        = atts.getValue("http://orbeon.org/oxf/xml/portlet", "is-portlet-form");
                if ("true".equals(isPrtltFrm)) {
                    ret = new NoRewriteState
                            (this, contentHandler, request, response, isPortlet, scriptDepth, rewriteURI);
                } else {
                    ret = this;
                }
                contentHandler.startElement(ns, lnam, qnam, newAtts);
            } else {
                ret = null;
            }
            return ret;
        }

        /**
         * If we have accumulated character data rewrite it and forward it.  Implements :
         * <pre>
         *   <xsl:template match="text()">
         *     <xsl:value-of
         *       select="replace(current(), 'wsrp_rewrite', 'wsrp_rewritewsrp_rewrite')"/>
         *     <xsl:apply-templates/>
         *   </xsl:template>
         * </pre>
         * If there no character data has been accumulated do nothing.  Also clears buffer.
         */
        private void flushCharacters() throws SAXException {
            final int bfLen = charactersBuf == null ? 0 : charactersBuf.position();
            if (bfLen > 0) {
                charactersBuf.flip();
                final char[] chs = charactersBuf.array();
                final int chsStrt = charactersBuf.arrayOffset();
                int last = 0;
                if (last < bfLen) {
                    final int len = bfLen - last;
                    contentHandler.characters(chs, chsStrt + last, len);
                }
                charactersBuf.clear();
            }
        }

        /**
         * Just calls flushCharacters and super.endElement( ... )
         */
        protected void endElementStart(final String ns, final String lnam, final String qnam)
                throws SAXException {
            flushCharacters();
            super.endElementStart(ns, lnam, qnam);
        }

        /**
         * Just calls flushCharacters then tests the event data.  If
         * <ul>
         * <li>
         *
         * @url-norewrite='true' then forward the event to the destination content handler and
         * return new NoRewriteState( ... ), otherwise
         * </li>
         * <li>
         * if ns.equals( XHTML_URI ) then
         * <ul>
         * <li>if one of the handleXXX methods returns non-null do nothing, otherwise</li>
         * <li>
         * forward the event to the destination content handler and return this, otherwise
         * </li>
         * </ul>
         * </li>
         * <li>
         * if the element is {http://orbeon.org/oxf/xml/formatting}rewrite then implement :
         * <pre>
         *       <xsl:choose>
         *         <xsl:when test="@type = 'action'">
         *           <xsl:value-of select="context:rewriteActionURL(@url)"/>
         *         </xsl:when>
         *         <xsl:when test="@type = 'render'">
         *           <xsl:value-of select="context:rewriteRenderURL(@url)"/>
         *         </xsl:when>
         *         <xsl:otherwise>
         *           <xsl:value-of select="context:rewriteResourceURL(@url)"/>
         *         </xsl:otherwise>
         *       </xsl:choose>
         *     </pre>
         * Note this means that we forward characters to the destination content handler instead
         * of a start element event, otherwise
         * </li>
         * <li>
         * simply forward the event as is to the destination content handler and return this.
         * </li>
         * </ul>
         */
        protected State startElementStart(final String ns, final String lnam, final String qnam, Attributes atts) throws SAXException {

            final int noRewriteIndex = atts.getIndex(XMLConstants.OPS_FORMATTING_URI, NOREWRITE_ATT);
            final String noRewriteValue = atts.getValue(noRewriteIndex);
            State ret = null;
            flushCharacters();

            if (noRewriteValue != null) {
                // Remove f:url-norewrite attribute
                final AttributesImpl attributesImpl = new AttributesImpl(atts);
                attributesImpl.removeAttribute(noRewriteIndex);
                atts = attributesImpl;
            }

            done :
            if ("true".equals(noRewriteValue)) {
                final State stt = new NoRewriteState(this, contentHandler, request, response, isPortlet, scriptDepth, rewriteURI);
                ret = stt.startElement(ns, lnam, qnam, atts);
            } else if (XMLConstants.OPS_FORMATTING_URI.equals(ns) && "rewrite".equals(lnam)) {
                final String typ = atts.getValue("", "type");
                final String url = atts.getValue("", "url");
                if (url != null) {
                    final String newURL;
                    if ("action".equals(typ)) {
                        newURL = response.rewriteActionURL(url);
                    } else if ("render".equals(typ)) {
                        newURL = response.rewriteRenderURL(url);
                    } else {
                        newURL = response.rewriteResourceURL(url, false);
                    }
                    final char[] chs = newURL.toCharArray();
                    contentHandler.characters(chs, 0, chs.length);
                }
            } else {
                scriptDepthOnStart(ns, lnam);
                if (rewriteURI.equals(ns)) {
                    ret = handleA(ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleForm(ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleArea(ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleEltWithResource("link", HREF_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleEltWithResource("img", SRC_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleEltWithResource("frame", SRC_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;
                    
                    ret = handleEltWithResource("iframe", SRC_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleEltWithResource(SCRIPT_ELT, SRC_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleInput(ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleEltWithResource("td", BACKGROUND_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleEltWithResource("body", BACKGROUND_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleObject(ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    ret = handleApplet(ns, lnam, qnam, atts);
                    if (ret != null) break done;

                    // Not valid in HTML, but useful for e.g. Dojo contentPane
                    ret = handleEltWithResource("div", HREF_ATT, ns, lnam, qnam, atts);
                    if (ret != null) break done;
                }
                ret = this;
                contentHandler.startElement(ns, lnam, qnam, atts);
            }
            return ret;
        }

        /**
         * If haveScriptAncestor then just forward data to destination contentHandler. Otherwise store that data in the
         * buffer and do not forward. Also manages init'ing and growing charactersBuf as need be.
         */
        public State characters(final char[] ch, final int strt, final int len)
                throws SAXException {
            if (scriptDepth > 0) {
                contentHandler.characters(ch, strt, len);
            } else {
                final int bufLen = charactersBuf == null ? 0 : charactersBuf.position();
                final int cpcty = bufLen + (len * 2);
                if (charactersBuf == null || charactersBuf.remaining() < cpcty) {
                    final java.nio.CharBuffer newBuf = java.nio.CharBuffer.allocate(cpcty);
                    if (charactersBuf != null) {
                        charactersBuf.flip();
                        newBuf.put(charactersBuf);
                    }
                    charactersBuf = newBuf;
                }
                charactersBuf.put(ch, strt, len);
            }
            return this;
        }

        /**
         * Just calls flushCharacters and super.ignorableWhitespace( ... )
         */
        public State ignorableWhitespace(final char[] ch, final int strt, final int len)
                throws SAXException {
            flushCharacters();
            return super.ignorableWhitespace(ch, strt, len);
        }

        /**
         * Just calls flushCharacters and super.processingInstruction( ... )
         */
        public State processingInstruction(final String trgt, final String dat)
                throws SAXException {
            flushCharacters();
            return super.processingInstruction(trgt, dat);
        }

        /**
         * Just calls flushCharacters and super.skippedEntity( ... )
         */
        public State skippedEntity(final String nam) throws SAXException {
            flushCharacters();
            return super.skippedEntity(nam);
        }
    }

    /**
     * Essentially this corresponds to the norewrite mode of oxf-rewrite.xsl. i.e. Just forwards events to the
     * destination content handler until we finish the initial element (depth < 0) or until it encounters
     * @url-norewrite='false'. In the first case transitions to the previous state and in the second case it transitions
     * to new RewriteState(this, contentHandler, response, isPortlet, haveScriptAncestor).
     */
    static class NoRewriteState extends State2 {
        NoRewriteState(final State2 previousState, final ContentHandler contentHandler, final Request request, final Response response,
                final boolean isPortlet, final int scriptDepth, final String rewriteURI) {
            super(previousState, contentHandler, request, response, isPortlet, scriptDepth, rewriteURI);
        }

        protected State startElementStart(final String ns, final String lnam, final String qnam, Attributes atts) throws SAXException {

            final int noRewriteIndex = atts.getIndex(XMLConstants.OPS_FORMATTING_URI, NOREWRITE_ATT);
            final String noRewriteValue = atts.getValue(noRewriteIndex);
            final State ret;

            if (noRewriteValue != null) {
                // Remove f:url-norewrite attribute
                final AttributesImpl attributesImpl = new AttributesImpl(atts);
                attributesImpl.removeAttribute(noRewriteIndex);
                atts = attributesImpl;
            }

            if ("false".equals(noRewriteValue)) {
                final State stt = new RewriteState(this, contentHandler, request, response, isPortlet, scriptDepth, rewriteURI);
                ret = stt.startElement(ns, lnam, qnam, atts);
            } else {
                scriptDepthOnStart(ns, lnam);
                final Attributes newAtts = XMLUtils.getAttributesFromDefaultNamespace(atts);
                contentHandler.startElement(ns, lnam, qnam, newAtts);
                ret = this;
            }
            return ret;
        }
    }

    /**
     * Namespace of the elements that are to be rewritten.
     */
    final String rewriteURI;

    /**
     * Just declares input 'rewrite-in' and output 'rewrite-out'.
     *
     * @param rewriteURI e.g. "http://www.w3.org/1999/xhtml" or ""
     */
    public AbstractRewrite(final String rewriteURI) {
        this.rewriteURI = rewriteURI;
        final ProcessorInputOutputInfo in = new ProcessorInputOutputInfo(REWRITE_IN);
        addInputInfo(in);
        final ProcessorInputOutputInfo out = new ProcessorInputOutputInfo("rewrite-out");
        addOutputInfo(out);
    }

    /**
     * @return new RewriteOutput( cls, nam ) after adding it with addOutput.
     */
    public ProcessorOutput createOutput(final String name) {

        final ProcessorOutput processorOutput = new ProcessorImpl.CacheableTransformerOutputImpl(getClass(), name) {

            /**
             * Creates a StatefulHandler and uses that to translate the events from the input, rewrite-in, and then
             * send them to the contentHandler (the output).
             */
            public void readImpl(final PipelineContext pipelineContext, final ContentHandler contentHandler) {
                readInputAsSAX(pipelineContext, REWRITE_IN, getRewriteContentHandler(pipelineContext, contentHandler, false));
            }
        };
        addOutput(name, processorOutput);
        return processorOutput;
    }

    public ContentHandler getRewriteContentHandler(final PipelineContext pipelineContext, final ContentHandler contentHandler, final boolean fragment) {
        // Get ExternalContext
        final ExternalContext externalContext
                = (ExternalContext) pipelineContext.getAttribute(PipelineContext.EXTERNAL_CONTEXT);

        final boolean isPortlet = externalContext instanceof PortletExternalContext;

        // Do the conversion
        final State rootState;
        if (fragment) {
            // Start directly with rewrite state
            final FragmentRootState fragmentRootState = new FragmentRootState(null, contentHandler);
            final State afterRootState = new RewriteState(fragmentRootState, contentHandler, externalContext.getRequest(), externalContext.getResponse(), isPortlet, 0, rewriteURI);
            fragmentRootState.setNextState(afterRootState);
            rootState = fragmentRootState;
        } else {
            // Start with root filter
            final DocumentRootState documentRootState = new DocumentRootState(null, contentHandler);
            final State afterRootState = new RewriteState(documentRootState, contentHandler, externalContext.getRequest(), externalContext.getResponse(), isPortlet, 0, rewriteURI);
            documentRootState.setNextState(afterRootState);
            rootState = documentRootState;
        }
        return new StatefulHandler(rootState);
    }
}

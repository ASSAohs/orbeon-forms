/**
 *  Copyright (C) 2005 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms;

import org.apache.log4j.Logger;
import org.dom4j.*;
import org.dom4j.io.DocumentSource;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.util.LoggerFactory;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.util.XPathCache;
import org.orbeon.oxf.xforms.action.actions.XFormsLoadAction;
import org.orbeon.oxf.xforms.action.actions.XFormsSetvalueAction;
import org.orbeon.oxf.xforms.control.controls.XFormsUploadControl;
import org.orbeon.oxf.xforms.event.XFormsEvent;
import org.orbeon.oxf.xforms.event.XFormsEventHandlerContainer;
import org.orbeon.oxf.xforms.event.XFormsEventTarget;
import org.orbeon.oxf.xforms.event.XFormsEvents;
import org.orbeon.oxf.xforms.event.events.*;
import org.orbeon.oxf.xforms.event.events.XFormsSubmitErrorEvent.ErrorType;
import org.orbeon.oxf.xforms.function.XFormsFunction;
import org.orbeon.oxf.xforms.processor.XFormsServer;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.XMLConstants;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import org.orbeon.oxf.xml.dom4j.ExtendedLocationData;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.orbeon.saxon.dom4j.NodeWrapper;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NodeInfo;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Represents an XForms model submission instance.
 *
 * TODO: This badly needs to be modularized instead of being a soup of "ifs"!
 */
public class XFormsModelSubmission implements XFormsEventTarget, XFormsEventHandlerContainer {

	public final static Logger logger = LoggerFactory.createLogger(XFormsModelSubmission.class);

    private final XFormsContainingDocument containingDocument;
    private final String id;
    private final XFormsModel model;
    private final Element submissionElement;
    private boolean submissionElementExtracted = false;

    private String avtActionOrResource; // required unless there is a nested xforms:resource element
    private String resolvedActionOrResource;
    private String method; // required

    private boolean validate = true;
    private boolean relevant = true;

    private String serialization;
    private boolean serialize = true;// for backward compability only (was in XForms 1.1 draft)

    private String target;

    private String version;
    private boolean indent;
    private String mediatype;
    private String encoding;
    private boolean omitxmldeclaration;
    private Boolean standalone;
    private String cdatasectionelements;

    private String replace = XFormsConstants.XFORMS_SUBMIT_REPLACE_ALL;
    private String replaceInstanceId;
    private String xxfReplaceInstanceId;
    private String separator = ";";
    private String includenamespaceprefixes;

    private String avtXXFormsUsername;
    private String resolvedXXFormsUsername;
    private String avtXXFormsPassword;
    private String resolvedXXFormsPassword;
    private String avtXXFormsReadonly;
    private String resolvedXXFormsReadonly;
    private String avtXXFormsShared;
    private String resolvedXXFormsShared;
    private String avtXXFormsTarget;
    private String resolvedXXFormsTarget;
    private String avtXXFormsHandleXInclude;
    private boolean resolvedXXFormsHandleXInclude;

    private boolean xxfFormsEnsureUploads;

    private List headerNames;
    private Map headerNameValues;

    private boolean xxfShowProgress;

    private boolean fURLNorewrite;

    public XFormsModelSubmission(XFormsContainingDocument containingDocument, String id, Element submissionElement, XFormsModel model) {
        this.containingDocument = containingDocument;
        this.id = id;
        this.submissionElement = submissionElement;
        this.model = model;
    }

    public XFormsContainingDocument getContainingDocument() {
        return containingDocument;
    }

    public Element getSubmissionElement() {
        return submissionElement;
    }


    public boolean isXxfShowProgress() {
        return xxfShowProgress;
    }

    public String getReplace() {
        return replace;
    }

    public String getResolvedXXFormsTarget() {
        return resolvedXXFormsTarget;
    }

    private void extractSubmissionElement() {
        if (!submissionElementExtracted) {

            avtActionOrResource = submissionElement.attributeValue("resource");
            if (avtActionOrResource == null) // @resource has precedence over @action
                avtActionOrResource = submissionElement.attributeValue("action");
            if (avtActionOrResource == null) {
                // TODO: For XForms 1.1, support @resource and nested xforms:resource
                throw new XFormsSubmissionException("xforms:submission: action attribute or resource attribute is missing.",
                        "processing xforms:submission attributes");
            }

            method = submissionElement.attributeValue("method");
            method = Dom4jUtils.qNameToexplodedQName(Dom4jUtils.extractAttributeValueQName(submissionElement, "method"));

            validate = !"false".equals(submissionElement.attributeValue("validate"));
            relevant = !"false".equals(submissionElement.attributeValue("relevant"));

            serialization = submissionElement.attributeValue("serialization");
            if (serialization != null) {
                serialize = !serialization.equals("none");
            } else {
                // For backward compability only, support @serialize if there is no @serialization attribute
                serialize = !"false".equals(submissionElement.attributeValue("serialize"));
            }

            target = submissionElement.attributeValue("target");

            version = submissionElement.attributeValue("version");

            if (submissionElement.attributeValue("indent") != null) {
                indent = Boolean.valueOf(submissionElement.attributeValue("indent")).booleanValue();
            }
            mediatype = submissionElement.attributeValue("mediatype");
            encoding = submissionElement.attributeValue("encoding");
            if (submissionElement.attributeValue("omitxmldeclaration") != null) {
                omitxmldeclaration = Boolean.valueOf(submissionElement.attributeValue("omit-xml-declaration")).booleanValue();
            }
            if (submissionElement.attributeValue("standalone") != null) {
                standalone = new Boolean(submissionElement.attributeValue("standalone"));
            }

            cdatasectionelements = submissionElement.attributeValue("cdata-section-elements");
            if (submissionElement.attributeValue("replace") != null) {
                replace = submissionElement.attributeValue("replace");

                if (replace.equals("instance")) {
                    replaceInstanceId = XFormsUtils.namespaceId(containingDocument, submissionElement.attributeValue("instance"));
                    xxfReplaceInstanceId = XFormsUtils.namespaceId(containingDocument, submissionElement.attributeValue(XFormsConstants.XXFORMS_INSTANCE_QNAME));
                }
            }
            if (submissionElement.attributeValue("separator") != null) {
                separator = submissionElement.attributeValue("separator");
            }
            includenamespaceprefixes = submissionElement.attributeValue("includenamespaceprefixes");

            // Headers
            {
                final List headerElements = submissionElement.elements("header");
                if (headerElements.size() > 0) {

                    headerNames = new ArrayList();
                    headerNameValues = new HashMap();

                    for (Iterator i = headerElements.iterator(); i.hasNext();) {
                        final Element headerElement = (Element) i.next();

                        // TODO: Handle @nodeset
                        final Element headerNameElement = headerElement.element("name");
                        if (headerNameElement == null)
                            throw new XFormsSubmissionException("Missing <name> child element of <header> element", "processing <header> elements");
                        final Element headerValueElement = headerElement.element("value");
                        if (headerValueElement == null)
                            throw new XFormsSubmissionException("Missing <value> child element of <header> element", "processing <header> elements");

                        // TODO: Handle @value
                        final String headerName = headerNameElement.getStringValue();
                        final String headerValue = headerValueElement.getStringValue();

                        headerNames.add(headerName);
                        headerNameValues.put(headerName, headerValue);
                    }
                }
            }

            // Extension attributes
            avtXXFormsUsername = submissionElement.attributeValue(XFormsConstants.XXFORMS_USERNAME_QNAME);
            avtXXFormsPassword = submissionElement.attributeValue(XFormsConstants.XXFORMS_PASSWORD_QNAME);

            avtXXFormsReadonly = submissionElement.attributeValue(XFormsConstants.XXFORMS_READONLY_ATTRIBUTE_QNAME);
            avtXXFormsShared = submissionElement.attributeValue(XFormsConstants.XXFORMS_SHARED_QNAME);

            avtXXFormsTarget = submissionElement.attributeValue(XFormsConstants.XXFORMS_TARGET_QNAME);
            xxfFormsEnsureUploads = !"false".equals(submissionElement.attributeValue(XFormsConstants.XXFORMS_ENSURE_UPLOADS_QNAME));
            avtXXFormsHandleXInclude = submissionElement.attributeValue(XFormsConstants.XXFORMS_XINCLUDE);

            // Whether we must show progress or not
            xxfShowProgress = !"false".equals(submissionElement.attributeValue(XFormsConstants.XXFORMS_SHOW_PROGRESS_QNAME));

            // Whether or not to rewrite URLs
            fURLNorewrite = XFormsUtils.resolveUrlNorewrite(submissionElement);

            // Remember that we did this
            submissionElementExtracted = true;
        }
    }

    private boolean isMethodOptimizedLocalSubmission() {
        return method.startsWith(XMLUtils.buildExplodedQName(XFormsConstants.XXFORMS_NAMESPACE_URI, ""))
                && (XFormsSubmissionUtils.isGet(method) || XFormsSubmissionUtils.isPost(method) || XFormsSubmissionUtils.isPut(method));
    }

    public String getId() {
        return id;
    }

    public String getEffectiveId() {
        return id;
    }

    public LocationData getLocationData() {
        return (LocationData) submissionElement.getData();
    }

    public XFormsEventHandlerContainer getParentContainer(XFormsContainingDocument containingDocument) {
        return model;
    }

    public List getEventHandlers(XFormsContainingDocument containingDocument) {
        return containingDocument.getStaticState().getEventHandlers(getEffectiveId());
    }

    public void performDefaultAction(PipelineContext pipelineContext, XFormsEvent event) {
        final String eventName = event.getEventName();

        if (XFormsEvents.XFORMS_SUBMIT.equals(eventName) || XFormsEvents.XXFORMS_SUBMIT.equals(eventName)) {
            // 11.1 The xforms-submit Event
            // Bubbles: Yes / Cancelable: Yes / Context Info: None

            containingDocument.setGotSubmission();

            boolean isDeferredSubmissionSecondPassReplaceAll = false;
            XFormsSubmitErrorEvent submitErrorEvent = null;
            XFormsSubmitDoneEvent submitDoneEvent = null;
            final long submissionStartTime = XFormsServer.logger.isDebugEnabled() ? System.currentTimeMillis() : 0;

            // Make sure submission element info is extracted
            extractSubmissionElement();

            try {
                final boolean isReplaceAll = replace.equals(XFormsConstants.XFORMS_SUBMIT_REPLACE_ALL);
                final boolean isReplaceInstance = replace.equals(XFormsConstants.XFORMS_SUBMIT_REPLACE_INSTANCE);
                final boolean isReplaceText = replace.equals(XFormsConstants.XFORMS_SUBMIT_REPLACE_TEXT);
                final boolean isReplaceNone = replace.equals(XFormsConstants.XFORMS_SUBMIT_REPLACE_NONE);

                final boolean isHandlingOptimizedGet = XFormsProperties.isOptimizeGetAllSubmission(containingDocument) && XFormsSubmissionUtils.isGet(method)
                        && isReplaceAll
                        && avtXXFormsUsername == null; // can't optimize if there are authentication credentials

                // Get current node for xforms:submission and instance containing the node to submit
                final NodeInfo currentNodeInfo;
                final XFormsInstance currentInstance;
                // Create context (should we simply reuse that of the model?)
                final XFormsContextStack contextStack = new XFormsContextStack(model);
                final XFormsFunction.Context functionContext;
                {
                    contextStack.setBinding(pipelineContext, XFormsModelSubmission.this);

                    currentNodeInfo = contextStack.getCurrentSingleNode();
                    functionContext = contextStack.getFunctionContext();

                    // Check that we have a current node and that it is pointing to a document or an element
                    if (currentNodeInfo == null)
                        throw new XFormsSubmissionException("Empty single-node binding on xforms:submission for submission id: " + id, "getting submission single-node binding",
                        		 XFormsSubmitErrorEvent.ErrorType.NO_DATA);

                    if (!(currentNodeInfo instanceof DocumentInfo || currentNodeInfo.getNodeKind() == org.w3c.dom.Document.ELEMENT_NODE)) {
                        throw new XFormsSubmissionException("xforms:submission: single-node binding must refer to a document node or an element.", "getting submission single-node binding",
                        		XFormsSubmitErrorEvent.ErrorType.NO_DATA);
                    }

                    // Current instance may be null if the document submitted is not part of an instance
                    currentInstance = contextStack.getCurrentInstance();
                }

                // Determine if the instance to submit has one or more bound and relevant upload controls
                //
                // o we don't check if we are currently initializing the document because at that point the
                //   client cannot have any files to upload yet
                //
                // o we don't check if we have already processed the second pass of a submission during this
                //   request, because it means that upload controls have been already committed
                //
                // o we don't check if we are requested not to with an attribute
                //
                // o we only check for replace="instance|none" and if serialization must take place

                boolean hasBoundRelevantUploadControl = false;
                if (currentInstance!= null && !containingDocument.isInitializing() && !containingDocument.isGotSubmissionSecondPass() && xxfFormsEnsureUploads && !isReplaceAll && serialize) {
                    final XFormsControls xformsControls = containingDocument.getXFormsControls();
                    final List uploadControls = xformsControls.getCurrentControlsState().getUploadControls();
                    if (uploadControls != null) {
                        for (Iterator i = uploadControls.iterator(); i.hasNext();) {
                            final XFormsUploadControl currentControl = (XFormsUploadControl) i.next();
                            if (currentControl.isRelevant()) {
                                final NodeInfo boundNodeInfo = currentControl.getBoundNode();
                                if (currentInstance == currentInstance.getModel(containingDocument).getInstanceForNode(boundNodeInfo)) {
                                    // Found one relevant upload control bound to the instance we are submitting
                                    hasBoundRelevantUploadControl = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                final boolean isDeferredSubmission = (isReplaceAll && !isHandlingOptimizedGet) || (!isReplaceAll && serialize && hasBoundRelevantUploadControl);
                final boolean isDeferredSubmissionFirstPass = isDeferredSubmission && XFormsEvents.XFORMS_SUBMIT.equals(eventName);
                final boolean isDeferredSubmissionSecondPass = isDeferredSubmission && !isDeferredSubmissionFirstPass; // here we get XXFORMS_SUBMIT
                isDeferredSubmissionSecondPassReplaceAll = isDeferredSubmissionSecondPass && isReplaceAll;

                if (isDeferredSubmissionSecondPass)
                    containingDocument.setGotSubmissionSecondPass();

                // If a submission requiring a second pass was already set, then we ignore a subsequent submission but
                // issue a warning
                {
                    final XFormsModelSubmission existingSubmission = containingDocument.getClientActiveSubmission();
                    if (isDeferredSubmission && existingSubmission != null) {
                        logger.warn("XForms - submission - another submission requiring a second pass already exists (" + existingSubmission.getEffectiveId() + "). Ignoring new submission (" + this.getEffectiveId() + ").");
                        return;
                    }
                }

                // "The data model is updated"
                final XFormsModel modelForInstance;
                if (currentInstance != null) {
                    modelForInstance = currentInstance.getModel(containingDocument);
                    {
                        // NOTE: XForms 1.1 seems to say this should happen regardless of whether we serialize or not. If
                        // the instance is not serialized and if no instance data is otherwise used for the submission,
                        // this seems however unneeded.

                        // TODO: XForms 1.1 says that we should rebuild/recalculate the "model containing this submission".

                        final XFormsModel.DeferredActionContext deferredActionContext = modelForInstance.getDeferredActionContext();
                        if (deferredActionContext != null) {
                            if (deferredActionContext.rebuild) {
                                modelForInstance.doRebuild(pipelineContext);
                            }
                            if (deferredActionContext.recalculate) {
                                modelForInstance.doRecalculate(pipelineContext);
                            }
                        }
                    }
                } else {
                    // Case where no instance was found
                    modelForInstance = null;
                }

                final Document initialDocumentToSubmit;
                if (serialize && !isDeferredSubmissionSecondPass) {
                    initialDocumentToSubmit = createDocumentToSubmit(pipelineContext, currentNodeInfo, currentInstance, modelForInstance);
                } else {
                    initialDocumentToSubmit = null;
                }

                final Map prefixToURIMap = containingDocument.getStaticState().getNamespaceMappings(getEffectiveId());

                // Deferred submission: end of the first pass
                if (isDeferredSubmissionFirstPass) {

                    // Resolve the target AVT if needed
                    final FunctionLibrary functionLibrary = XFormsContainingDocument.getFunctionLibrary();
                    resolvedXXFormsTarget = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtXXFormsTarget);

                    // When replace="all", we wait for the submission of an XXFormsSubmissionEvent from the client
                    containingDocument.setClientActiveSubmission(this);
                    return;
                }

                // Evaluate AVTs
                {
                    final FunctionLibrary functionLibrary = XFormsContainingDocument.getFunctionLibrary();

                    final String tempActionOrResource = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtActionOrResource);
                    resolvedActionOrResource = XFormsUtils.encodeHRRI(tempActionOrResource, true);

                    resolvedXXFormsUsername = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtXXFormsUsername);
                    resolvedXXFormsPassword = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtXXFormsPassword);
                    resolvedXXFormsReadonly = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtXXFormsReadonly);
                    resolvedXXFormsShared = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtXXFormsShared);

                    // Default is "false" for security reasons
                    final String tempHandleXInclude = XFormsUtils.resolveAttributeValueTemplates(pipelineContext, currentNodeInfo, contextStack.getCurrentVariables(), functionLibrary, functionContext, prefixToURIMap, getLocationData(), avtXXFormsHandleXInclude);
                    resolvedXXFormsHandleXInclude = "true".equals(tempHandleXInclude);
                }

                // Check read-only and shared hints
                XFormsInstance.checkSharedHints(submissionElement, resolvedXXFormsReadonly, resolvedXXFormsShared);
                final boolean isReadonlyHint = "true".equals(resolvedXXFormsReadonly);
                final boolean isApplicationSharedHint = "application".equals(resolvedXXFormsShared);
                final long timeToLive = XFormsInstance.getTimeToLive(submissionElement);
                if (isApplicationSharedHint) {
                    if (!XFormsSubmissionUtils.isGet(method))
                        throw new XFormsSubmissionException("xforms:submission: xxforms:shared=\"application\" can be set only with method=\"get\".",
                                "checking read-only and shared hints");
                    if (!isReplaceInstance)
                        throw new XFormsSubmissionException("xforms:submission: xxforms:shared=\"application\" can be set only with replace=\"instance\".",
                                "checking read-only and shared hints");
                } else if (isReadonlyHint) {
                    if (!isReplaceInstance)
                        throw new XFormsSubmissionException("xforms:submission: xxforms:readonly=\"true\" can be \"true\" only with replace=\"instance\".",
                                "checking read-only and shared hints");
                }

                final Document documentToSubmit;
                if (serialize && isDeferredSubmissionSecondPass) {
                    // Handle uploaded files if any
                    final Element filesElement = (event instanceof XXFormsSubmitEvent) ? ((XXFormsSubmitEvent) event).getFilesElement() : null;
                    if (filesElement != null) {
                        for (Iterator i = filesElement.elements().iterator(); i.hasNext();) {
                            final Element parameterElement = (Element) i.next();
                            final String name = parameterElement.element("name").getTextTrim();

                            final XFormsUploadControl uploadControl
                                        = (XFormsUploadControl) containingDocument.getObjectById(name);

                            // In case of xforms:repeat, the name of the template will not match an existing control
                            if (uploadControl == null)
                                continue;

                            final Element valueElement = parameterElement.element("value");
                            final String value = valueElement.getTextTrim();

                            final String filename;
                            {
                                final Element filenameElement = parameterElement.element("filename");
                                filename = (filenameElement != null) ? filenameElement.getTextTrim() : "";
                            }
                            final String mediatype;
                            {
                                final Element mediatypeElement = parameterElement.element("content-type");
                                mediatype = (mediatypeElement != null) ? mediatypeElement.getTextTrim() : "";
                            }
                            final String size = parameterElement.element("content-length").getTextTrim();

                            if (size.equals("0") && filename.equals("")) {
                                // No file was selected in the UI
                            } else {
                                // A file was selected in the UI (note that the file may be empty)
                                final String paramValueType = Dom4jUtils.qNameToexplodedQName(Dom4jUtils.extractAttributeValueQName(valueElement, XMLConstants.XSI_TYPE_QNAME));

                                // Set value of uploaded file into the instance (will be xs:anyURI or xs:base64Binary)
                                uploadControl.setExternalValue(pipelineContext, value, paramValueType, !isReplaceAll);

                                // Handle filename, mediatype and size if necessary
                                uploadControl.setFilename(pipelineContext, filename);
                                uploadControl.setMediatype(pipelineContext, mediatype);
                                uploadControl.setSize(pipelineContext, size);
                            }
                        }
                    }

                    // Create document to submit
                    documentToSubmit = createDocumentToSubmit(pipelineContext, currentNodeInfo, currentInstance, modelForInstance);

                } else {
                    // Don't recreate document
                    documentToSubmit = initialDocumentToSubmit;
                }

                if (serialize && !isDeferredSubmissionSecondPassReplaceAll) { // we don't want any changes to happen to the document upon xxforms-submit when producing a new document
                    // Fire xforms-submit-serialize

                    // "The event xforms-submit-serialize is dispatched. If the submission-body property of the event
                    // is changed from the initial value of empty string, then the content of the submission-body
                    // property string is used as the submission serialization. Otherwise, the submission serialization
                    // consists of a serialization of the selected instance data according to the rules stated at 11.9
                    // Submission Options."

                    // TODO: pass submission-body attribute
                    containingDocument.dispatchEvent(pipelineContext, new XFormsSubmitSerializeEvent(XFormsModelSubmission.this));
                    // TODO: look at result of submission-body attribute and see if submission body needs to be replaced
                }

                // Serialize
                // To support: application/xml, application/x-www-form-urlencoded, multipart/related, multipart/form-data
                final byte[] messageBody;
                final String queryString;
                final String defaultMediatypeForSerialization;
                {
                    if (method.equals("multipart-post")) {

                        // Set default serialization based on method
                        if (serialization == null)
                            serialization = "multipart/related";

                        // TODO
                        throw new XFormsSubmissionException("xforms:submission: submission method not yet implemented: " + method, "serializing instance");
                    } else if (method.equals("form-data-post")) {

                        // Set default serialization based on method
                        if (serialization == null)
                            serialization = "multipart/form-data";

                        // TODO

//                        final MultipartFormDataBuilder builder = new MultipartFormDataBuilder(, , null);

                        throw new XFormsSubmissionException("xforms:submission: submission method not yet implemented: " + method, "serializing instance");
                    } else if (method.equals("urlencoded-post")) {

                        // Set default serialization based on method
                        if (serialization == null)
                            serialization = "application/x-www-form-urlencoded";

                        // Perform "application/x-www-form-urlencoded" serialization
                        queryString = null;
                        messageBody = serialize? createWwwFormUrlEncoded(documentToSubmit).getBytes("UTF-8") : null;// the resulting string is already ASCII
                        defaultMediatypeForSerialization = "application/x-www-form-urlencoded";

                    } else if (XFormsSubmissionUtils.isPost(method) || XFormsSubmissionUtils.isPut(method)) {

                        // Set default serialization based on method
                        if (serialization == null)
                            serialization = "application/xml";

                        if (serialize) {

                            if (XMLUtils.isXMLMediatype(serialization)) {
                                // XML serialization

                                // Serialize XML to a stream of bytes
                                try {
                                    final Transformer identity = TransformerUtils.getIdentityTransformer();
                                    TransformerUtils.applyOutputProperties(identity,
                                            "xml", version, null, null, encoding, omitxmldeclaration, standalone, indent, 4);

                                    // TODO: use cdata-section-elements

                                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                                    identity.transform(new DocumentSource(documentToSubmit), new StreamResult(os));
                                    messageBody = os.toByteArray();
                                } catch (Exception e) {
                                    throw new XFormsSubmissionException(e, "xforms:submission: exception while serializing instance to XML.", "serializing instance");
                                }
                                defaultMediatypeForSerialization = "application/xml";
                            } else if (XMLUtils.isTextContentType(serialization)) {
                                // Text serialization
                                // TODO
                                throw new XFormsSubmissionException("xforms:submission: text serialization is not yet implemented.", "serializing instance");
                            } else {
                                // Binary serialization

                                final String nodeType = InstanceData.getType(documentToSubmit.getRootElement());

                                if (XMLConstants.XS_ANYURI_EXPLODED_QNAME.equals(nodeType)) {
                                    // Interpret node as anyURI
                                    // TODO: PERFORMANCE: Must pass InputStream all the way to the submission instead of storing into byte[] in memory!
                                    final String uri = documentToSubmit.getRootElement().getStringValue();
                                    messageBody = NetUtils.uriToByteArray(uri);
                                } else if (XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME.equals(nodeType)) {
                                    // TODO
                                    throw new XFormsSubmissionException("xforms:submission: binary serialization with base64Binary type is not yet implemented.", "serializing instance");
                                } else {
                                    // TODO
                                    throw new XFormsSubmissionException("xforms:submission: binary serialization without a type is not yet implemented.", "serializing instance");
                                }
                                defaultMediatypeForSerialization = "application/octet-stream";
                            }
                        } else {
                            messageBody = null;
                            defaultMediatypeForSerialization = null;
                        }
                        queryString = null;

                    } else if (XFormsSubmissionUtils.isGet(method) || XFormsSubmissionUtils.isDelete(method)) {

                        // Set default serialization based on method
                        if (serialization == null)
                            serialization = "application/x-www-form-urlencoded";

                        // Perform "application/x-www-form-urlencoded" serialization
                        queryString = serialize ? createWwwFormUrlEncoded(documentToSubmit) : null;
                        messageBody = null;
                        defaultMediatypeForSerialization = null;

                    } else {
                        throw new XFormsSubmissionException("xforms:submission: invalid submission method requested: " + method, "serializing instance");
                    }
                }

                final ExternalContext externalContext = (ExternalContext) pipelineContext.getAttribute(PipelineContext.EXTERNAL_CONTEXT);

                // Get URL type
                final String urlType = submissionElement.attributeValue(XMLConstants.FORMATTING_URL_TYPE_QNAME);
                final ExternalContext.Request request = externalContext.getRequest();

                // Find instance to update
                final XFormsInstance replaceInstance;
                if (isReplaceInstance) {
                    if (xxfReplaceInstanceId != null)
                        replaceInstance = containingDocument.findInstance(xxfReplaceInstanceId);
                    else if (replaceInstanceId != null)
                        replaceInstance = model.getInstance(replaceInstanceId);
                    else if (currentInstance == null)
                        replaceInstance = model.getDefaultInstance();
                    else
                        replaceInstance = currentInstance;
                } else {
                    replaceInstance = null;
                }

                // Result information
                ConnectionResult connectionResult = null;
                final long externalSubmissionStartTime = XFormsServer.logger.isDebugEnabled() ? System.currentTimeMillis() : 0;
                try {
                    if ((isReplaceInstance || isReplaceNone) && resolvedActionOrResource.startsWith("test:")) {
                        // Test action

                        if (messageBody == null)
                            throw new XFormsSubmissionException("Action 'test:': no message body.", "processing submission response");

                        connectionResult = new ConnectionResult(null);
                        connectionResult.statusCode = 200;
                        connectionResult.responseHeaders = new HashMap();
                        connectionResult.lastModified = 0;
                        connectionResult.responseMediaType = "application/xml";
                        connectionResult.dontHandleResponse = false;
                        connectionResult.setResponseInputStream(new ByteArrayInputStream(messageBody));

                    } else if (isHandlingOptimizedGet) {
                        // GET with replace="all": we can optimize and tell the client to just load the URL

                        final String actionString = (queryString == null) ? resolvedActionOrResource : resolvedActionOrResource + ((resolvedActionOrResource.indexOf('?') == -1) ? "?" : "") + queryString;
                        final String resultURL = XFormsLoadAction.resolveLoadValue(containingDocument, pipelineContext, submissionElement, true, actionString, null, null, fURLNorewrite, xxfShowProgress);
                        connectionResult = new ConnectionResult(resultURL);
                        connectionResult.dontHandleResponse = true;

                    } else if (!NetUtils.urlHasProtocol(resolvedActionOrResource)
                               && !fURLNorewrite
                               && headerNames == null
                               && ((request.getContainerType().equals("portlet") && !"resource".equals(urlType))
                                    || (request.getContainerType().equals("servlet")
                                        && (XFormsProperties.isOptimizeLocalSubmission(containingDocument) || isMethodOptimizedLocalSubmission())
                                        &&  isReplaceAll))) {

                        // This is an "optimized" submission, i.e. one that does not use an actual
                        // protocol handler to access the resource

                        // NOTE: Optimizing with include() for servlets doesn't allow detecting
                        // errors caused by the included resource, so we don't allow this for now.

                        // NOTE: For portlets, paths are served directly by the portlet, NOT as
                        // resources.

                        // Current limitations:
                        // o Portlets cannot access resources outside the portlet except by using absolute URLs (unless f:url-type="resource")
                        // o Servlets cannot access resources on the same server but not in the current application
                        //   except by using absolute URLs

                        final URI resolvedURI = XFormsUtils.resolveXMLBase(submissionElement, resolvedActionOrResource);

                        // NOTE: We don't want any changes to happen to the document upon xxforms-submit when producing
                        // a new document so we don't dispatch xforms-submit-done and pass a null XFormsModelSubmission
                        // in that case
                        connectionResult = XFormsSubmissionUtils.doOptimized(pipelineContext, externalContext,
                                isDeferredSubmissionSecondPassReplaceAll ? null : this, method, resolvedURI.toString(), (mediatype == null) ? defaultMediatypeForSerialization : mediatype, isReplaceAll,
                                messageBody, queryString);

                    } else {
                        // This is a regular remote submission going through a protocol handler

                        // Absolute URLs or absolute paths are allowed to a local servlet
                        String resolvedURL;

                        if (NetUtils.urlHasProtocol(resolvedActionOrResource) || fURLNorewrite) {
                            // Don't touch the URL if it is absolute or if f:url-norewrite="true"
                            resolvedURL = resolvedActionOrResource;
                        } else {
                            // Rewrite URL
                            resolvedURL = XFormsUtils.resolveResourceURL(pipelineContext, submissionElement, resolvedActionOrResource, false);

                            if (request.getContainerType().equals("portlet") && "resource".equals(urlType) && !NetUtils.urlHasProtocol(resolvedURL)) {
                                // In this case, we have to prepend the complete server path
                                resolvedURL = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() > 0 ? ":" + request.getServerPort() : "") + resolvedURL;
                            }
                        }

                        if (isApplicationSharedHint) {
                            // Get the instance from shared instance cache
                            // This can only happen is method="get" and replace="instance" and xxforms:readonly="true" and xxforms:shared="application"

                            if (XFormsServer.logger.isDebugEnabled())
                                containingDocument.logDebug("submission", "using instance from application shared instance cache",
                                        new String[] { "instance", replaceInstance.getEffectiveId() });

                            final URL absoluteResolvedURL = XFormsSubmissionUtils.createAbsoluteURL(resolvedURL, queryString, externalContext);
                            final String absoluteResolvedURLString = absoluteResolvedURL.toExternalForm();

                            final SharedXFormsInstance sharedInstance
                                    = XFormsServerSharedInstancesCache.instance().find(pipelineContext, containingDocument, replaceInstance.getEffectiveId(), replaceInstance.getModelId(), absoluteResolvedURLString, timeToLive, replaceInstance.getValidation());

                            if (XFormsServer.logger.isDebugEnabled())
                                containingDocument.logDebug("submission", "replacing instance with read-only instance",
                                        new String[] { "instance", sharedInstance.getEffectiveId() });

                            // Handle new instance and associated events
                            final XFormsModel replaceModel = sharedInstance.getModel(containingDocument);
                            replaceModel.handleNewInstanceDocuments(pipelineContext, sharedInstance);

                            connectionResult = null;
                            submitDoneEvent = new XFormsSubmitDoneEvent(XFormsModelSubmission.this, absoluteResolvedURLString, 200);
                        } else {
                            // Perform actual submission
                            connectionResult = XFormsSubmissionUtils.doRegular(externalContext, containingDocument,
                                    method, resolvedURL, resolvedXXFormsUsername, resolvedXXFormsPassword, (mediatype == null) ? defaultMediatypeForSerialization : mediatype,
                                    messageBody, queryString, headerNames, headerNameValues);
                        }
                    }

                    if (connectionResult != null && !connectionResult.dontHandleResponse) {
                        // Handle response
                        if (connectionResult.statusCode >= 200 && connectionResult.statusCode < 300) {// accept any success code (in particular "201 Resource Created")
                            // Sucessful response
                            if (connectionResult.hasContent()) {
                                // There is a body

                                if (isReplaceAll) {
                                    // When we get here, we are in a mode where we need to send the reply
                                    // directly to an external context, if any.

                                    // "the event xforms-submit-done is dispatched"
                                    if (!isDeferredSubmissionSecondPassReplaceAll) // we don't want any changes to happen to the document upon xxforms-submit when producing a new document
                                        containingDocument.dispatchEvent(pipelineContext, new XFormsSubmitDoneEvent(XFormsModelSubmission.this, connectionResult));

                                    final ExternalContext.Response response = externalContext.getResponse();

                                    // Forward headers to response
                                    connectionResult.forwardHeaders(response);

                                    // Forward content to response
                                    NetUtils.copyStream(connectionResult.getResponseInputStream(), response.getOutputStream());

                                    // TODO: [#306918] RFE: Must be able to do replace="all" during initialization.
                                    // http://forge.objectweb.org/tracker/index.php?func=detail&aid=306918&group_id=168&atid=350207
                                    // Suggestion is to write either binary or XML to processor output ContentHandler,
                                    // and make sure the code which would output the XHTML+XForms is disabled.

                                } else if (isReplaceInstance) {

                                    if (XMLUtils.isXMLMediatype(connectionResult.responseMediaType)) {
                                        // Handling of XML media type
                                        // Set new instance document to replace the one submitted

                                        if (replaceInstance == null) {
                                            // Replacement instance was specified but not found TODO: XForms 1.1 won't
                                            // dispatch xforms-binding-exception here Not sure what's the right thing to
                                            // do with 1.1, but this could be done as part of the model's static
                                            // analysis if the instance value is not obtained through AVT, and
                                            // dynamically otherwise. However, in the dynamic case, I think that this
                                            // should be a (currently non-specified by XForms) xforms-binding-error.
                                            containingDocument.dispatchEvent(pipelineContext, new XFormsBindingExceptionEvent(XFormsModelSubmission.this));
                                        } else {
                                            final XFormsInstance newInstance;
                                            try {
                                                // Read stream into Document
                                                if (!isReadonlyHint) {
                                                    // Resulting instance is not read-only

                                                    if (XFormsServer.logger.isDebugEnabled())
                                                        containingDocument.logDebug("submission", "replacing instance with mutable instance",
                                                            new String[] { "instance", replaceInstance.getEffectiveId() });

                                                    // TODO: What about configuring validation? And what default to choose?
                                                    final Document resultingInstanceDocument
                                                            = TransformerUtils.readDom4j(connectionResult.getResponseInputStream(), connectionResult.resourceURI, resolvedXXFormsHandleXInclude);
                                                    newInstance = new XFormsInstance(replaceInstance.getModelId(), replaceInstance.getEffectiveId(), resultingInstanceDocument,
                                                            connectionResult.resourceURI, resolvedXXFormsUsername, resolvedXXFormsPassword, false, -1, replaceInstance.getValidation());
                                                } else {
                                                    // Resulting instance is read-only

                                                    if (XFormsServer.logger.isDebugEnabled())
                                                        containingDocument.logDebug("submission", "replacing instance with read-only instance",
                                                            new String[] { "instance", replaceInstance.getEffectiveId() });

                                                    // TODO: What about configuring validation? And what default to choose?
                                                    // NOTE: isApplicationSharedHint is always false when get get here. isApplicationSharedHint="true" is handled above.
                                                    final DocumentInfo resultingInstanceDocument = TransformerUtils.readTinyTree(connectionResult.getResponseInputStream(), connectionResult.resourceURI, resolvedXXFormsHandleXInclude);
                                                    newInstance = new SharedXFormsInstance(replaceInstance.getModelId(), replaceInstance.getEffectiveId(), resultingInstanceDocument,
                                                            connectionResult.resourceURI, resolvedXXFormsUsername, resolvedXXFormsPassword, false, -1, replaceInstance.getValidation());
                                                }    
                                            } catch (Exception e) {
                                                submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.PARSE_ERROR);
                                                throw new XFormsSubmissionException(e, "xforms:submission: exception while serializing XML to instance.", "processing instance replacement");
                                            }

                                            // Handle new instance and associated events
                                            final XFormsModel replaceModel = newInstance.getModel(containingDocument);
                                            replaceModel.handleNewInstanceDocuments(pipelineContext, newInstance);

                                            // Notify that submission is done
                                            submitDoneEvent = new XFormsSubmitDoneEvent(XFormsModelSubmission.this, connectionResult);
                                        }
                                    } else {
                                        // Other media type
                                        submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.RESOURCE_ERROR);
                                        throw new XFormsSubmissionException("Body received with non-XML media type for replace=\"instance\": " + connectionResult.responseMediaType, "processing instance replacement");
                                    }
                                } else if (isReplaceText) {

                                    // XForms 1.1: "If the replace attribute contains the value "text" and the
                                    // submission response conforms to an XML mediatype (as defined by the content type
                                    // specifiers in [RFC 3023]) or a text media type (as defined by a content type
                                    // specifier of text/*), then the response data is encoded as text and replaces the
                                    // content of the replacement target node."

                                    // Get response body
                                    String responseBody = "";
                                    if (XMLUtils.isTextContentType(connectionResult.responseMediaType)) {
                                        // Text mediatype (including text/xml), read stream into String
                                        try {
                                            final String charset = NetUtils.getTextCharsetFromContentType(connectionResult.responseMediaType);
                                            final Reader reader = new InputStreamReader(connectionResult.responseInputStream, charset);
                                            try {
                                                responseBody = NetUtils.readStreamAsString(reader);
                                            } finally {
                                                try {
                                                    reader.close();
                                                } catch (Exception e) {
                                                }
                                            }
                                        } catch (Exception e) {
                                            XFormsServer.logger.error("XForms - submission - error while reading response body ", e);
                                        }
                                    } else if (XMLUtils.isXMLMediatype(connectionResult.responseMediaType)) {
                                        // XML mediatype other than text/xml

                                        // TODO: What should we do if the response Content-Type includes a charset parameter?
                                        final Reader reader = XMLUtils.getReaderFromXMLInputStream(connectionResult.resourceURI, connectionResult.responseInputStream);
                                        try {
                                            responseBody = NetUtils.readStreamAsString(reader);
                                        } finally {
                                            try {
                                                reader.close();
                                            } catch (Exception e) {
                                            }
                                        }
                                    } else {
                                        // This is a binary result

                                        // Don't store anything for now as per the spec, but we could do something better by going beyond the spec
                                        // NetUtils.inputStreamToAnyURI(pipelineContext, connectionResult.resultInputStream, NetUtils.SESSION_SCOPE);

                                        // XForms 1.1: "For a success response including a body that is both a non-XML
                                        // media type (i.e. with a content type not matching any of the specifiers in
                                        // [RFC 3023]) and a non-text type (i.e. with a content type not matching
                                        // text/*), when the value of the replace attribute on element submission is
                                        // "text", nothing in the document is replaced and submission processing
                                        // concludes after dispatching xforms-submit-error with appropriate context
                                        // information, including an error-type of resource-error."
                                        submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.RESOURCE_ERROR);
                                        throw new XFormsSubmissionException("Mediatype is neither text nor XML for replace=\"text\": " + connectionResult.responseMediaType, "reading response body");
                                    }

                                    // Find target location
                                    final NodeInfo destinationNodeInfo;
                                    if (target != null) {
                                        // Evaluate destination node
                                        final FunctionLibrary functionLibrary = XFormsContainingDocument.getFunctionLibrary();
                                        final Object destinationObject
                                                = XPathCache.evaluateSingle(pipelineContext, currentNodeInfo, target, prefixToURIMap,
                                                contextStack.getCurrentVariables(), functionLibrary, functionContext, null, getLocationData());

                                        if (destinationObject instanceof NodeInfo) {
                                            destinationNodeInfo = (NodeInfo) destinationObject;
                                            if (destinationNodeInfo.getNodeKind() != org.w3c.dom.Document.ELEMENT_NODE && destinationNodeInfo.getNodeKind() != org.w3c.dom.Document.ATTRIBUTE_NODE) {
                                                // Throw target-error

                                                // XForms 1.1: "If the processing of the target attribute fails, then
                                                // submission processing ends after dispatching the event
                                                // xforms-submit-error with an error-type of target-error."
                                                submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.TARGET_ERROR);
                                                throw new XFormsSubmissionException("target attribute doesn't point to an element or attribute for replace=\"text\".", "processing target attribute");
                                            }
                                        } else {
                                            // Throw target-error

                                            // XForms 1.1: "If the processing of the target attribute fails, then
                                            // submission processing ends after dispatching the event
                                            // xforms-submit-error with an error-type of target-error."
                                            submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.TARGET_ERROR);
                                            throw new XFormsSubmissionException("target attribute doesn't point to a node for replace=\"text\".", "processing target attribute");
                                        }
                                    } else {
                                        // Handle default destination
                                        destinationNodeInfo = replaceInstance.getInstanceRootElementInfo();
                                    }

                                    // Set value into the instance
                                    XFormsSetvalueAction.doSetValue(pipelineContext, containingDocument, this, destinationNodeInfo, responseBody, null, false);

                                    // Notify that processing is terminated
                                    submitDoneEvent = new XFormsSubmitDoneEvent(XFormsModelSubmission.this, connectionResult);

                                } else if (isReplaceNone) {
                                    // Just notify that processing is terminated
                                    submitDoneEvent = new XFormsSubmitDoneEvent(XFormsModelSubmission.this, connectionResult);
                                } else {
                                    submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.XXFORMS_INTERNAL_ERROR);
                                    throw new XFormsSubmissionException("xforms:submission: invalid replace attribute: " + replace, "processing instance replacement");
                                }

                            } else {
                                // There is no body, notify that processing is terminated

                                if (isReplaceInstance) {
                                    // XForms 1.1 says it is fine not to have a body, but in most cases you will want
                                    // to know that no instance replacement took place
                                    XFormsServer.logger.warn("XForms - submission - instance replacement did not take place upon successful response because no body was provided. Submission: "
                                            + getEffectiveId());
                                }

                                submitDoneEvent = new XFormsSubmitDoneEvent(XFormsModelSubmission.this, connectionResult);
                            }
                        } else if (connectionResult.statusCode == 302 || connectionResult.statusCode == 301) {
                            // Got a redirect

                            final ExternalContext.Response response = externalContext.getResponse();

                            // Forward headers to response
                            connectionResult.forwardHeaders(response);

                            // Forward redirect
                            response.setStatus(connectionResult.statusCode);

                        } else {
                            // Error code received
                            submitErrorEvent = createErrorEvent(pipelineContext, connectionResult, XFormsSubmitErrorEvent.ErrorType.RESOURCE_ERROR);
                            throw new XFormsSubmissionException("xforms:submission for submission id: " + id + ", error code received when submitting instance: " + connectionResult.statusCode, "processing submission response");
                        }
                    }
                } finally {
                    // Clean-up
                    if (connectionResult != null) {
                        connectionResult.close();
                    }
                    // Log time spent in submission if needed
                    if (XFormsServer.logger.isDebugEnabled()) {
                        final long submissionTime = System.currentTimeMillis() - externalSubmissionStartTime;
                        containingDocument.logDebug("submission", "external submission time including handling returned body",
                            new String[] { "time", Long.toString(submissionTime) });
                    }
                }
            } catch (Throwable e) {
                if (isDeferredSubmissionSecondPassReplaceAll && XFormsProperties.isOptimizeLocalSubmission(containingDocument)) {
                    // It doesn't serve any purpose here to dispatch an event, so we just propagate the exception
                    throw new XFormsSubmissionException(e, "Error while processing xforms:submission", "processing submission");
                } else {
                    // Any exception will cause an error event to be dispatched
                    if (submitErrorEvent == null)
                        submitErrorEvent = new XFormsSubmitErrorEvent(XFormsModelSubmission.this, resolvedActionOrResource,
                                (e instanceof XFormsSubmissionException)
                                        ? ((XFormsSubmissionException) e).getErrorType()
                                        : XFormsSubmitErrorEvent.ErrorType.XXFORMS_INTERNAL_ERROR, 0);
                    
                    submitErrorEvent.setThrowable(e);
                    containingDocument.dispatchEvent(pipelineContext, submitErrorEvent);
                }
            } finally {
                // If submission succeeded, dispatch success event
                if (submitDoneEvent != null && !isDeferredSubmissionSecondPassReplaceAll) { // we don't want any changes to happen to the document upon xxforms-submit when producing a new document
                    containingDocument.dispatchEvent(pipelineContext, submitDoneEvent);
                }
                // Log total time spent in submission if needed
                if (XFormsServer.logger.isDebugEnabled()) {
                    final long submissionTime = System.currentTimeMillis() - submissionStartTime;
                    containingDocument.logDebug("submission", "total submission time",
                        new String[] { "time", Long.toString(submissionTime) });
                }
            }

        } else if (XFormsEvents.XFORMS_BINDING_EXCEPTION.equals(eventName)) {
            // The default action for this event results in the following: Fatal error.
            throw new ValidationException("Binding exception for target: " + event.getTargetObject().getEffectiveId(), event.getTargetObject().getLocationData());
        }
    }

    private Document createDocumentToSubmit(PipelineContext pipelineContext, NodeInfo currentNodeInfo, XFormsInstance currentInstance, XFormsModel modelForInstance) {
        final Document documentToSubmit;

        // Revalidate instance
        // NOTE: We need to do this here so that bind/@type works correctly. XForms 1.1 seems to say that this
        // must be done after pruning, but then it is not clear how XML Schema validation would work then.
        if (modelForInstance != null)
            modelForInstance.doRevalidate(pipelineContext);

        // Get selected nodes (re-root and prune)
        documentToSubmit = reRootAndPrune(currentNodeInfo);

        // Check that there are no validation errors
        // NOTE: If the instance is read-only, it can't have MIPs at the moment, and can't fail validation/requiredness, so we don't go through the process at all.
        final boolean instanceSatisfiesValidRequired
                = (currentInstance != null && currentInstance.isReadOnly())
                || !validate
                || XFormsSubmissionUtils.isSatisfiesValidRequired(containingDocument, documentToSubmit, true, true, true);
        if (!instanceSatisfiesValidRequired) {
            if (logger.isDebugEnabled()) {
                final String documentString = TransformerUtils.tinyTreeToString(currentNodeInfo);
                containingDocument.logDebug("submission", "instance document or subset thereof cannot be submitted",
                        new String[] { "document", documentString });
            }
            throw new XFormsSubmissionException("xforms:submission: instance to submit does not satisfy valid and/or required model item properties.",
                    "checking instance validity", XFormsSubmitErrorEvent.ErrorType.VALIDATION_ERROR);
        }

        return documentToSubmit;
    }

    private Document reRootAndPrune(final NodeInfo currentNodeInfo) {

        final Document documentToSubmit;
        if (currentNodeInfo instanceof NodeWrapper) {
            final Node currentNode = (Node) ((NodeWrapper) currentNodeInfo).getUnderlyingNode();

            // "A node from the instance data is selected, based on attributes on the submission
            // element. The indicated node and all nodes for which it is an ancestor are considered for
            // the remainder of the submit process. "
            if (currentNode instanceof Element) {
                // Create subset of document
                documentToSubmit = Dom4jUtils.createDocument((Element) currentNode);
            } else {
                // Use entire instance document
                documentToSubmit = Dom4jUtils.createDocument(currentNode.getDocument().getRootElement());
            }

            if (relevant) {
                // "Any node which is considered not relevant as defined in 6.1.4 is removed."
                final Node[] nodeToDetach = new Node[1];
                do {
                    // NOTE: This is not very efficient, but at least we avoid NPEs that we would get by
                    // detaching elements within accept(). Should implement a more efficient algorithm to
                    // prune non-relevant nodes.
                    nodeToDetach[0] = null;
                    documentToSubmit.accept(new VisitorSupport() {

                        public final void visit(Element element) {
                            checkInstanceData(element);
                        }

                        public final void visit(Attribute attribute) {
                            checkInstanceData(attribute);
                        }

                        private final void checkInstanceData(Node node) {
                            if (nodeToDetach[0] == null) {
                                // Check "relevant" MIP and remove non-relevant nodes
                                    if (!InstanceData.getInheritedRelevant(node))
                                        nodeToDetach[0] = node;
                            }
                        }
                    });
                    if (nodeToDetach[0] != null)
                        nodeToDetach[0].detach();

                } while (nodeToDetach[0] != null);
            }

            // TODO: handle includenamespaceprefixes
        } else {
            // Submitting read-only instance backed by TinyTree (no MIPs to check)
            if (currentNodeInfo.getNodeKind() == org.w3c.dom.Document.ELEMENT_NODE) {
                documentToSubmit = TransformerUtils.tinyTreeToDom4j2(currentNodeInfo);
            } else {
                documentToSubmit = TransformerUtils.tinyTreeToDom4j2(currentNodeInfo.getRoot());
            }
        }
        return documentToSubmit;
    }

    private String createWwwFormUrlEncoded(final Document document) {

        final StringBuffer sb = new StringBuffer();
        document.accept(new VisitorSupport() {
            public final void visit(Element element) {
                // We only care about elements

                final List children = element.elements();
                if (children == null || children.size() == 0) {
                    // Only consider leaves
                    final String text = element.getText();
                    if (text != null && text.length() > 0) {
                        // Got one!
                        final String localName = element.getName();

                        if (sb.length() > 0)
                            sb.append(separator);

                        try {
                            sb.append(URLEncoder.encode(localName, "utf-8"));
                            sb.append('=');
                            sb.append(URLEncoder.encode(text, "utf-8"));
                            // TODO: check if line breaks will be correcly encoded as "%0D%0A"
                        } catch (UnsupportedEncodingException e) {
                            // Should not happen: utf-8 must be supported
                            throw new OXFException(e);
                        }
                    }
                }
            }
        });

        return sb.toString();
    }

    private XFormsSubmitErrorEvent createErrorEvent(PipelineContext pipelineContext, ConnectionResult connectionResult, ErrorType errorType) throws IOException {
        return new XFormsSubmitErrorEvent(pipelineContext, XFormsModelSubmission.this, errorType, connectionResult);
    }

    public static class ConnectionResult {
        public boolean dontHandleResponse;
        public int statusCode;
        public String responseMediaType;
        public Map responseHeaders;
        public long lastModified;
        public String resourceURI;

        private InputStream responseInputStream;
        private boolean hasContent;

        public ConnectionResult(String resourceURI) {
            this.resourceURI = resourceURI;
        }

        public InputStream getResponseInputStream() {
        	return responseInputStream;
        }

        public boolean hasContent() {
            return hasContent;
        }

        public void setResponseInputStream(final InputStream responseInputStream) throws IOException {
        	this.responseInputStream = responseInputStream;
        	setHasContentFlag();

        }

        private void setHasContentFlag() throws IOException {
            if (responseInputStream == null) {
                hasContent = false;
            } else {
                if (!responseInputStream.markSupported())
                    this.responseInputStream = new BufferedInputStream(responseInputStream);

                responseInputStream.mark(1);
                hasContent = responseInputStream.read() != -1;
                responseInputStream.reset();
            }
        }

        public void forwardHeaders(ExternalContext.Response response) {
            if (responseHeaders != null) {
                for (Iterator i = responseHeaders.entrySet().iterator(); i.hasNext();) {
                    final Map.Entry currentEntry = (Map.Entry) i.next();
                    final String headerName = (String) currentEntry.getKey();

                    if (headerName != null) {
                        // NOTE: As per the doc, this should always be a List, but for some unknown reason
                        // it appears to be a String sometimes
                        if (currentEntry.getValue() instanceof String) {
                            // Case of String
                            final String headerValue = (String) currentEntry.getValue();
                            forwardHeaderFilter(response, headerName, headerValue);
                        } else {
                            // Case of List
                            final List headerValues = (List) currentEntry.getValue();
                            if (headerValues != null) {
                                for (Iterator j = headerValues.iterator(); j.hasNext();) {
                                    final String headerValue = (String) j.next();
                                    if (headerValue != null) {
                                        forwardHeaderFilter(response, headerName, headerValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private void forwardHeaderFilter(ExternalContext.Response response, String headerName, String headerValue) {
            /**
             * Filtering the Transfer-Encoding header
             *
             * We don't pass the Transfer-Encoding header, as the request body is
             * already decoded for us. Passing along the Transfer-Encoding causes a
             * problem if the server sends us chunked data and we send it in the
             * response not chunked but saying in the header that it is chunked.
             *
             * Non-filtering of Content-Encoding header
             *
             * The Content-Encoding has the potential of causing the same problem as
             * the Transfer-Encoding header. It could be an issue if we get data with
             * Content-Encoding: gzip, but pass it along uncompressed but still
             * include the Content-Encoding: gzip. However this does not happen, as
             * the request we send does not contain a Accept-Encoding: gzip,deflate. So
             * filtering the Content-Encoding header is safe here.
             */
            if (!"transfer-encoding".equals(headerName.toLowerCase())) {
                response.addHeader(headerName, headerValue);
            }
        }

        public void close() {}
    }

    private class XFormsSubmissionException extends ValidationException {

        private final ErrorType errorType;

        public XFormsSubmissionException(final String message, final String description) {
            this(message, description, XFormsSubmitErrorEvent.ErrorType.XXFORMS_INTERNAL_ERROR);
        }

        public XFormsSubmissionException(final String message, final String description, final XFormsSubmitErrorEvent.ErrorType errorType) {
            super(message, new ExtendedLocationData(XFormsModelSubmission.this.getLocationData(), description,
                    XFormsModelSubmission.this.getSubmissionElement()));
            this.errorType = errorType;
        }

        public XFormsSubmissionException(final Throwable e, final String message, final String description) {
            this(e, message, description, XFormsSubmitErrorEvent.ErrorType.XXFORMS_INTERNAL_ERROR);
        }

        public XFormsSubmissionException(final Throwable e, final String message, final String description, final XFormsSubmitErrorEvent.ErrorType errorType) {
            super(message, e, new ExtendedLocationData(XFormsModelSubmission.this.getLocationData(), description,
                    XFormsModelSubmission.this.getSubmissionElement()));
            this.errorType = errorType;
        }

        public ErrorType getErrorType() {
            return errorType;
        }
    }
}
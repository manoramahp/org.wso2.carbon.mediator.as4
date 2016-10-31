/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.mediator.as4;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.wso2.carbon.mediator.as4.compression.GZipCompressionDataHandler;
import org.wso2.carbon.mediator.as4.msg.AS4MessageOut;
import org.wso2.carbon.mediator.as4.msg.MessageIdGenerator;
import org.wso2.carbon.mediator.as4.msg.impl.Error;
import org.wso2.carbon.mediator.as4.msg.impl.MessageInfo;
import org.wso2.carbon.mediator.as4.msg.impl.Messaging;
import org.wso2.carbon.mediator.as4.msg.impl.PartInfo;
import org.wso2.carbon.mediator.as4.msg.impl.Property;
import org.wso2.carbon.mediator.as4.msg.impl.SignalMessage;
import org.wso2.carbon.mediator.as4.pmode.PModeRepository;
import org.wso2.carbon.mediator.as4.pmode.impl.PMode;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * AS4 inbound mediator implementation which will build the incoming message for corner3 and save incoming payloads
 * in a temp location. After that it will respond back to corner2 and then it will pass control to next sequence so
 * that custom delivery sequence can be implemented.
 */
public class AS4InboundMediator extends AbstractMediator {
    private static final Log log = LogFactory.getLog(AS4InboundMediator.class);
    private JAXBContext jaxbMessagingContext;
    private PModeRepository pModeRepository;
    private String pModesLocation; //this can be set as custom class mediator properties "pModesLocation"
    private String tempStoreFolder;
    private boolean tempStoreCreated = false;

    /**
     * Constructor for inbound mediator implementation.
     */
    public AS4InboundMediator() {
        try {
            this.jaxbMessagingContext = JAXBContext.newInstance(Messaging.class);
        } catch (JAXBException e) {
            log.error("Unable to create JAXB marshaller and unmarshaller - " + e.getMessage(), e);
            throw new SynapseException("Unable to create JAXB marshaller and unmarshaller - " + e.getMessage(), e);
        }
    }

    @Override
    public boolean mediate(MessageContext messageContext) {
        try {
            createPModeRepo();//to create pMode repository if not exist
            createTempStoreFolder();//to create temp store if not exist
            handleMessage(messageContext);
        } catch (AS4Exception e) {
            handleError(messageContext, e);
        }

        Axis2Sender.sendBack(messageContext);
        return true;
    }

    /**
     * Helper method to handle incoming message.
     *
     * @param messageContext
     * @throws AS4Exception
     */
    private void handleMessage(MessageContext messageContext) throws AS4Exception {
        org.apache.axis2.context.MessageContext axis2MsgContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        try {
            RelayUtils.buildMessage(axis2MsgContext);
        } catch (Exception e) {
            throw new AS4Exception("Error building incoming message, " + e.getMessage(), AS4ErrorMapper.ErrorCode.EBMS0004, null);
        }


        OMElement messageEl = axis2MsgContext.getEnvelope().getHeader().getFirstElement();
        InputStream userMessageStream = new ByteArrayInputStream(messageEl.toString().getBytes());
        Messaging requestMsg = null;
        try {
            Unmarshaller messagingUnMarshaller = jaxbMessagingContext.createUnmarshaller();
            requestMsg = (Messaging) messagingUnMarshaller.unmarshal(userMessageStream);
        } catch (JAXBException e) {
            throw new AS4Exception("Error building message model, " + e.getMessage(), AS4ErrorMapper.ErrorCode.EBMS0004, null);
        }

        AS4Utils.validateMessaging(requestMsg);

        //by this point message id should be there
        String messageId = requestMsg.getUserMessage().getMessageInfo().getMessageId();

        PMode matchingPMode = pModeRepository.findPModeFromAgreement(requestMsg.getUserMessage().getCollaborationInfo().getAgreementRef());
        if (matchingPMode == null) {
            throw new AS4Exception("Error finding matching PMode for this communication",
                                   AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
        }

        AS4MessageOut messageOut = new AS4MessageOut();
        messageOut.setUserMessage(requestMsg.getUserMessage());
        messageOut.setPMode(matchingPMode);

        for (PartInfo partInfo : requestMsg.getUserMessage().getPayloadInfo().getPartInfo()) {
            DataHandler dataHandler = axis2MsgContext.getAttachment(partInfo.getHref());
            Property mimeType = partInfo.getPartPropertiesObj().getProperty(AS4Constants.MIME_TYPE);
            DataHandler gzipDataHandler = new GZipCompressionDataHandler(dataHandler.getDataSource(), mimeType.getValue());
            File partFile = new File(tempStoreFolder + File.separator + partInfo.getHref());
            messageOut.insertPayload(partInfo.getHref(), partFile);
            try {
                gzipDataHandler.writeTo(new FileOutputStream(partFile));
            } catch (IOException e) {
                throw new AS4Exception("Error extracting payload for the id - " + partInfo.getHref(), AS4ErrorMapper.ErrorCode.EBMS0303, messageId);
            }
        }

        File userMessageFile = new File(tempStoreFolder + File.separator + requestMsg.getUserMessage().getMessageInfo().getMessageId());
        try {
            FileOutputStream outputStream = new FileOutputStream(userMessageFile);
            messageEl.serialize(outputStream);
        } catch (Exception e) {
            throw new AS4Exception("Error writing user message to a file, " + e.getMessage(), AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
        }


        SignalMessage signalMessage = new SignalMessage();

        MessageInfo responseMessageInfo = new MessageInfo();
        responseMessageInfo.setTimestamp(new Date());
        responseMessageInfo.setMessageId(MessageIdGenerator.createMessageId());
        responseMessageInfo.setRefToMessageId(requestMsg.getUserMessage().getMessageInfo().getMessageId());

        signalMessage.setMessageInfo(responseMessageInfo);

        Messaging responseMessaging = new Messaging();
        responseMessaging.setMustUnderstand("true");

        responseMessaging.setSignalMessage(signalMessage);

        OMNode node = null;
        try {
            Marshaller messagingMarshaller = jaxbMessagingContext.createMarshaller();

//          OMNode node = XMLUtils.toOM(in);
            node = AS4Utils.getOMNode(messagingMarshaller, responseMessaging);
        } catch (Exception e) {
            throw new AS4Exception("Error generating response for the message, " + e.getMessage(), AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
        }
        if (node == null) {
            throw new AS4Exception("Error generated response is invalid", AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
        }

        SOAPEnvelope soapEnvelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
        soapEnvelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPHeader());
        soapEnvelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        soapEnvelope.getHeader().addChild(node);

        try {
            messageContext.setEnvelope(soapEnvelope);
        } catch (AxisFault axisFault) {
            throw new AS4Exception("Error creating valid response, " + axisFault.getMessage(), AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
        }
        messageContext.setTo(null);
        messageContext.setProperty(AS4Constants.AS4_OUT_MESSAGE, messageOut);
    }

    /**
     * Helper method to handle error scenarios.
     *
     * @param messageContext
     * @param e
     */
    private void handleError(MessageContext messageContext, AS4Exception e) {
        SignalMessage signalMessage = new SignalMessage();

        MessageInfo responseMessageInfo = new MessageInfo();
        responseMessageInfo.setTimestamp(new Date());
        responseMessageInfo.setMessageId(MessageIdGenerator.createMessageId());

        signalMessage.setMessageInfo(responseMessageInfo);

        Error error = new Error();
        AS4ErrorMapper.setErrorDetailsAndDesc(e.getAs4ErrorCode(), error, e.getMessage());
        if (e.getMessageId() != null) {
            responseMessageInfo.setRefToMessageId(e.getMessageId());
            error.setRefToMessageInError(e.getMessageId());
        }

        signalMessage.setError(error);

        Messaging responseMessaging = new Messaging();
        responseMessaging.setMustUnderstand("true");

        responseMessaging.setSignalMessage(signalMessage);

        OMNode node = null;
        try {
            Marshaller messagingMarshaller = jaxbMessagingContext.createMarshaller();

//          OMNode node = XMLUtils.toOM(in);
            node = AS4Utils.getOMNode(messagingMarshaller, responseMessaging);
        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("Error creating response error message, " + ex.getMessage(), ex);
            }
        }
        if (node == null) {
            if (log.isDebugEnabled()) {
                log.debug("Created error node is invalid");
            }
            return;
        }

        SOAPEnvelope soapEnvelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
        soapEnvelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPHeader());
        soapEnvelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        soapEnvelope.getHeader().addChild(node);

        try {
            messageContext.setEnvelope(soapEnvelope);
        } catch (AxisFault axisFault) {
            if (log.isDebugEnabled()) {
                log.debug("Error attaching create error response, " + axisFault.getMessage(), axisFault);
            }
        }
        messageContext.setTo(null);
    }

    /**
     * Helper method to create PMode repository.
     */
    private void createPModeRepo() {
        if (pModeRepository == null) {
            initPModeRepository();
        }
    }

    /**
     * Synchronized method so that single PMode repo will get created per proxy.
     */
    private synchronized void initPModeRepository() {
        if (pModeRepository == null) {
            pModeRepository = new PModeRepository(pModesLocation);
        }
    }

    /**
     * Helper method to create temp store location if not already exist.
     */
    private void createTempStoreFolder() {
        if (!tempStoreCreated) {
            initTempStoreFolder();
        }
    }

    /**
     * Synchronized method to create it only once.
     */
    private synchronized void initTempStoreFolder() {
        if (!tempStoreCreated) {
            if (tempStoreFolder == null || tempStoreFolder.isEmpty()) {
                tempStoreFolder = AS4Constants.INBOUND_AS4_PAYLOAD_TEMP_STORE_DEFAULT_LOCATION;
            }
            if (tempStoreFolder.endsWith(File.separator)) {
                tempStoreFolder = tempStoreFolder.substring(0, tempStoreFolder.length() - File.separator.length());
            }
            File tmpStoreDir = new File(String.valueOf(tempStoreFolder));
            if (!tmpStoreDir.exists()) {
                tmpStoreDir.mkdir();
            }
            tempStoreCreated = true;
        }
    }

    public String getPModesLocation() {
        return pModesLocation;
    }

    public void setPModesLocation(String pModesLocation) {
        this.pModesLocation = pModesLocation;
    }

    public String getTempStoreFolder() {
        return tempStoreFolder;
    }

    public void setTempStoreFolder(String tempStoreFolder) {
        this.tempStoreFolder = tempStoreFolder;
    }
}

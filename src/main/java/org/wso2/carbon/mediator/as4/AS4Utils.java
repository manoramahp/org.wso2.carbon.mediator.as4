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

import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.wso2.carbon.mediator.as4.msg.impl.Messaging;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Util class which provides some common utilities.
 */
public class AS4Utils {

    /**
     * Helper method to convert Messaging object to relevant xml element.
     *
     * @param marshaller
     * @param messaging
     * @return node
     * @throws IOException
     * @throws XMLStreamException
     * @throws PropertyException
     */
    public static OMNode getOMNode(final Marshaller marshaller, final Messaging messaging)
            throws IOException, XMLStreamException, PropertyException {
        final PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream();

        in.connect(out);

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        new Thread(new Runnable() {
            public void run() {
                try {
                    // write the original OutputStream to the PipedOutputStream
                    marshaller.marshal(messaging, out);
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //Create a new builder with the StAX reader
        StAXOMBuilder builder = new StAXOMBuilder(in);

        OMNode node = builder.getDocumentElement();
        node.close(true);

        return node;
    }

    /**
     * Helper method to validate messaging object to see whether it contains required fields.
     * Will throw AS4Exception with error message and error code for failed validation.
     *
     * @param messaging
     * @throws org.wso2.carbon.mediator.as4.AS4Exception
     */
    public static void validateMessaging(Messaging messaging) throws AS4Exception {
        if (messaging == null) {
            throw new AS4Exception("Incoming messaging element is invalid", AS4ErrorMapper.ErrorCode.EBMS0001, null);
        }
        if (messaging.getUserMessage() == null) {
            throw new AS4Exception("Incoming userMessage element is invalid", AS4ErrorMapper.ErrorCode.EBMS0001, null);
        }
        if (messaging.getUserMessage().getMessageInfo() == null) {
            throw new AS4Exception("Incoming messageInfo element is invalid", AS4ErrorMapper.ErrorCode.EBMS0001, null);
        }
        String messageId = messaging.getUserMessage().getMessageInfo().getMessageId();
        if (messageId == null || messageId.isEmpty()) {
            throw new AS4Exception("Incoming message id is invalid", AS4ErrorMapper.ErrorCode.EBMS0001, null);
        }
        if (messaging.getUserMessage().getCollaborationInfo() == null) {
            throw new AS4Exception("Incoming CollaborationInfo element is invalid", AS4ErrorMapper.ErrorCode.EBMS0003, messageId);
        }
        String agreementRef = messaging.getUserMessage().getCollaborationInfo().getAgreementRef();
        if (agreementRef == null || agreementRef.isEmpty()) {
            throw new AS4Exception("Incoming agreementRef is invalid", AS4ErrorMapper.ErrorCode.EBMS0003, messageId);
        }
    }
}

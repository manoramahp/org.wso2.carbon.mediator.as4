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

package org.wso2.carbon.mediator.as4.temp;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.mediator.as4.AS4Constants;
import org.wso2.carbon.mediator.as4.msg.AS4MessageIn;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Iterator;

public class CustomClassMediator extends AbstractMediator {
    @Override
    public boolean mediate(MessageContext messageContext) {


        String agreementRef = messageContext.getEnvelope().getBody().getFirstElement().getFirstChildWithName(new QName("agreementRef")).getText();

        AS4MessageIn messageIn = new AS4MessageIn();

        messageIn.setAgreementRef(agreementRef);

        Iterator<OMElement> payloads = messageContext.getEnvelope().getBody().getFirstElement().getFirstChildWithName(new QName("payloads")).getChildElements();

        while (payloads.hasNext()) {
            OMElement payload = payloads.next();
            String mimeType = payload.getFirstChildWithName(new QName("mimeType")).getText();
            String filePath = payload.getFirstChildWithName(new QName("path")).getText();
            messageIn.insertPayload(mimeType, new File(filePath));
        }

        messageContext.setProperty(AS4Constants.AS4_IN_MESSAGE, messageIn);
        return true;
    }
}

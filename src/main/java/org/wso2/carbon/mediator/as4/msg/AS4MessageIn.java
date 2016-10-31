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
package org.wso2.carbon.mediator.as4.msg;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the object which needs to be created before calling AS4OutboundMediator.
 */
public class AS4MessageIn {
    //agreementRef value
    private String agreementRef;

    //payloads which needs to be sent to corner3
    private List<AS4Payload> payloads;

    /**
     * Constructor for AS4MessageIn
     */
    public AS4MessageIn() {
        this.payloads = new ArrayList<AS4Payload>(1);
    }

    public String getAgreementRef() {
        return agreementRef;
    }

    public void setAgreementRef(String agreementRef) {
        this.agreementRef = agreementRef;
    }

    public List<AS4Payload> getPayloads() {
        return payloads;
    }

    /**
     * Api method to insert payloads.
     *
     * @param payload
     */
    public void insertPayload(AS4Payload payload) {
        this.payloads.add(payload);
    }

    /**
     * Api method to insert input stream payloads.
     *
     * @param mimeType
     * @param payloadStream
     */
    public void insertPayload(String mimeType, InputStream payloadStream) {
        AS4Payload payload = new AS4Payload(mimeType, payloadStream);
        this.payloads.add(payload);
    }

    /**
     * Api method to insert file payloads.
     *
     * @param mimeType
     * @param payloadFile
     */
    public void insertPayload(String mimeType, File payloadFile) {
        AS4Payload payload = new AS4Payload(mimeType, payloadFile);
        this.payloads.add(payload);
    }
}

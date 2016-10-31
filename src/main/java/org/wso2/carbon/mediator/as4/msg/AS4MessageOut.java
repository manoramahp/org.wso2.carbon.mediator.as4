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

import org.wso2.carbon.mediator.as4.msg.impl.UserMessage;
import org.wso2.carbon.mediator.as4.pmode.impl.PMode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the object which will be emitted from AS4InboundMediator to the following sequence. So with this,
 * any kind of delivery logic can be implemented.
 */
public class AS4MessageOut {

    private UserMessage userMessage;

    //id(href/uuid), payload file
    private Map<String, File> payloads;

    private PMode pMode;

    public AS4MessageOut() {
        payloads = new HashMap<String, File>();
    }

    public UserMessage getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(UserMessage userMessage) {
        this.userMessage = userMessage;
    }


    public Map<String, File> getPayloads() {
        return payloads;
    }

    public void setPayloads(Map<String, File> payloads) {
        this.payloads = payloads;
    }

    public PMode getPMode() {
        return pMode;
    }

    public void setPMode(PMode pMode) {
        this.pMode = pMode;
    }

    public void insertPayload(String id, File file) {
        this.payloads.put(id, file);
    }
}

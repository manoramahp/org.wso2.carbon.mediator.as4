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

import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;

/**
 * AS4 Exception Implementation. Which has error codes, so that it is easy to generate error messages.
 */
public class AS4Exception extends SynapseException {
    private AS4ErrorMapper.ErrorCode as4ErrorCode;
    private String messageId;

    public AS4Exception(String string, AS4ErrorMapper.ErrorCode as4ErrorCode, String messageId) {
        super(string);
        this.as4ErrorCode = as4ErrorCode;
        this.messageId = messageId;
    }

    public AS4Exception(String msg, Throwable e, AS4ErrorMapper.ErrorCode as4ErrorCode, String messageId) {
        super(msg, e);
        this.as4ErrorCode = as4ErrorCode;
        this.messageId = messageId;
    }

    public AS4Exception(Throwable t, AS4ErrorMapper.ErrorCode as4ErrorCode, String messageId) {
        super(t);
        this.as4ErrorCode = as4ErrorCode;
        this.messageId = messageId;
    }

    public AS4Exception(String msg, SynapseLog synLog, AS4ErrorMapper.ErrorCode as4ErrorCode, String messageId) {
        super(msg, synLog);
        this.as4ErrorCode = as4ErrorCode;
        this.messageId = messageId;
    }

    public AS4Exception(String msg, Throwable cause, SynapseLog synLog, AS4ErrorMapper.ErrorCode as4ErrorCode, String messageId) {
        super(msg, cause, synLog);
        this.as4ErrorCode = as4ErrorCode;
        this.messageId = messageId;
    }

    public AS4ErrorMapper.ErrorCode getAs4ErrorCode() {
        return as4ErrorCode;
    }

    public String getMessageId() {
        return messageId;
    }
}

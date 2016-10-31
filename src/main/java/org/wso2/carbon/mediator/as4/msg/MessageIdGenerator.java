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

import java.util.UUID;

/**
 * Created by rajith on 9/21/16.
 */
public class MessageIdGenerator {
    public static String createMessageId() {
        // Generate a UUID as the msg-id
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    public static String generateContentId(String messageId, long nanoTime) {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid + messageId + nanoTime;
        return uuid;
    }

    public static String generateConversationId() {//todo discuss and implement this if needed
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }
}

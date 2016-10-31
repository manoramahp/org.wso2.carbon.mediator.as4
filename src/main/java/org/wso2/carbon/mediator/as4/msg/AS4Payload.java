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

/**
 * Payload implementation which contains either input stream payload of file payload.
 */
public class AS4Payload {
    //mime type of the payload.
    private String mimeType;

    //File payload.
    private File payloadFile;

    //Input stream payload.
    private InputStream payloadStream;

    //Payload type (File or InputStream).
    private PayloadType payloadType;

    /**
     * Constructor which takes file type payload.
     *
     * @param mimeType
     * @param payloadFile
     */
    public AS4Payload(String mimeType, File payloadFile) {
        this.mimeType = mimeType;
        this.payloadFile = payloadFile;
        this.payloadType = PayloadType.FILE;
    }

    /**
     * Constructor which takes input stream type payload.
     *
     * @param mimeType
     * @param payloadStream
     */
    public AS4Payload(String mimeType, InputStream payloadStream) {
        this.mimeType = mimeType;
        this.payloadStream = payloadStream;
        this.payloadType = PayloadType.INPUT_STREAM;
    }

    /**
     * Get mime type of the payload.
     *
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public File getPayloadFile() {
        return payloadFile;
    }

    public void setPayloadFile(File payloadFile) {
        this.payloadFile = payloadFile;
    }

    public InputStream getPayloadStream() {
        return payloadStream;
    }

    public void setPayloadStream(InputStream payloadStream) {
        this.payloadStream = payloadStream;
    }

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public enum PayloadType {
        FILE,
        INPUT_STREAM
    }
}

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

/**
 * Constants related to AS4 implementation.
 */
public class AS4Constants {

    /**
     * String representing SOAPAction which is used to set WSAction in synapse message properties.
     */
    public final static String SOAP_ACTION = "SOAPAction";

    /**
     * String representing port of the InboundEndpoint
     */
    public static final String INBOUND_ENDPOINT_PARAMETER_AS4_PMODE_DEFAULT_LOCATION = "repository/conf/pModes/";



    public static final String COMPRESSION_TYPE = "CompressionType";

    public static final String MIME_TYPE = "MimeType";


//    public static final String INBOUND_AS4_PAYLOAD_DELIVERY_LOCATION = "inbound.as4.payload.delivery.location"; //todo parameter
//    public static final String INBOUND_AS4_PAYLOAD_DELIVERY_DEFAULT_LOCATION = "inbound";

//    public static final String INBOUND_AS4_PAYLOAD_TEMP_STORE_LOCATION = "inbound.as4.payload.temp.store.location"; //todo parameter
    public static final String INBOUND_AS4_PAYLOAD_TEMP_STORE_DEFAULT_LOCATION = "tempInbound"; //todo parameter

    public static final String AS4_IN_MESSAGE = "as4.in.message";
    public static final String AS4_OUT_MESSAGE = "as4.out.message";
    public static final String AS4_RESPONSE_MESSAGE = "as4.response.message";
    public static final String AS4_ENDPOINT_ADDRESS = "as4.endpoint.address";


}
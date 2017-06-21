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
package org.wso2.carbon.mediator.as4.connection;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.saaj.util.UnderstandAllHeadersHandler;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.mediator.as4.AS4ErrorMapper;
import org.wso2.carbon.mediator.as4.AS4Exception;
import org.wso2.carbon.mediator.as4.pmode.impl.PMode;
import org.wso2.carbon.mediator.as4.pmode.impl.Retry;

import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * AS4 connection which will be used to invoke corner3.
 */
public class AS4Connection {
    private static final Log log = LogFactory.getLog(AS4Connection.class);
    /**
     * Attribute which keeps track of whether this connection has been closed
     */
    private boolean closed = false;

    private final ConfigurationContext configurationContext;

    /**
     * Constructor which creates default configuration Context.
     *
     * @throws SOAPException
     */
    public AS4Connection() throws SOAPException {
        // Create a new ConfigurationContext that will be used by all ServiceClient instances.
        // There are two reasons why this is necessary:
        //  * Starting with r921685, if no ConfigurationContext is supplied to the ServiceClient,
        //    it will create a new one (unless it can locate one using MessageContext.getCurrentMessageContext(),
        //    but this is not the most common use case for SOAPConnection). This means that
        //    SOAPConnection#call would create a new ConfigurationContext every time, and this is
        //    too expensive.
        //  * We need to disable mustUnderstand processing. However, we can't do that on an AxisConfiguration
        //    that is shared with other components, because this would lead to unpredictable results.
        // Note that we could also use a single ServiceClient instance, but then the SOAPConnection
        // implementation would no longer be thread safe. Although thread safety is not explicitly required
        // by the SAAJ specs, it appears that the SOAPConnection in Sun's reference implementation is
        // thread safe.
        try {
            this.configurationContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
            disableMustUnderstandProcessing(configurationContext.getAxisConfiguration());
        } catch (AxisFault ex) {
            throw new SOAPException(ex);
        }
    }

    /**
     * Constructor which accept a config context.
     *
     * @param configurationContext
     * @throws SOAPException
     */
    public AS4Connection(ConfigurationContext configurationContext) throws SOAPException {
        this.configurationContext = configurationContext;
        disableMustUnderstandProcessing(configurationContext.getAxisConfiguration());
    }

    /**
     * Sends the given message to the specified endpoint and blocks until it has returned the
     * response.
     *
     * @param msgCtx   the msgCtx object to be sent
     * @param endpoint an <code>Object</code> that identifies where the message should be sent. It
     *                 is required to support Objects of type <code>java.lang.String</code>,
     *                 <code>java.net.URL</code>, and when JAXM is present <code>javax.xml.messaging.URLEndpoint</code>
     * @return the <CODE>SOAPMessage</CODE> object that is the response to the message that was
     * sent
     * @throws javax.xml.soap.SOAPException if there is a SOAP error, or this SOAPConnection is
     *                                      already closed
     */
    public MessageContext call(MessageContext msgCtx, Object endpoint, PMode pMode, String messageId) throws AS4Exception, SOAPException {
        boolean sendReceipt = false;
        boolean deliveryFailuresNotifyProducer = false;
        boolean processErrorsNotifyProducer = false;
        if(pMode.getSecurity() != null) {
            sendReceipt = pMode.getSecurity().isSendReceipt();
        }
        if(pMode.getErrorHandling() != null && pMode.getErrorHandling().getReport() != null) {
            deliveryFailuresNotifyProducer = pMode.getErrorHandling().getReport().isDeliveryFailuresNotifyProducer();
            processErrorsNotifyProducer = pMode.getErrorHandling().getReport().isProcessErrorNotifyProducer();
        }
        if (closed) {
            if(sendReceipt && deliveryFailuresNotifyProducer) {
                throw new AS4Exception("Connection Failure", AS4ErrorMapper.ErrorCode.EBMS0005, messageId);
            }
            throw new SOAPException("SOAPConnection closed");
        }

        // initialize URL
        URL url;
        try {
            url = (endpoint instanceof URL) ? (URL) endpoint : new URL(endpoint.toString());
        } catch (MalformedURLException e) {
            if(sendReceipt && processErrorsNotifyProducer) {
                throw new AS4Exception("Invalid Endpoint", AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
            }
            log.error(e);
            throw new SOAPException(e);
        }

        int maxRetries = 0;
        int period = 0;
        if(pMode.getReceptionAwareness() != null && pMode.getReceptionAwareness().getRetry() != null) {
            Retry retry = pMode.getReceptionAwareness().getRetry();
            if(retry.getParameters()!= null && !retry.getParameters().isEmpty()) {
                String[] parameters = retry.getParameters().split("[\\s,;]+");
                for(String param : parameters) {
                    String[] keyValuePair = param.split("[\\s=]+");
                    if(keyValuePair[0].contentEquals("maxretries")) {
                        maxRetries = Integer.parseInt(keyValuePair[1]);
                    } else if (keyValuePair[0].contentEquals("period")) {
                        period = Integer.parseInt(keyValuePair[1]);
                    } else {
                        if(sendReceipt && processErrorsNotifyProducer) {
                            throw new AS4Exception("Invalid parameter : " + keyValuePair[0], AS4ErrorMapper.ErrorCode.EBMS0001, messageId);
                        }
                    }
                }
            }
        }

        // initialize and set Options
//        Options options = new Options();
        Options options = msgCtx.getOptions();
        options.setTo(new EndpointReference(url.toString()));

        HttpMethodParams methodParams = new HttpMethodParams();
        final int finalPeriod = period;
        HttpMethodRetryHandler httpMethodRetryHandler = new DefaultHttpMethodRetryHandler(maxRetries, false) {
            @Override
            public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
                boolean isRetry = super.retryMethod(method, exception, executionCount);
                if (isRetry && finalPeriod > 0) {
                    try {
                        Thread.sleep(finalPeriod);
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
                return isRetry;
            }
        };

        methodParams.setParameter(HttpMethodParams.RETRY_HANDLER, httpMethodRetryHandler);
        options.setProperty(HTTPConstants.HTTP_METHOD_PARAMS, methodParams);

        // initialize the Sender
        ServiceClient serviceClient;
        OperationClient opClient;
        try {
            serviceClient = new ServiceClient(configurationContext, null);
            opClient = serviceClient.createClient(ServiceClient.ANON_OUT_IN_OP);
        } catch (AxisFault e) {
            if(sendReceipt && deliveryFailuresNotifyProducer) {
                log.error(e);
                throw new AS4Exception("Error creating service client", AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
            }
            throw new SOAPException(e);
        }

        MessageContext requestMsgCtx = new MessageContext();
        SOAPEnvelope envelope;
        envelope = msgCtx.getEnvelope();
        if (msgCtx.getAttachmentMap().getMap().size() != 0) { // SOAPMessage with attachments
            requestMsgCtx.setAttachmentMap(msgCtx.getAttachmentMap());
            options.setProperty(Constants.Configuration.ENABLE_SWA, Constants.VALUE_TRUE);
        }
        opClient.setOptions(options);
        try {
            MessageContext responseMsgCtx = null;
            try {
                requestMsgCtx.setEnvelope(envelope);
                opClient.addMessageContext(requestMsgCtx);
                opClient.execute(true);
                responseMsgCtx =
                        opClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                return responseMsgCtx;
            } catch (AxisFault ex) {
                if(sendReceipt && deliveryFailuresNotifyProducer) {
                    // Delivery failure -> Missing Receipt
                    throw new AS4Exception("Missing Receipt", AS4ErrorMapper.ErrorCode.EBMS0301, messageId);
                }
                throw new SOAPException(ex);
            }
        } finally {
            try {
                serviceClient.cleanupTransport();
                serviceClient.cleanup();
            } catch (AxisFault ex) {
                if(sendReceipt && deliveryFailuresNotifyProducer) {
                    throw new AS4Exception("Error cleaning up service client", AS4ErrorMapper.ErrorCode.EBMS0004, messageId);
                }
                throw new SOAPException(ex);
            }
        }
    }

    /**
     * Installs UnderstandAllHeadersHandler that marks all headers as processed
     * because MU validation should not be done for SAAJ clients.
     *
     * @param config
     */
    private void disableMustUnderstandProcessing(AxisConfiguration config) {
        DispatchPhase phase;
        phase = getDispatchPhase(config.getInFlowPhases());
        if (phase != null) {
            phase.addHandler(new UnderstandAllHeadersHandler());
        }
        phase = getDispatchPhase(config.getInFaultFlowPhases());
        if (phase != null) {
            phase.addHandler(new UnderstandAllHeadersHandler());
        }
    }

    private static DispatchPhase getDispatchPhase(List<Phase> phases) {
        for (Phase phase : phases) {
            if (phase instanceof DispatchPhase) {
                return (DispatchPhase) phase;
            }
        }
        return null;
    }

    /**
     * Closes this <CODE>SOAPConnection</CODE> object.
     *
     * @throws javax.xml.soap.SOAPException if there is a SOAP error, or this SOAPConnection is
     *                                      already closed
     */
    public void close() throws SOAPException {
        if (closed) {
            throw new SOAPException("SOAPConnection Closed");
        }
        try {
            configurationContext.terminate();
        } catch (AxisFault axisFault) {
            throw new SOAPException(axisFault.getMessage());
        }
        closed = true;
    }
}

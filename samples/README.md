# WSO2 ESB AS4 Sample

## This sample will guide through how to setup AS4 custom mediator and send messages.

### Requirements

1) Two wso2esb-5.0.0 nodes (2nd node configured with port offset : 1)
2) SoapUI

### Steps

1) Copy the org.wso2.carbon.mediator.as4-1.0.0-SNAPSHOT.jar to
   <ESB_HOME>/repository/components/dropins/ folder of both ESB nodes

2) Copy the resources/as4.xml file to <ESB_HOME>/repository/conf/axis2/ in ESB node-1.
   (This will be the corner-2 node)

3) Copy the resources/push.xml file to <ESB_HOME>/repository/conf/pModes folder of both ESB nodes
   (This is the PMode file and <ESB_HOME>/repository/conf/pModes is the default PMode location)

4) Copy the resources/as4C2.xml file to the following location in ESB node-1
   <ESB_HOME>/repository/deployment/server/synapse-configs/default/proxy-services

5) Copy the resource/as4C3.xml file to the following location in ESB node-2
   <ESB_HOME>/repository/deployment/server/synapse-configs/default/proxy-services

6) Send the following Soap request to the ESB node-1

    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
        <soapenv:Header/>
        <soapenv:Body>
            <as4Msg>
                <agreementRef>http://wso2.org/examples/agreement0</agreementRef>
                <payloads>
                    <payload>
                        <mimeType>text/xml</mimeType>
                        <path>/home/user/Desktop/simple_document.txt</path>
                    </payload>
                    <payload>
                        <mimeType>text/xml</mimeType>
                        <path>/home/user/Desktop/simple_document.xml</path>
                    </payload>
                </payloads>
            </as4Msg>
        <soapenv:Body/>
    </soapenv:Envelope>

7) You will be able to see the sent payload in the following location in ESB node-2.
    <ESB_HOME>/tempInbound/
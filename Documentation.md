# Documentation

## PMode Parameters Supported by WSO2 ESB AS4 Custom Mediator

|------------------------------------------------------------|--------------------------|
|PMode Parameter                                             |     Supported or not     |
|------------------------------------------------------------|--------------------------|
|PMode.ID                                                    |       true               |
|PMode.Agreement                                             |       true               |
|PMode.MEP                                                   |       true               |
|PMode.MEPbinding                                            |       true               |
|PMode.Initiator.Party                                       |       true               |
|PMode.Initiator.Role                                        |       true               |
|PMode.Initiator.Authorization.username                      |       false              |
|PMode.Initiator.Authorization.password                      |       false              |
|PMode.Responder.Party                                       |       true               |
|PMode.Responder.Role                                        |       true               |
|PMode.Responder.Authorization.username                      |       false              |
|PMode.Responder.Authorization.password                      |       false              |
|PMode.Protocol.Address                                      |       true               |
|PMode.Protocol.SOAPVersion                                  |       true               |
|PMode.BusinessInfo.Service                                  |       true               |
|PMode.BusinessInfo.Action                                   |       true               |
|PMode.BusinessInfo.Properties[]                             |       false              |
|PMode.BusinessInfo.PayloadProfile[]                         |       false              |
|PMode.BusinessInfo.PayloadProfile.maxSize                   |       false              |
|PMode.ErrorHandling.Report.SenderErrorsTo                   |       false              |
|PMode.ErrorHandling.Report.ReceiverErrorsTo                 |       false              |
|PMode.ErrorHandling.Report.AsResponse                       |       true               |
|PMode.ErrorHandling.Report.ProcessErrorNotifyConsumer       |       false              |
|PMode.ErrorHandling.Report.ProcessErrorNotifyProducer       |       true               |
|PMode.ErrorHandling.Report.DeliveryFailuresNotifyProducer   |       true               |
|PMode.Security.WSSVersion                                   |       false              |
|PMode.Security.X509.Sign                                    |       false              |
|PMode.Security. X509.Encryption                             |       false              |
|PMode.Security.UsernameToken                                |       false              |
|PMode.Security.PModeAuthorize                               |       false              |
|PMode.Security.SendReceipt                                  |       true               |
|PMode.Security.SendReceipt.NonRepudiation                   |       false              |
|PMode.PayloadService.CompressionType                        |       true               |
|PMode.ReceptionAwareness                                    |       true               |
|PMode.ReceptionAwareness.Retry.Parameters                   |       true               |
|---------------------------------------------------------------------------------------|


## Error Handling and Reception Awareness - PMode Parameter Configurations

**PMode.Security.SendReceipt** must be set to **true** in order to receive **eb3:Error** in case of an error.

**PMode.ErrorHandling.Report.AsResponse** must be set to **true** in order to transmit an error synchronously to the Sender

**PMode.ErrorHandling.Report.ProcessErrorNotifyProducer** can be set to true/false so that the escalation of error to the sender can be controlled when a **processing error** is occurred.

**PMode.ErrorHandling.Report.DeliveryFailuresNotifyProducer** can be set to true/false so that the escalation of error to the sender can be controlled when a **delivery error** is occurred.

**PMode.ReceptionAwareness**
**PMode.ReceptionAwareness.Retry.Parameters**

When a receiving MSH is not available due to unforeseen errors, reception awareness
ensure the message delivery retry is done as per the configured Retry.Parameters
Eg: - **maxretries=3;period=5000**


## Support for setting PMode values dynamically
   In the corner-2 ESB, property values should be set as below before the mediator org.wso2.carbon.mediator.as4.AS4OutboundMediator

``
   <property name="/*[name()='PMode']/*[name()='Agreement']/*[name()='Name']"
                      value="http://wso2.org/examples/agreement1"/>
 ``                     

   Property name should be the XPath expression of the corresponding PMode value
   Property value is the new value to be set as the PMode value

   These property values will be read inside the org.wso2.carbon.mediator.as4.AS4OutboundMediator and will replace the
   PMode values corresponding to the given XPath.


package eu.h2020.symbiote.client;



import javax.websocket.*;
import java.io.IOException;
import java.net.URI;



public class TestWebSocketClient {
    /////////////////////////////https://www.javatips.net/api/javax.websocket.websocketcontainer///////////////////////////////////////////

    /*
     * Parameters:
     * String platformID,
     * String internalID,
     * String coreUrl
     * String interworkingInterface
     */


    /*
     * TODO
     *  check messageHandler,session.addMessageHandler();
     */


    /*
     * TEST CASES:
     * 1) WRONG PLATFORM ID: ERROR MESSAGE: "Exception 3: null rhClient is null, check platform id,"
     * 2) WRONG INTERNAL ID: ERROR MESSAGE: ERROR: CLOUD RESOURCE FOUND NULL. Check the validity of internal id: 8999671. Is registered ?
     * 3) WRONG INTERWORKING INTERFACE: ERROR MESSAGE:Failed to create session for web socket end point: wss://xxxxxxx/rap/notification
     */

    public static void main(String[] args) {

        /*
         * Fill the next parameters.
         */

        /*
         * The id of the resource we want to observe.
         */
        String internalID            = "899967";

        /*
         * The URL of the L2 Cloud platform
         * where the resource is registered.
         */
        String interWorkingInterface = "<FILL ME>";

        /*
         * The name of the L2 Cloud platform
         * where the resource is registered.
         */

        String platformID  = "<FILL ME>";

        /*
         * Insert your Service Owner credentials,as defined in the registration
         * at https://symbiote-core.intracom-telecom.com/administration Symbiote platform.
         */

        String userName = "xxx";

        String password = "xxx";

        WebSocketApi webSocketApi = new WebSocketApi(platformID,interWorkingInterface);

        webSocketApi.setCredentials(userName,password);

        /*
         * Get the websocket uri
         */

        String endpointURI = null;

        try {
            endpointURI = webSocketApi.getWebSocketURL();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return;
        }

        Session session    = null;

        /*
         * Get the resource id
         * from the internalID
         */

        String resourceId = webSocketApi.getResourceIdFromInternalID(internalID);

        if(resourceId == null){
            System.out.println("Failed to find the resource id");
            String errorMessage = webSocketApi.getErrorMessage();
            System.out.println("ERROR MESSAGE: " + errorMessage);
            return;
        }


        /*
         * Open a web socket to the
         * remote RAP of platformID.
         */

        LocalClientSocket clientSocket = new LocalClientSocket();

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(clientSocket, URI.create(endpointURI));
            webSocketApi.setSession(session);


            if (session != null) {

                /*
                 * Create the subscribe resource
                 * request json message.
                 */

                String message = null;
                try {
                    message = webSocketApi.getMessage(webSocketApi.SUBSCRIBE_COMMAND, resourceId);
                }catch(Exception ex){
                    System.out.println(ex.getMessage().toString());
                    return;
                }

                /*
                 * Send the subscribe command
                 * to the remote RAP microservice.
                 */
                try {
                    webSocketApi.sendMessageToRAP(message);
                }catch (Exception ex){
                    System.out.println("Failed to send message to RAP");
                    return;
                }


                /*
                 * Wait here some time
                 * before unsubscribe the resource.
                 */

                long start = System.currentTimeMillis();
                long duration = 10000; //10 seconds

                while (true){
                    long now = System.currentTimeMillis();
                    if (now - start > duration)
                        break;
                }

                /*
                 * Now is time to unsubscribe
                 * and  close the session.
                 */


                /*
                 * Build the unsubscribe request json message.
                 */

                try {
                    message = webSocketApi.getMessage(webSocketApi.UNSUBSCRIBE_COMMAND, resourceId);
                }catch(Exception ex){
                    System.out.println(ex.getMessage().toString());
                    return;
                }

                /*
                 * Send the unsubscribe command
                 * to the remote RAP microservice.
                 */
                try {
                    webSocketApi.sendMessageToRAP(message);
                }catch (Exception ex){
                    System.out.println("Failed to send message to RAP");
                    return;
                }

                System.out.println("Closing session...");
                webSocketApi.closeSession();

            }else{
                System.out.println("ERROR: Session is null");
            }
        } catch (Exception e) {
            System.out.println("Exception");
            if(session == null){
                System.out.println("Failed to create session for web socket end point: " + endpointURI);
            }
             throw new RuntimeException(e);
        }
    }//end

//--------------------------------------------------------------------------------------
    /*
     * This is the local web socket that receives
     * messages from the remote RAP Web Socket.
     */
    @ClientEndpoint
    public static class LocalClientSocket {
        public String messageEchoed;
        public volatile boolean spin = true;

        @OnOpen
        public void onWebSocketConnect(Session session) throws IOException, EncodeException {
            System.out.println("onWebSocketConnect " + session.getId());
            System.out.println("Web Socket Connection established with remote RAP Web socket ");
            System.out.println("LocalClientSocket session.getOpenSessions().size() " + session.getOpenSessions().size());
            System.out.println("LocalClientSocket session.getMaxIdleTimeout() " + session.getMaxIdleTimeout());
        }//end
        //--------------------------------------------------------------------------------------------
        @OnMessage
        public void onWebSocketText(String message) throws IOException, EncodeException {
            messageEchoed = message;
            spin = false;
            System.out.println("Received message from remote RAP web socket =  " + message);
        }//end
        //--------------------------------------------------------------------------------------------
        @OnClose
        public void onWebSocketClose(CloseReason reason) {
            System.out.println("onWebSocketClose");
            System.out.println("Closing connection with remote RAP web socket,reason =  " + reason);
        }//end
        //--------------------------------------------------------------------------------------------
        @OnError
        public void onWebSocketError(Throwable cause) {
            System.out.println("onWebSocketError");

            if(cause !=null)
             System.out.println("onWebSocketError cause" + cause.getMessage());
        }//end


    }//end of class

    ////////////////////////////////////////

}//end of class

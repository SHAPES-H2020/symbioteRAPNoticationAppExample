package eu.h2020.symbiote.client;

import eu.h2020.symbiote.client.interfaces.CRAMClient;
import eu.h2020.symbiote.client.interfaces.PRClient;
import eu.h2020.symbiote.client.interfaces.RAPClient;
import eu.h2020.symbiote.client.interfaces.RHClient;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.cloud.model.internal.PlatformRegistryQuery;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.model.cim.Observation;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import javax.websocket.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.getFactory;

/*
 * Parameters:
 * String platformID,
 * String coreUrl
 * String interworkingInterface
 */
/*
 * Version 1.0
 */

public class WebSocketApi {


    public static String NO_ERROR = "NO ERROR";
    public static String RESOURCE_ID_NULL     = "ERROR: RESOURCE ID FOUND NULL";
    public static String CLOUD_RESOURCE_NULL  = "ERROR: CLOUD RESOURCE FOUND NULL";
    public static String INVALID_PLATFORM_ID  = " ERROR: INVALID PLATFORM ID ";

    public static String ERROR_MESSAGE = NO_ERROR;

    /*
     * TEST CASES:
     * 1) WRONG PLATFORM ID: ERROR MESSAGE: "Exception 3: null rhClient is null, check platform id,"
     * 2) WRONG INTERNAL ID: ERROR MESSAGE: ERROR: CLOUD RESOURCE FOUND NULL. Check the validity of internal id: 8999671. Is registered ?
     * 3) WRONG INTERWORKING INTERFACE: ERROR MESSAGE:Failed to create session for web socket end point: wss://xxxxxxx/rap/notification

     */

    public static String SUBSCRIBE_COMMAND   = "SUBSCRIBE";
    public static String UNSUBSCRIBE_COMMAND = "UNSUBSCRIBE";

    public String interWorkingInterface = ""; //required
    public String coreUrl               = "https://symbiote-core.intracom-telecom.com";
    public String platformID            = "";//required
    public String userName              = "xxx";
    public String password              = "xxx";

    public Session session = null;

    public WebSocketApi(String platformID,String interWorkingInterface ){
      this.platformID = platformID;
      this.interWorkingInterface = interWorkingInterface;
    }//end
//------------------------------------------------------------------------------------------

public WebSocketApi(String platformID,String interWorkingInterface,String userName,String password){
    this.platformID = platformID;
    this.interWorkingInterface = interWorkingInterface;
    this.userName   = userName;
    this.password   = password;
}//end
//------------------------------------------------------------------------------------------
    public void setCoreAddress(String coreUrl){
        this.coreUrl    = coreUrl;

    }//end
//------------------------------------------------------------------------------------------
    public void setCredentials(String userName,String password){
        this.userName   = userName;
        this.password   = password;
    }//end
//--------------------------------------------------------------------------------------
    public String getWebSocketURL() throws Exception{
        String rapWebSocketUrl = "";
        String tempUrl = null;

        try {
            tempUrl = this.interWorkingInterface.split("//")[1].split("/")[0].split(":")[0];
        }catch(Exception ex){
            tempUrl = null;
        }

        if(tempUrl == null)
            throw new Exception("Failed to parse interworking interface " + this.interWorkingInterface);

        rapWebSocketUrl = "wss://" + tempUrl + "/rap/notification";
        return(rapWebSocketUrl);
    }//end
//------------------------------------------------------------------------------------------
public void setSession(Session session){
   this.session = session;
}//end
//------------------------------------------------------------------------------------------
public Session getSession(){
  return this.session;
}//end
//------------------------------------------------------------------------------------------
public void closeSession() {
    try {
        session.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}//end
//------------------------------------------------------------------------------------------
public void sendMessageToRAP(String message) throws IOException {
    session.getBasicRemote().sendText(message);
}//end
//------------------------------------------------------------------------------------------
public static String getMessage(String command,String resourceId) throws Exception{
    String message = "";
    JSONObject rootJsonObject       = new JSONObject();
    JSONObject secRequestJsonObject = new JSONObject();
    JSONObject payloadJsonObject    = new JSONObject();

    if(resourceId == null){
        throw new Exception("Exception resourceId is null");
    }

    String[] resourceIds = new String[1];
    resourceIds[0] = resourceId;

    if(!command.equals(SUBSCRIBE_COMMAND) && !command.equals(UNSUBSCRIBE_COMMAND)){
        throw new Exception("command: " + command + " not supported");
    }

    if(resourceIds.length == 0){
        throw new Exception("Resource ids array should not be empty");
    }

    /*
     * Build the payload json object
     */

    payloadJsonObject.put("action", command);
    payloadJsonObject.put("ids", resourceIds);

    /*
     * Build the security request json object
     */

    String timestamp = Long.toString(new Timestamp(System.currentTimeMillis()).getTime());
    secRequestJsonObject.put("x-auth-timestamp", timestamp);
    secRequestJsonObject.put("x-auth-size", 0);
    secRequestJsonObject.put("authenticationChallenge", "");
    secRequestJsonObject.put("clientCertificate", "");
    secRequestJsonObject.put("clientCertificateSigningAAMCertificate", "");
    secRequestJsonObject.put("foreignTokenIssuingAAMCertificate", "");

    rootJsonObject.put("payload", payloadJsonObject);
    rootJsonObject.put("secRequest", secRequestJsonObject);
    message = rootJsonObject.toString();
    return message;
}//end

//--------------------------------------------------------------------------------------
public  String getErrorMessage(){
   return ERROR_MESSAGE;
}//end
//--------------------------------------------------------------------------------------
    public  String getResourceIdFromInternalID(String internalID){
        String coreAddress      =  this.coreUrl;
        String keystorePath     = "testKeystore" +  System.currentTimeMillis();
        String keystorePassword = "testKeystore";
        String exampleHomePlatformIdentifier = "SymbIoTe_Core_AAM";
        boolean checkIfIsObserved = true;//false;

        Set<String> platformIds = new HashSet<>(Collections.singletonList(exampleHomePlatformIdentifier));
        AbstractSymbIoTeClientFactory.Type type = AbstractSymbIoTeClientFactory.Type.FEIGN;

        // Get the configuration
        AbstractSymbIoTeClientFactory.Config config = new AbstractSymbIoTeClientFactory.Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory
        AbstractSymbIoTeClientFactory factory;
        try {
            factory = getFactory(config);

            // OPTIONAL section... needs to be run only once
            // - per new platform
            // and/or after revoking client certificate in an already initialized platform


            // ATTENTION: This MUST be an interactive procedure to avoid persisting credentials (password)
            // Here, you can add credentials FOR MORE THAN 1 platforms
            Set<AbstractSymbIoTeClientFactory.HomePlatformCredentials> platformCredentials = new HashSet<>();

            // example credentials
            String username = this.userName;
            String password = this.password;
            String clientId = "webSocketClient";
            AbstractSymbIoTeClientFactory.HomePlatformCredentials exampleHomePlatformCredentials = new AbstractSymbIoTeClientFactory.HomePlatformCredentials(
                    exampleHomePlatformIdentifier,
                    username,
                    password,
                    clientId);
            platformCredentials.add(exampleHomePlatformCredentials);
            /*
             * Get Certificates for the specified platforms.
             */
            factory.initializeInHomePlatforms(platformCredentials);


            // end of optional section..
            // After running it the first time and creating the client keystore you should comment out this section.
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception1 raised");
            ERROR_MESSAGE = "EXCEPTION 1: " + e.getMessage();
            return null;
        }

        /*
         * Get the necessary component clients.
         */

        try {
            factory = getFactory(config);
        }catch(Exception ex){
            System.out.println("Exception2");
            ERROR_MESSAGE = "EXCEPTION 2: " + ex.getMessage();
            return null;
        }

        CRAMClient cramClient       = null;
        RAPClient rapClient         = null;
        RHClient rhClient           = null;
        CloudResource cloudResource = null;

        try {
            // SearchClient searchClient   = factory.getSearchClient();
            cramClient    = factory.getCramClient();
            rapClient     = factory.getRapClient();
            rhClient      = factory.getRHClient(this.platformID);
            if(rhClient == null){
                ERROR_MESSAGE = INVALID_PLATFORM_ID + ": " + this.platformID;
                return null;
            }
            cloudResource = rhClient.getResource(internalID);
            // prcClient =factory.getPRClient("wew");
        }catch(Exception ex ){
            ERROR_MESSAGE = "Exception 3: " + ex.getMessage();
            if(rhClient == null)
                ERROR_MESSAGE += " rhClient is null, check platform id, ";
            if(cramClient == null)
                ERROR_MESSAGE += " cramClient is null, ";
            if(rapClient == null)
                ERROR_MESSAGE += " rapClient is null, ";
            return null;
        }

        if(cloudResource == null){
            ERROR_MESSAGE = CLOUD_RESOURCE_NULL + ". Check the validity of internal id: " + internalID + ". Is registered ? ";
            return null;
        }

        String resourceId = cloudResource.getResource().getId();

        if(resourceId == null){
            ERROR_MESSAGE = RESOURCE_ID_NULL;
            return null;
        }

        String resourceUrl = "";

        try {
            String id   = cloudResource.getResource().getId();
            ResourceUrlsResponse resourceUrlsResponse = cramClient.getResourceUrl(cloudResource.getResource().getId(), true, platformIds);
            resourceUrl = resourceUrlsResponse.getBody().get(cloudResource.getResource().getId());
            Observation observation = null;

            if(checkIfIsObserved == true) {
                try {
                    observation = rapClient.getLatestObservation(resourceUrl, true, platformIds);//check the correct value of platformIds
                }catch(Exception ex){
                    System.out.println("The resource cannot be observed");
                    ERROR_MESSAGE = "The resource cannot be observed,exception: " + ex.getMessage();
                    return null;
                }
                //System.out.println("Latest Observation = " + observation.toString());
            }

        }catch(Exception ex ){
            ERROR_MESSAGE = "EXCEPTION 4: " + ex.getMessage();
            return null;
        }


        return resourceId;
    }//end
//---------------------------------------------------------------------------------------------
    public String getResourceIdFromPlatformRegistry(String internalID,String coreUrl,String federationId) {
        String coreAddress      = this.coreUrl;

        String keystorePath     = "searchL2keystore"+System.currentTimeMillis()+".jks";
        String keystorePassword = "test";
        String exampleHomePlatformIdentifier = this.platformID;


        AbstractSymbIoTeClientFactory.Type type = AbstractSymbIoTeClientFactory.Type.FEIGN;

        /*
         * Get the configuration
         */
        AbstractSymbIoTeClientFactory.Config config = new AbstractSymbIoTeClientFactory.Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory
        AbstractSymbIoTeClientFactory factory;

        try {
            factory = getFactory(config);
            Set<AbstractSymbIoTeClientFactory.HomePlatformCredentials> platformCredentials = new HashSet<>();

            /*
             * User credentials
             */

            String username = this.userName;
            String password = this.password;
            String clientId = "webSocket";
            AbstractSymbIoTeClientFactory.HomePlatformCredentials exampleHomePlatformCredentials = new AbstractSymbIoTeClientFactory.HomePlatformCredentials(
                    exampleHomePlatformIdentifier,
                    username,
                    password,
                    clientId);
            platformCredentials.add(exampleHomePlatformCredentials);
            factory.initializeInHomePlatforms(platformCredentials);
        } catch ( Exception e) {
            e.printStackTrace();
            JSONObject parameters = new JSONObject();
            parameters.put("message","WRONG CREDENTIALS OR PLATFORM NAME");
            return null;
        }

        System.out.println("ok");

        try {
            CRAMClient cramClient   = factory.getCramClient();
            RAPClient rapClient     = factory.getRapClient();
            PRClient rpcClient      = factory.getPRClient(this.platformID);
            Set<String> platformIds = new HashSet<>(Collections.singletonList(this.platformID));

            /*
             * Get the necessary component clients
             */
            PRClient searchClient = factory.getPRClient(this.platformID);

            /*
             * Create the request
             * Here, we specify just one search criteria, which is the platform id. You can add more criteria, such as
             * platform name, location, etc. You can check what the PlatformRegistryQuery.Builder supports.
             * If you specify no criteria, all the L2 resources will be returned
             */

            PlatformRegistryQuery registryQuery = new PlatformRegistryQuery.Builder()//.resourceType(resourceType)//.maxDistance(100.0)//.resourceTrust(100.0)
                    .build();

           // System.out.println("Searching the Platform Registry of platform: " + this.platformId);
            FederationSearchResult result = searchClient.search(registryQuery, false, platformIds);
            int numberOfResources = result.getResources().size();
            JSONArray jsonArray   = new JSONArray();

            for(int i = 0; i < result.getResources().size(); i++){
                FederatedResource federatedResource = result.getResources().get(i);
                String internalIdFound    = federatedResource.getCloudResource().getInternalId();

                if(internalIdFound!=null){
                    /*
                     * Check if is the one
                     * we are looking for.
                     */

                    if(internalIdFound.equals(internalID)){
                        if(federatedResource.getCloudResource().getResource()!= null)
                        return federatedResource.getCloudResource().getResource().getId();

                    }

                }

            }

           }
        catch(Exception ex){
            return null;
        }

      return null;


        //return new ResponseEntity<>("Found " + Integer.toString(numberOfResources) , new HttpHeaders(), HttpStatus.OK);
    }//end

}//end of class

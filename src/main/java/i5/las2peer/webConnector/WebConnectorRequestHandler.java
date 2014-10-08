package i5.las2peer.webConnector;

import i5.httpServer.HttpRequest;
import i5.httpServer.HttpResponse;
import i5.httpServer.RequestHandler;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;
//import rice.p2p.util.Base64;
import i5.las2peer.restMapper.exceptions.NoMethodFoundException;
import i5.las2peer.restMapper.exceptions.NotSupportedUriPathException;
import i5.las2peer.security.*;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Enumeration;


import rice.p2p.util.Base64;


/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer Web connector.
 * Each request will be distributed to its corresponding session.
 *
 * 
 */



public class WebConnectorRequestHandler implements RequestHandler {

	private static final String AUTHENTICATION_FIELD = "Authorization";
	private WebConnector connector;
	private Node l2pNode;

	
	/**
	 * Standard Constructor
	 *
	 */
	public WebConnectorRequestHandler () {		

	}
	
	
	/**
	 * set the connector handling this request processor
	 * @param connector
	 */
	public void setConnector ( WebConnector connector ) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();		
	}
	
	/**
	 * Logs in a las2peer user
	 * @param request
	 * @param response
	 * @return -1 if no successful login else userId
	 * @throws UnsupportedEncodingException
	 */
	private PassphraseAgent authenticate (HttpRequest request, HttpResponse response) throws UnsupportedEncodingException
	{
		
		final int BASIC_PREFIX_LENGTH="BASIC ".length();
		String userPass="";
		String username="";
		String password="";
		
		//Check for authentication information in header
		if(request.hasHeaderField(AUTHENTICATION_FIELD)
			&&(request.getHeaderField(AUTHENTICATION_FIELD).length()>BASIC_PREFIX_LENGTH))
		{
			//looks like: Authentication Basic <Byte64(name:pass)>
			userPass=request.getHeaderField(AUTHENTICATION_FIELD).substring(BASIC_PREFIX_LENGTH);
			userPass=new String(Base64.decode(userPass), "UTF-8");
			int separatorPos=userPass.indexOf(':');

			//get username and password
			username=userPass.substring(0,separatorPos);
			password=userPass.substring(separatorPos+1);
			
			
			return login(username,password,request,response);
			
		}//no information? check if there is a default account for login
        else if(connector.defaultLoginUser.length()>0)
        {
            return login(connector.defaultLoginUser,connector.defaultLoginPassword,request,response);
        }
		else
		{
            sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": No Authentication provided!");
		}
		return null;
	}

    private PassphraseAgent login(String username, String password, HttpRequest request, HttpResponse response)
    {
        try
        {

            long userId;
            PassphraseAgent userAgent;

            if ( username.matches ("-?[0-9].*") ) {//username is id?
                try {
                    userId = Long.valueOf(username);
                } catch ( NumberFormatException e ) {
                    throw new L2pSecurityException ("The given user does not contain a valid agent id!");
                }
            } else {//username is string
                userId = l2pNode.getAgentIdForLogin(username);
            }

			//keep track of active requests
			synchronized (this.connector)
			{

				if(this.connector.getOpenUserRequests().containsKey(userId))
				{
					Integer numReq = this.connector.getOpenUserRequests().get(userId);
					this.connector.getOpenUserRequests().put(userId,numReq+1);
					//System.out.println("### numreq " +numReq);
				}
				else
				{
					this.connector.getOpenUserRequests().put(userId,1);
					//System.out.println("### numreq 0" );
				}
			}
            userAgent = (PassphraseAgent)l2pNode.getAgent(userId);

           /* if ( ! (userAgent instanceof PassphraseAgent ))
                throw new L2pSecurityException ("Agent is not passphrase protected!");*/

			userAgent.unlockPrivateKey(password);





            return userAgent;

        }catch (AgentNotKnownException e) {
            sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": login denied for user " + username);
        } catch (L2pSecurityException e) {
            sendUnauthorizedResponse( response, null, request.getRemoteAddress() + ": unauth access - prob. login problems");
        } catch (Exception e) {

            sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": something went horribly wrong. Check your request for correctness.");
        }
        return null;
    }
	/**
	 * Delegates the request data to a service method, which then decides what to do with it (maps it internally)
	 * @param request
	 * @param response
	 * @return
	 */
	private boolean invoke(PassphraseAgent userAgent, HttpRequest request, HttpResponse response) {


        response.setStatus(HttpResponse.STATUS_INTERNAL_SERVER_ERROR); //internal server error unless otherwise specified (errors might occur)
		String[] requestSplit=request.getPath().split("/",2);
		// first: empty (string starts with '/')
		// second: URI
		
		
		
		String uri="";
		String content="";
		
		try {
			
			
			
			if(requestSplit.length>=2)
			{
				int varsstart=requestSplit[1].indexOf('?');
				if(varsstart>0)
					uri=requestSplit[1].substring(0,varsstart);
				else
					uri=requestSplit[1];
			}
			
			//http body
			content=request.getContentString();
			
			if(content==null)
				content="";
			//http method
			int httpMethodInt=request.getMethod();
			String httpMethod="get";
			
			switch (httpMethodInt) 
			{
				case HttpRequest.METHOD_GET:
					httpMethod="get";
					break;
				case HttpRequest.METHOD_HEAD:
					httpMethod="head";
					break;
				case HttpRequest.METHOD_DELETE:
					httpMethod="delete";
					break;
				case HttpRequest.METHOD_POST:
					httpMethod="post";
					break;
				case HttpRequest.METHOD_PUT:
					httpMethod="put";
					break;
				default:
					break;
			}
			
			
			
			
		
			
			ArrayList<Pair<String>> variablesList=new ArrayList<Pair<String>>();
			@SuppressWarnings("rawtypes")
			Enumeration en = request.getGetVarNames();		
			
			while(en.hasMoreElements())
			{				
				String param = (String) en.nextElement();
				String val= request.getGetVar(param);
				Pair<String> pair= new Pair<String>(param,val);	
				variablesList.add(pair);				
			}
			@SuppressWarnings("unchecked")
			Pair<String>[] variables=variablesList.toArray(new Pair[variablesList.size()]);

            ArrayList<Pair<String>> headersList=new ArrayList<Pair<String>>();


            en = request.getHeaderFieldNames();

            String acceptHeader="*/*";
            String contentTypeHeader="text/plain";
            while(en.hasMoreElements())
            {
                String param = (String) en.nextElement();

                String val= request.getHeaderField(param);
                Pair<String> pair= new Pair<String>(param,val);
                headersList.add(pair);

                //fetch MIME types
                if(param.equals("accept") && !val.trim().isEmpty())
                    acceptHeader=val.trim();
                if(param.equals("content-type")&& !val.trim().isEmpty())
                {
                    contentTypeHeader=val.trim();

                }

            }


            @SuppressWarnings("unchecked")
            Pair<String>[] headers=headersList.toArray(new Pair[headersList.size()]);

			//connector.logMessage(httpMethod+" "+request.getUrl());
			
			
			
			
			//Serializable[] parameters={httpMethod,restURI,variables,content};
			
			Serializable result="";

			Mediator mediator = l2pNode.getOrRegisterLocalMediator(userAgent);

				boolean gotResult=false;

                String returnMIMEType="text/plain";
                StringBuilder warnings = new StringBuilder();
                InvocationData[] invocation =RESTMapper.parse(this.connector.getMappingTree(), httpMethod, uri, variables, content,contentTypeHeader,acceptHeader,headers,warnings);

                if(invocation.length==0)
                {
                    response.setStatus(404);
                    if(warnings.length()>0)
                    {
                        response.setContentType( "text/plain" );

                        response.println(warnings.toString().replaceAll("\n"," "));
                    }
                    return false;
                }

                for (int i = 0; i < invocation.length; i++) {
					try
					{
						result= mediator.invoke(invocation[i].getServiceName(),invocation[i].getMethodName(), invocation[i].getParameters(), connector.preferLocalServices());// invoke service method
						gotResult=true;
                        returnMIMEType=invocation[i].getMIME();
                        break;
					
					} catch ( NoSuchServiceException | TimeoutException e ) {

                        sendNoSuchService(request, response, invocation[i].getServiceName());
					}
                    catch ( NoSuchServiceMethodException e ) {

						sendNoSuchMethod(request, response);
					} catch ( L2pSecurityException e ) {
						sendSecurityProblems(request, response, e);					
					} catch ( ServiceInvocationException e ) {

                        if ( e.getCause() == null ){
                            sendResultInterpretationProblems(request, response);
                        }else{
							sendInvocationException(request, response, e);}
					} catch ( InterruptedException e ) {
						sendInvocationInterrupted(request, response);						
						
					}
				}
				
				
				if (gotResult)
					sendInvocationSuccess ( result, returnMIMEType, response );


				
			//}
			return true;
			
		} catch ( NoMethodFoundException | NotSupportedUriPathException e ) {
			sendNoSuchMethod(request, response);	
		}
        catch (Exception e){


           // System.out.println(((UserAgent) userAgent).getLoginName());


            //e.printStackTrace();
			connector.logError("Error occured:" + request.getPath()+" "+e.getMessage() );
		}
		return false;
	}
	
	/**
	 * Logs the user out	 
	 * @param userAgent
	 */
	private void logout(PassphraseAgent userAgent)
	{
		long userId=userAgent.getId();


		//synchronize across multiple threads
		synchronized (this.connector)
		{

			if(this.connector.getOpenUserRequests().containsKey(userId))
			{
				Integer numReq = this.connector.getOpenUserRequests().get(userId);
				if(numReq<=1)
				{
					this.connector.getOpenUserRequests().remove(userId);
					try {
						l2pNode.unregisterAgent(userAgent);
						userAgent.lockPrivateKey();
						//System.out.println("+++ logout");

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				else
				{
					this.connector.getOpenUserRequests().put(userId,numReq-1);

				}
			}
			else
			{
				try {
					l2pNode.unregisterAgent(userAgent);
					userAgent.lockPrivateKey();
					//System.out.println("+++ logout");

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		//userAgent.lockPrivateKey(); //lock local copy again

		/*try {


            //if(userAgent!=null)

                //TODO check
              //  l2pNode.unregisterAgent(userAgent);
              //  userAgent.lockPrivateKey();//don't know if really necessary


		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	/**
	 * Handles a request (login, invoke)
	 */
	@Override
	public void processRequest(HttpRequest request, HttpResponse response) throws Exception {
		response.setHeaderField( "Server-Name", "LAS2peer" );
		response.setContentType( "text/xml" );
		
		
		PassphraseAgent userAgent;
		if((userAgent=authenticate(request,response))!= null)
        {
			invoke(userAgent,request,response);
            logout(userAgent);
        }
	}
	
	/**
	 * send a notification, that the requested service does not exists
	 * @param request
	 * @param response
     * @param service
	 */
	private void sendNoSuchService(HttpRequest request, HttpResponse response,
			String service) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_SERVICE_UNAVAILABLE );
		response.setContentType( "text/plain" );
		response.println ( "The service you requested is not known to this server!" );
		
		connector.logError ("Service not found: " +service);
	}
	
	/**
	 * send a notification, that the requested method does not exists at the requested service
	 * @param request
	 * @param response
	 */
	private void sendNoSuchMethod(HttpRequest request, HttpResponse response) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_NOT_FOUND );
		response.setContentType( "text/plain" );
		response.println ( "The method you requested is not known to this service!" );
		connector.logError("Invocation request " + request.getPath() + " for unknown service method");
	}
	
	/**
	 * send a notification, that security problems occurred during the requested service method
	 * @param request
	 * @param response
	 * @param e
	 */
	private void sendSecurityProblems(HttpRequest request,
			HttpResponse response, L2pSecurityException e) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_FORBIDDEN );
		response.setContentType( "text/plain" );
		response.println ( "You don't have access to the method you requested" );
		connector.logError("Security exception in invocation request " + request.getPath());
		
		if ( System.getProperty("http-connector.printSecException") != null
				&& System.getProperty( "http-connector.printSecException").equals ( "true" ) )
			e.printStackTrace();
	}
	
	/**
	 * send a notification, that the result of the service invocation is
	 * not transportable 
	 * 
	 * @param request
	 * @param response
	 */
	private void sendResultInterpretationProblems(HttpRequest request,
			HttpResponse response) {
		// result interpretation problems
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType( "text/xml" );
		response.println ("the result of the method call is not transferable!");
		connector.logError("Exception while processing RMI: " + request.getPath());
	}
	
	/**
	 * send a notification about an exception which occurred inside the requested service method
	 * 
	 * @param request
	 * @param response
	 * @param e
	 */
	private void sendInvocationException(HttpRequest request,
			HttpResponse response, ServiceInvocationException e) {
		// internal exception in service method
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType( "text/xml" );
		connector.logError("Exception while processing RMI: " + request.getPath());
		
		Object[] ret = new Object[4];
		ret[0] = "Exception during RMI invocation!";
		
		ret[1] = e.getCause().getCause().getClass().getCanonicalName();
		ret[2] = e.getCause().getCause().getMessage();
		ret[3] = e.getCause().getCause();
		String code = ret[0]+"\n"+ret[1]+"\n"+ret[2]+"\n"+ret[3];
		response.println ( code );
	}
	
	/**
	 * send a notification, that the processing of the invocation has been interrupted
	 * 
	 * @param request
	 * @param response
	 */
	private void sendInvocationInterrupted(HttpRequest request,
			HttpResponse response) {
		response.clearContent();
		response.setStatus (HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType ( "text/plain");
		response.println ( "The invoction has been interrupted!");
		connector.logError("Invocation has been interrupted!");
	}	
	
	/**
	 * 
	 * @param result
     * @param contentType
	 * @param response
	 */
	private void sendInvocationSuccess ( Serializable result, String contentType, HttpResponse response  ) {
		if ( result != null ) {
			response.setContentType( contentType );
            response.setStatus(200);
            if(result instanceof i5.las2peer.restMapper.HttpResponse)
            {

                i5.las2peer.restMapper.HttpResponse res=(i5.las2peer.restMapper.HttpResponse)result;
                Pair<String>[] headers= res.listHeaders();
                for(Pair<String> header : headers)
                {
                    response.setHeaderField(header.getOne(),header.getTwo());

                    if(header.getOne().toLowerCase().equals("content-type"))//speacial case because of the used http server lib
                    {
                        //response.clearContent();
                        response.setContentType(header.getTwo());
                    }
                }
                response.setStatus(res.getStatus() );
                response.println ( res.getResult() );

            }
            else
            {
                String resultCode =  (RESTMapper.castToString(result));
                response.println ( resultCode );
            }


			
		} else {
			response.setStatus(HttpResponse.STATUS_NO_CONTENT);
		}
	}
	
	/**
	 * send a message about an unauthorized request
	 * @param response
	 * @param logMessage
	 */
	private void sendUnauthorizedResponse(HttpResponse response, String answerMessage,
			String logMessage) {
		response.clearContent();
		response.setContentType( "text/plain" );
        response.setHeaderField("WWW-Authenticate","Basic realm=\"LAS2peer WebConnector\"");
		if ( answerMessage != null)
			response.println ( answerMessage );
		response.setStatus( HttpResponse.STATUS_UNAUTHORIZED );
		connector.logMessage ( logMessage  );
	}
	
	/**
	 * send a response that an internal error occurred
	 * 
	 * @param response
	 * @param answerMessage
	 * @param logMessage
	 */
	private void sendInternalErrorResponse(HttpResponse response,
			String answerMessage, String logMessage) {
		response.clearContent();
		response.setContentType( "text/plain" );
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.println ( answerMessage );
		connector.logMessage ( logMessage );
	}
}




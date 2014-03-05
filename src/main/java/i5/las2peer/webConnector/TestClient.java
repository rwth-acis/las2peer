package i5.las2peer.webConnector;

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

//import rice.p2p.util.Base64;
/**
 * Very simple client to communicate with the las2peer web connector
 * @author Alexander
 *
 */
public class TestClient {

			
		private String authentication;
		private String serverAddress;
		
		/**
		 * default constructor
		 */
		public TestClient()
		{
		
		}
		/**
		 * set address and port
		 * @param address address of the server
		 * @param port if 0 no port is appended to the address
		 */
		public void setAddressPort(String address, int port)
		{
			
			serverAddress=address;
			if(port>0)
			{
				serverAddress+=":"+Integer.toString(port);
			}
		}
		/**
		 * set login data
		 * @param username
		 * @param password
		 * @throws UnsupportedEncodingException
		 */
		public void setLogin(String username, String password) throws UnsupportedEncodingException
		{
			
			authentication=username+":"+password;//Base64.encodeBytes((username+":"+password).getBytes("UTF-8"));
			authentication= Base64.encodeBase64String(authentication.getBytes());
		}
		
		/**
		 * send request to server
		 * @param method POST, GET, DELETE, PUT
		 * @param uri REST-URI (server address excluded)
		 * @param content if POST is used information can be embedded here
		 * @return returns server response
		 * @throws HttpErrorException contains HTTP error code
		 */
		public String sendRequest(String method, String uri, String content) throws HttpErrorException
		{
			 	URL url;
			    HttpURLConnection connection=null;  
			    try {
			      //Create connection
			      url = new URL(String.format("%s/%s", serverAddress,uri));
			      connection = (HttpURLConnection)url.openConnection();
			      connection.setRequestMethod(method.toUpperCase());
			      connection.setRequestProperty("Authorization",
			           "Basic "+authentication);
			      
			      connection.setRequestProperty("Content-Type", 
			              "text/xml");			   			
			         connection.setRequestProperty("Content-Length", "" + 
			                  Integer.toString(content.getBytes().length));
			      connection.setUseCaches (false);
			      connection.setDoInput(true);
			      if(method.toUpperCase().equals("POST"))
			      {
			    	  connection.setDoOutput(true);
	
				      //Send request
				      DataOutputStream wr = new DataOutputStream (
				                  connection.getOutputStream ());
				      wr.writeBytes (content);
				      wr.flush ();
				      wr.close ();
			      }
			      int code = connection.getResponseCode();
			      if(code!=200)
			    	  throw new HttpErrorException(code);
			      //Get Response	
			      InputStream is = connection.getInputStream();
			      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			      String line;
			      StringBuffer response = new StringBuffer(); 
			      while((line = rd.readLine()) != null) {
			        response.append(line);
			        response.append('\r');
			      }
			      rd.close();
			      return response.toString();
			    } catch (HttpErrorException e) {
			    	throw e;
			    } catch (Exception e) {

			      e.printStackTrace();
			      return null;

			    } finally {

			      if(connection != null) {
			        connection.disconnect(); 
			      }
			    }
		}
}

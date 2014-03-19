package i5.las2peer.webConnector.client;

import java.util.HashMap;

/**
 * @author Alexander
 */
public class ClientResponse
{


    String response="";
    HashMap<String,String> headers=new HashMap<>();
    int httpCode;

    public ClientResponse(int httpCode)
    {
        this.httpCode = httpCode;
    }
    public void setResponse(String response)
    {
        this.response = response;
    }
    public String getResponse()
    {
        return response;
    }

    public HashMap<String, String> getHeaders()
    {
        return headers;
    }

    public int getHttpCode()
    {
        return httpCode;
    }
    public void addHeader(String name, String value)
    {
        headers.put(name.trim().toLowerCase(), value.trim());
    }
    public String getHeader(String name)
    {
        return headers.get(name.trim().toLowerCase());
    }
}

package i5.las2peer.restMapper;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

public class RESTMapperSwaggerTest
{

    @Test
    public void testGetResourceListing()
    {
        try
        {
            HttpResponse r = RESTMapper.getSwaggerResourceListing(SurveyService.class);
            JSONObject o = (JSONObject) JSONValue.parseWithException(r.getResult());
            
            System.out.println("Swagger Resource Listing\n================\n");
            System.out.println(o.toJSONString() + "\n");
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("failed to extract Swagger Resource Listing. Cause: " + e.getMessage() );
        }
    }
    
    @Test
    public void testGetApiDeclaration()
    {
        try
        {
        	HttpResponse r = RESTMapper.getSwaggerApiDeclaration(SurveyService.class,"surveys", "http://localhost:8080/mobsos-surveys");
           
        	JSONObject o = (JSONObject) JSONValue.parseWithException(r.getResult());
            
            System.out.println("Swagger API Declaration (surveys)\n================\n");
            System.out.println(o.toJSONString()+ "\n");
            assertTrue(true);
        }
        catch(Exception e)
        {
            fail("failed to extract Swagger API declaration. Cause: " + e.getMessage() );
        }
        
        
        
        
    }

}

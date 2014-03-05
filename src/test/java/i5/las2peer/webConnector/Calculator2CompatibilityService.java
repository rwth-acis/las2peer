package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;

@Version("0.1")
public class Calculator2CompatibilityService extends Service
{
    public String getRESTMapping()
    {
        String result="";
        try {
            result= RESTMapper.mergeXMLs(RESTMapper.readAllXMLFromDir("./XMLCompatibility2"));
        } catch (Exception e) {

            e.printStackTrace();
        }
        return result;
    }
}

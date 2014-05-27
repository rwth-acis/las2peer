package i5.las2peer.restMapper;


import static org.junit.Assert.assertEquals;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.restMapper.tools.ExampleClass;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;

import org.junit.Test;
/**
 * @author Alexander
 */
public class RESTMapperDeveloperTest
{
    static PathTree tree;



    @Test
    public void testValidator1()
    {
        String xml="123";
        try
        {
            xml= RESTMapper.getMethodsAsXML(ExampleClass.class);
            //xml=RESTMapper.getFile(new File("./XMLOutput/xml1.xml"));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        XMLCheck validator= new XMLCheck();
        ValidationResult result = validator.validate(xml);

        assertEquals(result.getMessage(),true,result.isValid());
    }

    @Test
    public void testValidator2()
    {
        String xml="123";
        try
        {
            xml= RESTMapper.getMethodsAsXML(TestClassBad1.class);
            //xml=RESTMapper.getFile(new File("./XMLOutput/xml1.xml"));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        XMLCheck validator= new XMLCheck();
        ValidationResult result = validator.validate(xml);

        assertEquals(result.getMessage(),false,result.isValid());
    }

    @Test
    public void testValidator3()
    {
        String xml="123";
        try
        {
            xml= RESTMapper.getMethodsAsXML(TestClassBad2.class);
            //xml=RESTMapper.getFile(new File("./XMLOutput/xml1.xml"));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        XMLCheck validator= new XMLCheck();
        ValidationResult result = validator.validate(xml);

        assertEquals(result.getMessage(),false,result.isValid());
    }



}

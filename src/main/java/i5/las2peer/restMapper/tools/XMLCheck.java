package i5.las2peer.restMapper.tools;

import i5.las2peer.restMapper.RESTMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/**
 * @author Alexander
 */
public class XMLCheck
{
//    private Document doc;
//    private DocumentBuilderFactory dbFactory;
//    private DocumentBuilder dBuilder;

    private SchemaFactory factory;
    private Schema schema;
    private Validator validator;
    public static final String XSD_FILE="/validation/restschema.xsd";
//    private XPath xPath = XPathFactory.newInstance().newXPath();
    public XMLCheck()
    {


        try
        {
//            dbFactory=DocumentBuilderFactory.newInstance();
//            dBuilder=dbFactory.newDocumentBuilder();
            factory =SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            //schema = factory.newSchema(new StreamSource(new StringReader(RESTMapper.getFile(new File("./xsd/validation/restschema.xsd")))));
            try
            {
                InputStream xsd= XMLCheck.class.getResourceAsStream(XSD_FILE);
                schema = factory.newSchema(new StreamSource(xsd));
            }
            catch (Exception e)
            {

                try
                {
                    schema = factory.newSchema(new StreamSource(new StringReader(RESTMapper.getFile(new File("./xsd"+XSD_FILE)))));
                }
                catch(IOException e1)
                {
                    e1.printStackTrace();
                }
            }

            validator = schema.newValidator();
        }
        catch(SAXException e)
        {
            e.printStackTrace();
        }
      /*  catch(IOException e)
        {
            e.printStackTrace();
        }*/
//        catch(ParserConfigurationException e)
//        {
//            e.printStackTrace();
//        }
    }

    public ValidationResult validate(String xml)
    {

        ValidationResult result = new ValidationResult();
        try
        {
            xml=xml.replaceFirst("<services ", "<services xmlns=\"RESTMapper/check\" ");
            xml=xml.replaceFirst("<service ", "<service xmlns=\"RESTMapper/check\" ");
            validator.validate(new StreamSource(
                    new StringReader(xml)));
            result.setValid(true);

            ValidationResult pathResult=checkPathAnnotations(xml);

            if(!pathResult.isValid())
            {
                result.setValid(false);
                result.addMessage(pathResult.getMessage());
            }

        }
        catch(Exception e)
        {

            result.setValid(false);
            result.setMessage(e.getMessage());
        }

        return result;

    }

    private ValidationResult checkPathAnnotations(String xml)
    {

        ValidationResult result=new ValidationResult();
        try
        {
            RESTMapper.getMappingTree(xml,true,result);


        }
        catch(Exception e)
        {

            result.setValid(false);
            result.setMessage(e.getMessage());
        }

        return result;
    }
}

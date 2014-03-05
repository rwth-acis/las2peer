package i5.las2peer.webConnector;

import java.io.File;
import java.io.FileReader;

import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import i5.las2peer.api.Service;

public class RESTCompatibilityService extends Service {
	
	private ArrayList<File> files = new ArrayList<File>();
	private DocumentBuilderFactory dbFactory;
	private DocumentBuilder dBuilder;
	private Document doc;
	private final String DEFAULT_DIR="./XMLCompatibility";
	private String dir=DEFAULT_DIR;
    private void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                files.add(fileEntry);
            }
        }
    }
	
	
	public RESTCompatibilityService(){
		
	}
	public RESTCompatibilityService(String dir){
		this.dir=dir;
	}
	private void createXMLInfo() {
		dbFactory = DocumentBuilderFactory.newInstance();
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc=dBuilder.newDocument();
			Element root=doc.createElement("services");
			doc.appendChild(root);
		} catch (ParserConfigurationException e) {
		
			e.printStackTrace();
		}
		
		for (File file : files) {
			try {
				//String xml=getFile(file);
				Document local=dBuilder.parse(new InputSource(new FileReader(file)));			
				
				doc.getDocumentElement().appendChild(doc.importNode(local.getDocumentElement(), true));
			} catch (Exception e) {				
				e.printStackTrace();
			}
		}
		doc.getDocumentElement().normalize();
		
	}
	public String getServiceXML(){
		File folder = new File(dir);
		listFilesForFolder(folder);
		createXMLInfo();
		return toString();
	}
	
	public String toString()
	{
		if(doc!=null)
		{			
			try
			{				
				Transformer t = TransformerFactory.newInstance().newTransformer();
				StreamResult out = new StreamResult(new StringWriter());
				t.setOutputProperty(OutputKeys.INDENT, "yes"); //pretty printing
				t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
				t.transform(new DOMSource(doc),out);
				return out.getWriter().toString();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return "";
			}
			
		}
		else
			return "";
	}
}

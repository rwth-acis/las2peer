package i5.las2peer.restMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.restMapper.data.PathTree;

import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

public class RESTMapperTest {

	

	
	static PathTree tree;
	static PathTree tree2;
    static PathTree tree3;
    static PathTree tree4;
    static PathTree tree5;
	@BeforeClass
	public static void testSetup() 
	{			
		
		String xml;
		String xml2;
        String xml3;
        String xml4;
        String xml5;
		try {
			//tree=mapper.getMappingTree(mapper.getMethodsAsXML(TestClass1.class));
			xml=RESTMapper.getMethodsAsXML(TestClass1.class);
			xml2=RESTMapper.getMethodsAsXML(TestClass2.class);
            xml3=RESTMapper.getMethodsAsXML(TestClass3.class);
            xml4=RESTMapper.getMethodsAsXML(TestClass4.class);
            xml5=RESTMapper.getMethodsAsXML(TestClass5.class);
			//System.out.println(xml);
			tree=RESTMapper.getMappingTree(xml);
			tree2=RESTMapper.getMappingTree(xml2);
            tree3=RESTMapper.getMappingTree(xml3);
            tree4=RESTMapper.getMappingTree(xml4);
            tree5=RESTMapper.getMappingTree(xml5);

			tree.merge(tree2);
            tree.merge(tree3);
            tree.merge(tree4);
//            String s = tree.merge(tree5);


            //System.out.println(RESTMapper.mergeXMLs(new String[]{xml,xml2}));

        } catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	public void invokeMethod(String httpMethod, String uri, Pair<String>[] variables, String content, String contentType, String returnType, Pair<String>[] httpHeaders,  String assertionMessage, String assertion) throws Exception
	{

            StringBuilder warnings=new StringBuilder();
			InvocationData[] invocation =RESTMapper.parse(tree, httpMethod, uri, variables, content, contentType, returnType,  httpHeaders, warnings);
            //if (warnings.length()>0) System.out.println(warnings.toString());
            if (invocation.length==0)
                throw new Exception("no method found for " +assertionMessage);
            for(InvocationData anInvocation : invocation)
            {
                Object[] parameters = anInvocation.getParameters();
                Class<?>[] parameterTypes = anInvocation.getParameterTypes();

                //System.out.println(invocation[i].getServiceName());
                //System.out.println(invocation[i].getMethodName());
                Class<?> clazz = Class.forName(anInvocation.getServiceName());
                Method method = clazz.getMethod(anInvocation.getMethodName(), parameterTypes);

                Object obj = clazz.newInstance();
                Object result = method.invoke(obj, parameters);
                String r = RESTMapper.castToString(result);
                assertEquals(assertionMessage, assertion, r);

                //System.out.println(r);
                //System.out.println("__");
                break;//take only first
            }

    }

    public void invokeMethod(String httpMethod, String uri, Pair<String>[] variables, String content,String contentType, Pair<String>[] httpHeaders, String assertionMessage, String assertion) throws Exception
    {

        invokeMethod(httpMethod,uri,variables,content,contentType, "",httpHeaders,assertionMessage,assertion);
    }
    public void invokeMethod(String httpMethod, String uri, Pair<String>[] variables, String content, Pair<String>[] httpHeaders, String assertionMessage, String assertion) throws Exception
    {

        invokeMethod(httpMethod,uri,variables,content,"", "",httpHeaders,assertionMessage,assertion);
    }


	
	@SuppressWarnings("unchecked")
	@Test
	public void testA1() {
		try {
			invokeMethod("get","",new Pair[]{},"",new Pair[]{},"a1","true");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA2(){
		
		try {
			invokeMethod("put","/users/205",new Pair[]{},"",new Pair[]{},"a2","20.5");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA3(){
		try {
			invokeMethod("delete","/users/12/products/5/true",new Pair[]{},"",new Pair[]{},"a3","60true");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA4(){
		try {
			invokeMethod("post","hi/b/c",new Pair[]{new Pair<String>("d","12"),new Pair<String>("e","4")},"",new Pair[]{},"a4","hi16");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testA4_default(){
		try {

           invokeMethod("post","hi/b/c",new Pair[]{},"",new Pair[]{},"a4_default","hi24");


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA4_ex(){
		
		try	{
			invokeMethod("post","hi/b/",new Pair[]{new Pair<String>("d","12"),new Pair<String>("e","4")},"",new Pair[]{},"a4_ex","hi9");
			
			fail("Wrong path, no exception");
		}
		catch (Exception e)	{
			//e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testA5(){
		try	{
			invokeMethod("get","a",new Pair[]{},"t",new Pair[]{},"a5","tt");
		}
		catch (Throwable e)	{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testA6(){
		try	{
			invokeMethod("put","d/c/b/1",new Pair[]{},"",new Pair[]{},"a6","dcb1");
			
		}
		catch (Throwable e)	{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMerge1(){
		try	{
			invokeMethod("put","visitors/12",new Pair[]{},"",new Pair[]{},"Merge1","2.4");
			
		}
		catch (Throwable e)	{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testMerge2(){
		try	{
			invokeMethod("delete","users/12/cart/13/2.33",new Pair[]{},"",new Pair[]{},"Merge2","1562.33");
			
		}
		catch (Throwable e)	{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
    @SuppressWarnings("unchecked")
    @Test
    public void testHeaders1(){
        try	{
            invokeMethod("delete","users/12",new Pair[]{},"",new Pair[]{new Pair<String>("productID","12")},"Headers1","1440.0");

        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    @SuppressWarnings("unchecked")
    @Test
    public void testHeaders2(){
        try	{
            invokeMethod("delete","users/44/4",new Pair[]{},"",new Pair[]{new Pair<String>("a","12"),new Pair<String>("b","13")},"Headers1","44a: 12\nb: 13\n");

        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClassPath1(){
        try	{
            invokeMethod("get","animals/4/food/6",new Pair[]{},"",new Pair[]{},"ClassPath1","10");

        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClassPath2(){
        try	{
            invokeMethod("delete","animals/7",new Pair[]{},"",new Pair[]{},"ClassPath2","7");

        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConsumes1(){
        try	{
            invokeMethod("post","animals/3",new Pair[]{},"", MediaType.TEXT_PLAIN,new Pair[]{},"Consumes1","6");
            invokeMethod("post","animals/3",new Pair[]{},"", MediaType.TEXT_XML,new Pair[]{},"Consumes1","15");
            invokeMethod("post","animals/3",new Pair[]{},"", MediaType.AUDIO_MPEG,new Pair[]{},"Consumes1","21");

        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConsumes2(){
        try	{
            invokeMethod("post","animals/3",new Pair[]{},"", MediaType.VIDEO_AVI,new Pair[]{},"Consumes2","33"); //test for wildcards


        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConsumes3(){
        try	{
            invokeMethod("post","animals/3",new Pair[]{},"", MediaType.APPLICATION_EDIFACT,new Pair[]{},"Consumes3","33");
            fail("testConsumes3 should throw Exception");

        }
        catch (Throwable e)	{


        }
    }

    @Test
    public void testAcceptHeaderSorting(){ //see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html  14.1 Accept
        try	{
            String accept="text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5,audio/*";
            String[] result=RESTMapper.getAcceptedTypes(accept);
            assertEquals("Wrong Accept header sorting","text/html;level=1 audio/\\w+ text/html \\w+/\\w+ text/html;level=2 text/\\w+", RESTMapper.join(result," "));

        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProduces1(){
        try	{
            invokeMethod("get","books/4",new Pair[]{},"", MediaType.VIDEO_AVI,"audio/*,audio/ogg" ,new Pair[]{},"Produces1","8");
            invokeMethod("get","books/4",new Pair[]{},"", MediaType.VIDEO_AVI,"video/mp4,text/*" ,new Pair[]{},"Produces1","4");


        }
        catch (Throwable e)	{
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProduces2(){
        try	{
            invokeMethod("get","books/4",new Pair[]{},"", MediaType.VIDEO_AVI,"video/mp4,text/xml" ,new Pair[]{},"Produces2","4");
            fail("testProduces2 should throw Exception");

        }
        catch (Throwable e)	{


        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPut()
    {

        try
        {
            invokeMethod("put","books/5",new Pair[]{},"asdf",new Pair[]{},"PUT","asdf");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


}

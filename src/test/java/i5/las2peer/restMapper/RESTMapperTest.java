package i5.las2peer.restMapper;

import static org.junit.Assert.*;


import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.restMapper.data.PathTree;

import java.lang.reflect.Method;



import org.junit.BeforeClass;
import org.junit.Test;

public class RESTMapperTest {

	
	TestClass1 testClass1= new TestClass1();
	static RESTMapper mapper;
	static PathTree tree;
	@BeforeClass
	public static void testSetup() 
	{			
		mapper= new RESTMapper();
		String xml="";
		try {
			//tree=mapper.getMappingTree(mapper.getMethodsAsXML(TestClass1.class));
			xml=mapper.getMethodsAsXML(TestClass1.class);
			//System.out.println(xml);
			tree=mapper.getMappingTree(xml);
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	
	public void invokeMethod(String httpMethod, String uri, Pair<String>[] variables, String content, String assertionMessage, String assertion) throws Exception
	{
		
			InvocationData[] invocation =mapper.parse(tree, httpMethod, uri, variables, content);
			for (int i = 0; i < invocation.length; i++) {
				Object[] parameters= invocation[i].getParameters();
				Class<?> [] parameterTypes =invocation[i].getParameterTypes();
				
				//System.out.println(invocation[i].getServiceName());
				//System.out.println(invocation[i].getMethodName());
				Class<?> clazz=Class.forName(invocation[i].getServiceName());
				Method method=clazz.getMethod(invocation[i].getMethodName(), parameterTypes);
				
				Object obj=clazz.newInstance();
				Object result=method.invoke(obj, parameters);
				String r=RESTMapper.castToString(result);
				assertEquals(assertionMessage,assertion,r);
				//System.out.println(r);
				//System.out.println("__");
			}
			
		
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA1() {
		try {
			invokeMethod("get","",new Pair[]{},"","a1","true");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA2(){
		
		try {
			invokeMethod("put","/users/205",new Pair[]{},"","a2","20.5");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA3(){
		try {
			invokeMethod("delete","/users/12/products/5/true",new Pair[]{},"","a3","60true");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA4(){
		try {
			invokeMethod("post","hi/b/c",new Pair[]{new Pair<String>("d","12"),new Pair<String>("e","4")},"","a4","hi16");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testA4_default(){
		try {
			invokeMethod("post","hi/b/c",new Pair[]{},"","a4","hi9");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testA4_ex(){
		
		try	{
			invokeMethod("post","hi/b/",new Pair[]{new Pair<String>("d","12"),new Pair<String>("e","4")},"","a4","hi9");
			
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
			invokeMethod("get","a",new Pair[]{},"t","a5","tt");			
		}
		catch (Throwable e)	{
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testA6(){
		try	{
			invokeMethod("put","d/c/b/1",new Pair[]{},"","a6","dcb1");			
			
		}
		catch (Throwable e)	{
			e.printStackTrace();
		}
	}
	
}

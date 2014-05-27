package i5.las2peer.restMapper.tools;

import i5.las2peer.restMapper.RESTMapper;

import java.io.File;
import java.io.IOException;
/**
 * @author Alexander
 */
public class CommandLine
{
    public static void main(String args[])
    {
        String className;
        String output;
        String input;
        String command;
        if (args.length>0)
        {
            command=args[0].toLowerCase();


            if(command.equals("create")&&args.length==3)
            {
                className=args[1];
                output=args[2];
                try
                {
                    Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(className);
                    String xml= RESTMapper.getMethodsAsXML(cls);
                    RESTMapper.writeFile(output,xml);

                }
                catch(ClassNotFoundException e)
                {
                    System.out.println("Class " + className + " not found. " + e.getMessage());
                }
                catch(IOException e)
                {
                    System.out.println("Could not write to file: " + e.getMessage());
                }
                catch(Exception e)
                {
                    System.out.println("An error occurred: " + e.getMessage());
                }
            }
            else if(command.equals("validate")&&args.length==2)
            {
                input=args[1];
                String xml="123";
                try
                {
                    xml=RESTMapper.getFile(new File(input));
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }

                XMLCheck validator= new XMLCheck();
                ValidationResult result = validator.validate(xml);

                if(result.isValid())
                    System.out.println("valid!");
                else
                    System.out.println(result.getMessage());

            }
            else
            {
                printUsage();
            }






        }
        else
        {
            printUsage();
        }

    }

    private static void printUsage()
    {
        System.out.println("Usage: create <className> <output>");
        System.out.println("Usage: validate <xmlFile>");
    }
}

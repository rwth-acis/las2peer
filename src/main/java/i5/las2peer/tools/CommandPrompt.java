package i5.las2peer.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;

import i5.las2peer.communication.ListMethodsContent;
import i5.las2peer.tools.ColoredOutput.ForegroundColor;

/**
 * A simple command line for executing commands, generating new instances and handling local variables.
 * 
 */
public class CommandPrompt {

	/**
	 * Enumeration for different return status codes of executed lines.
	 * 
	 */
	public enum ReturnStatus {
		OK_NOTHING_TO_DO(30),
		OK_PROCEED(10),
		OK_EXIT(0),
		ERROR_PROCEED(20),
		ERROR_EXIT(-10),
		NOT_KNOWN_PROCEED(100),
		NOT_KNOWN_EXIT(-100);

		private int value;

		private ReturnStatus(int value) {
			this.value = value;
		}

		public boolean proceed() {
			return value > 0;
		}
	}

	/**
	 * counter for the current line number
	 * 
	 */
	private int lineNumber = 0;

	/**
	 * an object to run methods on with out given variable (local instance name)
	 * 
	 */
	private Object boundTo = null;

	/**
	 * the first set bound object
	 * 
	 */
	private Object firstBoundToObject = null;

	/**
	 * a package to use with classes
	 * 
	 */
	private String packagePrefix = "";

	/**
	 * storage for local variables (instances)
	 * 
	 */
	private Hashtable<String, Object> htLocals = new Hashtable<String, Object>();

	/**
	 * reader for the input
	 * 
	 */
	private BufferedReader input;

	/**
	 * last exception that occurred while executing a command
	 * 
	 */
	private Throwable lastCommandException = null;

	/**
	 * the execution line corresponding to {@link #lastCommandException}
	 * 
	 */
	private String exceptionLine = null;

	/**
	 * create a new unbound prompt
	 * 
	 */
	public CommandPrompt() {
		this(null);
	}

	/**
	 * create a new prompt bound to the given object
	 * 
	 * @param boundTo
	 */
	public CommandPrompt(Object boundTo) {
		this.boundTo = boundTo;
		firstBoundToObject = boundTo;

		input = new BufferedReader(new InputStreamReader(System.in));
	}

	/**
	 * handle an assignment command
	 * 
	 * @param line
	 * @return status code
	 */
	public ReturnStatus handleAssignment(String line) {
		String[] split = line.split("=", 2);
		String local = split[0].trim();
		String valueString = split[1].trim();

		try {
			split = valueString.trim().split("\\s+");

			Object value;
			if (split[0].equals("new")) {
				value = handleNew(valueString);
			} else {
				value = handleCommand(valueString);
			}

			htLocals.put(local, value);

			System.out.println(
					ColoredOutput.colorize("   -> " + local + " set to '" + value + "'", ForegroundColor.Green));

			return ReturnStatus.OK_PROCEED;
		} catch (ClassNotFoundException e) {
			System.out.println(ColoredOutput.colorize("  Class not found!", ForegroundColor.Red));
		} catch (NoSuchMethodException e) {
			System.out.println(ColoredOutput.colorize("  Method or Constructur not found!", ForegroundColor.Red));
		} catch (Exception e) {
			System.out.println(ColoredOutput.colorize("  Error: " + e.getMessage(), ForegroundColor.Red));
			e.printStackTrace();
		}
		return ReturnStatus.ERROR_PROCEED;
	}

	/**
	 * extract parameters enclosed in parentheses from the given string
	 * 
	 * simple strings will be interpreted as names of local variables and replaced with them, strings enclosed in " or '
	 * will be used as String values
	 * 
	 * @param value
	 * @return array with parameter values
	 * @throws Exception
	 */
	private Object[] getParameters(String value) throws Exception {

		if (value.contains("(") || value.contains(")")) {
			if (!value.contains("(") || !value.contains(")")) {
				throw new Exception("parentheses mismatch!");
			}

			String parameterString = value.substring(value.indexOf("(") + 1, value.lastIndexOf(")"));
			if (parameterString.isEmpty()) {
				// only parantheses with no arguments given
				return new String[0];
			}

			String[] split = parameterString.trim().split("\\s*,\\s*");
			Object[] result = new Object[split.length];

			for (int i = 0; i < split.length; i++) {
				if ((split[i].charAt(0) == '"' && split[i].charAt(split[i].length() - 1) == '"')
						|| (split[i].charAt(0) == '\'' && split[i].charAt(split[i].length() - 1) == '\''))
					result[i] = split[i].substring(1, split[i].length() - 1);
				else {
					result[i] = htLocals.get(split[i]);
					System.out.println("  " + split[i] + ": " + result[i]);
				}
			}
			return result;
		} else
			return new String[0];

	}

	/**
	 * handle a NEW command, e.g. try to create a new instance of the given class with the possibly given parameters
	 * 
	 * @param command
	 * @return result of the new instantiation
	 * @throws Exception
	 */
	public Object handleNew(String command) throws Exception {
		String value = command.trim().substring(4).trim();

		Object[] parameters = getParameters(value);

		String cls;
		if (value.contains("("))
			cls = value.substring(0, value.indexOf("(")).trim();
		else
			cls = value.trim();

		try {
			return createInstance(cls, parameters);
		} catch (ClassNotFoundException e) {
			System.out.println("   not found: " + cls);
			if (packagePrefix != null && !packagePrefix.isEmpty()) {
				return createInstance(packagePrefix + "." + cls, parameters);
			} else
				throw new ClassNotFoundException("class " + cls + " not found");
		}
	}

	/**
	 * create an instance of the given class using the given string parameters for the constructor call
	 * 
	 * @param className
	 * @param parameters
	 * @return result of the constructor call
	 * @throws Exception
	 */
	private Object createInstance(String className, Object[] parameters) throws Exception {
		Class<?> cls = Class.forName(className);

		if (parameters == null || parameters.length == 0)
			return cls.newInstance();

		Class<?>[] types = getParameterTypes(parameters);

		Constructor<?> constr = cls.getConstructor(types);
		return constr.newInstance((Object[]) parameters);
	}

	/**
	 * get an array with the classes out of the given array of objects
	 * 
	 * @param parameters
	 * @return array with the classes of the given object array
	 */
	private Class<?>[] getParameterTypes(Object[] parameters) {
		Class<?>[] types = new Class<?>[parameters.length];
		for (int i = 0; i < types.length; i++)
			if (parameters[i] == null)
				types[i] = null;
			else
				types[i] = parameters[i].getClass();
		return types;
	}

	/**
	 * handle a command, i.e. a static method or a class method of a local variable or the bound object
	 * 
	 * @param command
	 * @return result of the invoked operation
	 * @throws Exception
	 */
	public Object handleCommand(String command) throws Exception {
		Object[] parameters = getParameters(command);

		String cmd;
		if (command.contains("("))
			cmd = command.substring(0, command.indexOf("(")).trim();
		else
			cmd = command;

		if (cmd.contains(".")) {
			String firstPart = cmd.substring(0, cmd.lastIndexOf("."));
			String method = cmd.substring(cmd.lastIndexOf(".") + 1);

			if (htLocals.containsKey(firstPart)) {
				return executeMethod(htLocals.get(firstPart), method, parameters);
			} else {
				return executeStatic(firstPart, method, parameters);
			}
		} else {
			if (boundTo == null)
				System.out.println(ColoredOutput.colorize("  bound is not set -> unable to execute methods on it!",
						ForegroundColor.Red));

			return executeMethod(boundTo, cmd, parameters);
		}
	}

	/**
	 * execute a method
	 * 
	 * @param on
	 * @param method
	 * @param parameters
	 * @return result of the invoked operation
	 * @throws Exception
	 * @throws NoSuchMethodException
	 */
	private Object executeMethod(Object on, String method, Object[] parameters) throws Exception {
		Class<?>[] types = getParameterTypes(parameters);

		Method m = null;
		try {
			m = on.getClass().getMethod(method, types);
		} catch (NoSuchMethodException e) {
			Method[] methods = on.getClass().getMethods();

			int lauf = 0;
			boolean hasMethodOfName = false;
			while (m == null && lauf < methods.length) {
				if (methods[lauf].getName().equals(method))
					hasMethodOfName = true;

				if (methodMatches(methods[lauf], method, parameters))
					m = methods[lauf];
				lauf++;
			}

			if (m == null) {
				if (hasMethodOfName)
					throw new NoSuchMethodException("No signature of " + method + " on " + on.getClass().getSimpleName()
							+ " matches the given parameters");
				else
					throw new NoSuchMethodException(on.getClass().getSimpleName() + " has no method '" + method + "'");
			}

		}

		if (Modifier.isStatic(m.getModifiers())) {
			System.out.println(ColoredOutput.colorize("  warning: this method is static - executing anyway",
					ForegroundColor.Yellow));
			return executeStatic(on.getClass(), method, parameters);
		} else
			return m.invoke(on, parameters);

	}

	/**
	 * check, if the given method matches the name and parameter types
	 * 
	 * @param method
	 * @param methodName
	 * @param parameters
	 * @return true on a match
	 */
	private boolean methodMatches(Method method, String methodName, Object[] parameters) {

		if (!method.getName().equals(methodName))
			return false;

		Class<?>[] types = getParameterTypes(parameters);

		Class<?>[] methodParameterTypes = method.getParameterTypes();
		if (methodParameterTypes.length != types.length)
			return false;

		for (int lauf = 0; lauf < types.length; lauf++)
			if (types[lauf] == null || !methodParameterTypes[lauf].isAssignableFrom(types[lauf]))
				return false;

		return true;

	}

	/**
	 * execute a static method
	 * 
	 * 
	 * @param className
	 * @param method
	 * @param parameters
	 * @return result of the static method invocation
	 * @throws Exception
	 */
	private Object executeStatic(String className, String method, Object[] parameters) throws Exception {

		Class<?> cls = null;

		try {
			if (packagePrefix != null && !packagePrefix.isEmpty())
				cls = Class.forName(packagePrefix + "." + className);
		} catch (ClassNotFoundException e) {
		}

		if (cls == null)
			cls = Class.forName(className);

		return executeStatic(cls, method, parameters);
	}

	/**
	 * execute a static method of a given class
	 * 
	 * @param cls
	 * @param method
	 * @param parameters
	 * @return result of the static method invocation
	 * @throws Exception
	 */
	private Object executeStatic(Class<?> cls, String method, Object[] parameters) throws Exception {
		Class<?>[] types = getParameterTypes(parameters);

		Method m = cls.getMethod(method, types);

		if (!Modifier.isStatic(m.getModifiers()))
			throw new NoSuchMethodException("the given method is not static!");

		return m.invoke(null, parameters);
	}

	/**
	 * handle a line of input
	 * 
	 * @param line
	 * @return return status code
	 */
	public ReturnStatus handleLine(String line) {
		lineNumber++;

		if (line.trim().startsWith("//") || line.trim().startsWith("#"))
			return ReturnStatus.OK_NOTHING_TO_DO;

		ReturnStatus status;
		if (line.contains("="))
			status = handleAssignment(line);
		else
			status = localCommand(line);

		if (status == ReturnStatus.NOT_KNOWN_PROCEED) {
			try {
				Object result = handleCommand(line);
				System.out.println(ColoredOutput.colorize("  ok  ", ForegroundColor.Green));
				printResult("\tresult:", result);
				status = ReturnStatus.OK_PROCEED;
			} catch (NoSuchMethodException e) {
				System.err.println(ColoredOutput.colorize(e.toString(), ForegroundColor.Red));
				status = ReturnStatus.ERROR_PROCEED;
			} catch (InvocationTargetException e) {
				System.err.println(ColoredOutput.colorize("  --> Exception in executed method!", ForegroundColor.Red));
				System.err.println(ColoredOutput.colorize(
						"      " + e.getClass().getSimpleName() + " (" + e.getCause().getMessage() + ")",
						ForegroundColor.Red));
				System.err.println(ColoredOutput.colorize("      (print StackTrace with pst / printStackTrace)",
						ForegroundColor.LightGrey));

				lastCommandException = e.getCause();
				exceptionLine = line;
				status = ReturnStatus.ERROR_PROCEED;
			} catch (Exception e) {
				System.err.println(
						ColoredOutput.colorize("  --> Exception during command execution!", ForegroundColor.Red));
				System.err.println(ColoredOutput.colorize(
						"      " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")", ForegroundColor.Red));
				System.err.println(ColoredOutput.colorize("      (print StackTrace with pst / printStackTrace)",
						ForegroundColor.LightGrey));

				lastCommandException = e;
				exceptionLine = line;
				status = ReturnStatus.ERROR_PROCEED;
			}
		}

		if (status == ReturnStatus.NOT_KNOWN_PROCEED) // TODO this will never be reached
			System.out.println(ColoredOutput.colorize("   -> command '" + line + "' not known", ForegroundColor.Red));

		return status;
	}

	/**
	 * print the result of an executed method
	 * 
	 * @param string
	 * @param result
	 */
	private void printResult(String string, Object result) {
		System.out.print(string + "\t");
		printResult(result, 1);
	}

	/**
	 * print the last caught exception / throwable
	 */
	public void printException() {
		if (lastCommandException == null) {
			System.out.println("No exception stored!");
			return;
		} else {
			System.out.print("exception in executed line: ");
			System.out.println(ColoredOutput.colorize(exceptionLine + "\n", ForegroundColor.Red));
			lastCommandException.printStackTrace(System.out);
			System.out.println("\n\n");
		}
	}

	/**
	 * print the result of an executed method - unfold arrays recursively
	 * 
	 * @param result
	 * @param level
	 */
	private void printResult(Object result, int level) {
		String tab = SimpleTools.repeat("\t", level + 1);

		if (result == null)
			System.out.println(" /null/");
		else if (result.getClass().isArray()) {
			System.out
					.println("Array (" + ((Object[]) result).length + ", " + result.getClass().getSimpleName() + ") :");

			for (int i = 0; i < Math.min(10, ((Object[]) result).length); i++) {
				System.out.print(tab);
				printResult(((Object[]) result)[i], level + 1);
			}
		} else {
			System.out.println(result.toString().replaceAll("\\n", tab.toString() + "\n"));
		}
	}

	/**
	 * print the value of all given local variables
	 * 
	 * @param line
	 * 
	 * @return return status code
	 */
	public ReturnStatus handlePrint(String line) {
		String[] split = line.trim().split("\\s+");

		ReturnStatus status = ReturnStatus.OK_PROCEED;
		// [0] is 'print' itself
		for (int i = 1; i < split.length; i++) {
			if (htLocals.containsKey(split[i])) {
				System.out.println("   " + split[i] + ": " + htLocals.get(split[i]) + " ("
						+ htLocals.get(split[i]).getClass().getCanonicalName() + ")");
			} else
				System.out.println(
						ColoredOutput.colorize("  --> local var '" + split[i] + "' not known", ForegroundColor.Red));
		}
		System.out.println("");
		return status;
	}

	/**
	 * check, if the given line contains (starts with) a local command and execute it.
	 * 
	 * 
	 * @param line
	 * 
	 * @return return status code
	 */
	public ReturnStatus localCommand(String line) {
		String[] split = line.trim().split("\\s+");

		if (split[0].trim().equals("quit") || split[0].trim().equals("exit") || split[0].trim().equals("q")) {
			System.out.println(" -> Ok, normal exit! ");
			return ReturnStatus.OK_EXIT;
		} else if (split[0].equals("print") || split[0].equals("p")) {
			return handlePrint(line);
		} else if (split[0].equals("bind")) {
			if (split.length == 1) {
				boundTo = firstBoundToObject;
				return ReturnStatus.OK_PROCEED;
			}
			if (split.length != 2) {
				System.out.println(ColoredOutput.colorize("  -> usage: bind [local var name]", ForegroundColor.Red));
				return ReturnStatus.ERROR_PROCEED;
			}

			if (htLocals.containsKey(split[1])) {
				boundTo = htLocals.get(split[1]);
				return ReturnStatus.OK_PROCEED;
			} else {
				System.out.println(
						ColoredOutput.colorize("  -> local var '" + split[1] + "' not known!", ForegroundColor.Red));
				return ReturnStatus.ERROR_PROCEED;
			}
		} else if (split[0].equals("package")) {
			if (split.length == 1)
				packagePrefix = "";
			else if (split.length > 2) {
				System.out.println(
						ColoredOutput.colorize("  -> usage: package [used package prefix]", ForegroundColor.Red));
				return ReturnStatus.ERROR_PROCEED;
			} else
				packagePrefix = split[1].trim();

			return ReturnStatus.OK_PROCEED;
		} else if (split[0].equals("list") || split[0].equals("l")) {
			if (boundTo == null) {
				System.out.println(ColoredOutput.colorize("  -> I'm not bound to an instance, what should I list?",
						ForegroundColor.Red));
				return ReturnStatus.ERROR_PROCEED;
			} else {
				printMethodsOfBound(boundTo);
				return ReturnStatus.OK_PROCEED;
			}
		} else if (split[0].equals("pst") || split[0].equals("printStackTrace")) {
			printException();
			return ReturnStatus.OK_PROCEED;
		} else if (split[0].equals("?") || split[0].equals("help")) {
			printHelp();
			return ReturnStatus.OK_PROCEED;
		}

		return ReturnStatus.NOT_KNOWN_PROCEED;
	}

	/**
	 * list all (public) methods of an object
	 * 
	 * @param object
	 */
	private void printMethodsOfBound(Object object) {
		ListMethodsContent lmc = new ListMethodsContent(false);
		for (Method m : object.getClass().getMethods())
			lmc.addMethod(m);

		for (String methodName : lmc.getSortedMethodNames()) {
			boolean namePrinted = false;

			for (String[] paramList : lmc.getSortedMethodParameters(methodName)) {
				if (namePrinted) {
					System.out.print(SimpleTools.repeat(" ", methodName.length() + 4) + "(");
				} else {
					System.out.print(" * " + methodName + " (");
					namePrinted = true;
				}

				System.out.println(SimpleTools.join(paramList, ", ") + ")");

			}
		}

		/*
		Method[] methods = object.getClass().getMethods();
		
		Hashtable<String, HashSet<String[]>> htMethodnamesToParameters = new Hashtable<String, HashSet<String[]>> ();
		for ( Method m: methods ) {
			if ( ! Modifier.isPublic ( m.getModifiers()) || Modifier.isStatic(m.getModifiers()) )
				continue;
			
			HashSet<String[]> multiple = htMethodnamesToParameters.get( m.getName() );
			if (multiple == null) {
				multiple = new HashSet<String[]> ();
				htMethodnamesToParameters.put(m.getName(), multiple);
			}
			
			Class<?>[] paramTypes = m.getParameterTypes();
			String[] paramClassNames = new String[ paramTypes.length ];
			for ( int lauf=0; lauf<paramClassNames.length; lauf++) 
				paramClassNames[lauf] = paramTypes[lauf].getSimpleName();
			
			multiple.add(paramClassNames);
		}
		
		
		String[] methodNames = htMethodnamesToParameters.keySet().toArray(new String[0]);
		java.util.Arrays.sort ( methodNames );
		
		for ( String method: methodNames ) {
			String[][] params = htMethodnamesToParameters.get(method).toArray ( new String[0][]);
			
			java.util.Arrays.sort(params, new Comparator<String[]>(){
				@Override
				public int compare(String[] o1, String[] o2) {
					if ( o1.length != o2.length)
						return o1.length - o2.length;
					else {
						for ( int i=0; i<o1.length; i++ )
							if ( ! o1[i].equals( o2[i]))
								return o1[i].compareTo(o2[i]);
						
						return 0;
					}
				}
			});
			
			boolean namePrinted = false;
			for ( String[] paramlist : params) {
				if ( namePrinted ) {
					System.out.print ( SimpleTools.repeat ( " ", method.length() + 4 ) +  "(");
				} else {
					System.out.print ( " * " + method + " (");
					namePrinted = true;
				}
				
				System.out.println ( SimpleTools.join(paramlist, ", ") + ")");
			}
		}*/
	}

	/**
	 * print a help message
	 */
	public void printHelp() {
		System.out.println("Simple Java Command Line\n" + "------------------------\n" + "available commands:\n" + "\n"

				+ "\thelp / ?\tprint this message\n\n"

				+ "\tp(rint) [var1] [var2] ...\n" + "\t\t\tprint the contents of the given local variables\n\n"

				+ "\tpackage\t[some.package.name]\n\n"
				+ "\t\t\tuse the given package name as prefix for all Class relevant operations\n\n"

				+ "\tbind [varname]\n" + "\t\t\tuse the given local variable as object for method calls\n\n"

				+ "\tl(ist)\n"
				+ "\t\tprint all accessible methods of the bound object including their parameter list\n\n"

				+ "\tprintStackTrace / pst\n" + "\t\tprint the stacktrace of the last caught exception\n\n"

				+ "\texit/quit/q\texit the console\n\n" + "\tnew [some.Class] ([param1], [param2], ...)\n"
				+ "\t\t\tcreate a new instance of [some.Class] using the given parameters for the constructor\n"
				+ "\t\t\tthe parameters may be strings enclosed in ' or \" or local variables\n\n"
				+ "\t[some.Static.method] ([param1], [param2], ...)\n"
				+ "\t\t\tExecute a static method. If a package has been defined before, this package will be\n"
				+ "\t\t\tused as prefix for the package of the class containing the static method\n"
				+ "\t\t\tparameters mey be strings or local variables\n\n" + "\t[localVar].[method] ([param1], ...)\n"
				+ "\t\t\texecute a method of a local varialble\n\n" + "\t[method] ([param1], ...)\n"
				+ "\t\t\texecute a method on the bound object\n\n" + "\t[localVar] = ...\n"
				+ "\t\t\tAssign a value to a local variable. Any command may like creating a new instance\n"
				+ "\t\t\tor calling a static method may be used\n\n"
				+ "Lines statring with // or # will be ignored\n\n");
	}

	/**
	 * print a simple prompt and wait for input
	 * 
	 * @return return status code
	 */
	public ReturnStatus prompt() {
		try {
			System.out.print("" + lineNumber);
			if (packagePrefix != null && !packagePrefix.isEmpty())
				System.out.print(" : " + packagePrefix);
			if (boundTo != null)
				System.out.print(" : " + boundTo);
			System.out.print(" > ");
			String read = input.readLine().trim();

			if (!read.trim().isEmpty())
				return handleLine(read);
			else
				return ReturnStatus.OK_PROCEED;
		} catch (IOException e) {
			System.err.println("Error reading input command: " + e);
			return ReturnStatus.ERROR_EXIT;
		}
	}

	/**
	 * loop through input and execution
	 */
	public void startPrompt() {
		ReturnStatus status = ReturnStatus.OK_PROCEED;
		do {
			status = prompt();

			System.out.println("status: " + status);
		} while (status.proceed());
	}

	/**
	 * 
	 * start a command line
	 * 
	 * @param argv
	 * 
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] argv) throws ClassNotFoundException {
		CommandPrompt prompt = new CommandPrompt();

		prompt.printHelp();

		prompt.startPrompt();
	}

}

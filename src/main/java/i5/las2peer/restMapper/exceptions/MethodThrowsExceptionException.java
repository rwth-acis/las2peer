package i5.las2peer.restMapper.exceptions;

/**
 * @author Alexander
 */

public class MethodThrowsExceptionException extends Exception {


    private static final long serialVersionUID = 8705410691079747674L;


    String method;
    public MethodThrowsExceptionException(String method)
    {
        super();
        this.method=method;
    }

    public String getMethod()
    {
        return method;
    }

    public String getMessage()
    {
        return "Method: "+method+" throws unhandled exception(s). Please handle exceptions inside the method!";
    }
}
package i5.las2peer.restMapper.exceptions;

/**
 * @author Alexander
 */

public class ConflictingMethodPathException extends Exception {


    private static final long serialVersionUID = -3980276539276763845L;
    String methodName;
    String oldMethodName;
    public ConflictingMethodPathException(String methodName, String oldMethodName)
    {
        super();
        this.methodName=methodName;
        this.oldMethodName=oldMethodName;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public String getOldMethodName()
    {
        return oldMethodName;
    }
    public String getMessage()
    {
        return "Conflict: "+methodName+" can not be added, because there is already a method from another class: "+oldMethodName+" at this path.";
    }
}
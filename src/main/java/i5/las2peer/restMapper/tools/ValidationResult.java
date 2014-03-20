package i5.las2peer.restMapper.tools;

/**
 * @author Alexander
 */
public class ValidationResult
{
    private boolean isValid=false;
    private String message="";

    public void setValid(boolean isValid)
    {
        this.isValid = isValid;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public boolean isValid()
    {
        return isValid;
    }

    public String getMessage()
    {
        return message;
    }

    public ValidationResult()
    {

    }

    public ValidationResult(boolean isValid, String message)
    {
        this.isValid = isValid;
        this.message = message;
    }

    public void addMessage(String s)
    {
        this.message+="\n"+s;
    }
}

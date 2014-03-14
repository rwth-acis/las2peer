package i5.las2peer.restMapper.data;

import java.util.Comparator;

/**
 * @author Alexander
 */
public class AcceptHeaderTypeComperator implements Comparator<AcceptHeaderType>
{
    @Override
    public int compare(AcceptHeaderType a, AcceptHeaderType b) //see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html  14.1 Accept
    {

        if(a.getQvalue()==1&&b.getQvalue()==1)
        {
            if(a.getSpecificLevel() < b.getSpecificLevel())
                return 1;
            else if (a.getSpecificLevel() == b.getSpecificLevel())
            {
                return 0;
            }
            else
                return -1;

        }
        else if(a.getQvalue()==1&&b.getQvalue()!=1)
        {
            return -1;
        }
        else if(a.getQvalue()!=1&&b.getQvalue()==1)
        {
            return 1;
        }
        else if(a.getQvalue()<b.getQvalue())
        {
            return 1;
        }
        else if(a.getQvalue()==b.getQvalue())
        {
            return 0;
        }
        else
            return -1;



    }
}

package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.annotations.*;

@Version("0.2")
public class PseudoLatin extends Service{

	@POST
	@Path("latin")
	public String latin(@ContentParam String text)
	{
		StringBuilder result= new StringBuilder();
		String [] parts=text.split(" ");
		
		for (int i = 0; i < parts.length; i++) {
			int rnd=0 + (int)(Math.random() * (2 + 1));
			
			switch (rnd) {
			case 0:		
				result.append(parts[i]+"a ");
				break;
			case 1:		
				result.append(parts[i]+"us ");
				break;
			case 2:			
				result.append(parts[i]+"um ");
				break;			
			default:
				break;
			}
			
		}
		
		
		return result.toString();
	}
}

package ctSim.Model;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;

public class CtBotRealTcp extends CtBotReal {

	private TcpConnection tcpCon;
	
	public CtBotRealTcp(Point3f pos, Vector3f head, TcpConnection tc) {
		super(pos,head);
		tcpCon = tc; 
		// TODO Auto-generated constructor stub
	}

	public void work(){
		// Still missing
		ErrorHandler.error("BotRealTcp.run is missing");
	}

	protected void init() {
		// TODO Auto-generated method stub
		
	}

	protected void cleanup() {
		try {
			if (tcpCon != null)
				tcpCon.disconnect();
//			super.cleanup();
		} catch (Exception ex){			
		}		
	}
}

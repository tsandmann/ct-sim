package ctSim.view.contestConductor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;

import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.controller.Main;
import ctSim.view.View;

public class ContestConductorRecoveryTest extends ConductorTestUtil {
    @Override
    protected DatabaseAdapter getDbFromChildClass() {
        // TODO Auto-generated method stub
        return null;
    }

    public static class Timebomb implements InvocationHandler {
    	private Object proxied;

		public Timebomb(Object proxied) {
			this.proxied = proxied;
        }

		public Object invoke(
			@SuppressWarnings("unused") Object proxy,
			Method method,
			Object[] args)
		throws Throwable {
			return method.invoke(proxied, args);
        }
    }

    public static class MockMain extends Main {
    	public MockMain(String[] args) throws Exception {
    		super(args);
    	}
        @Override
        protected Controller buildController() {
        	return buildBombProxy(Controller.class,
        		new DefaultController());
        }

        @Override
        protected View buildContestConductor(Controller c)
        throws SQLException, ClassNotFoundException {
            return new ContestConductor(c,
            	new ConductorToDatabaseAdapter(),
            	buildBombProxy(TournamentPlanner.class,
            		new TournamentPlanner()));
        }
	}



    @SuppressWarnings("unchecked")
    public static <T> T buildBombProxy(Class<T> ifc, final T proxied) {
    	return (T)Proxy.newProxyInstance(
            ifc.getClassLoader(),
            new Class[] { ifc },
            timebomb);
    }

    public static void main(String[] unused) throws Exception {
    	String[] plannerMines = {};
    	String[] controllerMines = {};

    	ArrayList<Timebomb> bombs = new ArrayList<Timebomb>();
    	for (String s : plannerMines)
    		bombs.add(new Timebomb(controller));
    	for (int i = 0; i < 42; i++)
    		new MockMain("-conf", "config/ct-sim-contest-conductor.xml");
    }
}

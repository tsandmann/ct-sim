package ctSim.view;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

//$$ doc
public class ViewYAdapter  {
	public static View createInstance(final View... views) {
	    return (View)Proxy.newProxyInstance(
	    		View.class.getClassLoader(), 
	    		new Class[] { View.class }, 
	    		new InvocationHandler() {
					public Object invoke(
							@SuppressWarnings("unused") Object proxy, 
							Method method, Object[] args) 
					throws Throwable {
						for (View v : views)
							method.invoke(v, args);
	                    return null;
                    }});
    }
}

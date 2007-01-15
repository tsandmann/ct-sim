package ctSim.view;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * <p>
 * Fabrik-Klasse, die es erlaubt, beliebige Mengen Views in der Applikation
 * einzusetzen, wobei der Controller davon abstrahiert ist: F&uuml;r ihn sieht
 * sein View weiterhin wie nur ein einzelner View aus, egal wieviele Views
 * tats&auml;chlich dahinterstehen. Die Klasse realisiert mit anderen Worten
 * Point-to-Multipoint-Methodenaufrufe.
 * </p>
 * <p>
 * <strong>Verwendungsbeispiel:</strong>
 *
 * <pre>
 * interface View {
 *     void setWurst(...);
 * }
 *
 * View ziel1 = ...;
 * View ziel2 = ...;
 * View yAdapter = ViewYAdapter.newInstance(ziel1, ziel2);
 * yAdapter.setWurst(...); // so gut wie: ziel1.setWurst(...); ziel2.setWurst(...);
 * </pre>
 *
 * <strong>Vorteil</strong>: Der Code der letzten Zeile braucht nicht zu
 * wissen, mit wievielen Views er zu tun hat, er sieht nur eine Instanz. Bei
 * konventioneller Herangehensweise mit einem Array von Views w&auml;re an der
 * Stelle eine <code>for</code>-Schleife erforderlich gewesen. Je mehr solche
 * Methodenaufrufe, desto mehr an Schleifenb&uuml;rokratie wird durch diese
 * Klasse gespart.
 * </p>
 * <p>
 * Eine <strong>Metapher</strong> f&uuml;r diese Klasse w&auml;re ein Y-Adapter
 * mit einem 3,5-mm-Klinkenstecker auf der einen Seite und zwei 3,5mm-Buchsen
 * auf der anderen: Eine solche Weiche ist in der Lage, das Audiosignal an zwei
 * Empf&auml;nger (z.B. zwei Kopfh&ouml;rer) auszugeben. Dabei braucht das
 * Ger&auml;t, von dem das Tonsignal kommt, nur einen Ausgang zu haben und muss
 * nicht ausgelegt sein f&uuml;r mehrere Empf&auml;nger. (Diese Klasse
 * unterst&uuml;tzt abweichend von der Metapher nicht nur zwei Empf&auml;nger,
 * sondern jede Menge.)
 * </p>
 * <p>
 * Diese Klasse passt sich &Auml;nderungen im Interface View automatisch an,
 * d.h. von dieser Klasse generierte Objekte implementieren immer das
 * View-Interface in der aktuellen Form.
 * </p>
 *
 * @see View
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ViewYAdapter {
	/**
	 * <p>
	 * Erzeugt ein Objekt, das das Interface View implementiert. Es ist
	 * angeschlossen an die View-Objekte, die als Parameter &uuml;bergeben
	 * werden. Der Witz des zur&uuml;ckgegebenen Objekts ist: Wenn auf diesem
	 * Objekt zu irgendeinem Zeitpunkt eine beliebige Methode <em>M</em> des
	 * View-Interfaces aufgerufen wird, wird der Aufruf an jedes der
	 * angeschlossenen View-Objekte weitergeleitet &ndash; das hei&szlig;t,
	 * dass die Methode <em>M</em> nacheinander aufgerufen wird auf jedem der
	 * Views, wobei etwaige Argumente mitgegeben werden.
	 * </p>
	 * <p>
	 * Verwendungsbeispiel siehe {@link ViewYAdapter}.
	 * </p>
	 */
	public static View newInstance(final Iterable<View> views) {
		return (View)Proxy.newProxyInstance(View.class.getClassLoader(),
		    new Class[] { View.class }, new InvocationHandler() {
			    public Object invoke(
			    	@SuppressWarnings("unused") Object proxy,
			    	Method method, Object[] args) throws Throwable {
				    for (View v : views)
					    method.invoke(v, args);
				    return null;
			    }
		    });
	}

	/** Wie {@link #newInstance(Iterable)}, aber kann Varargs. */
	public static View newInstance(final View... views) {
		return newInstance(views);
	}
}

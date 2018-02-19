/*
 * c't-Sim - Robotersimulator für den c't-Bot
 * 
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version. 
 * This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 * 
 */

package ctSim.view;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * <p>
 * Fabrik-Klasse, die es erlaubt, beliebige Mengen Views in der Applikation
 * einzusetzen, wobei der Controller davon abstrahiert ist: Für ihn sieht
 * sein View weiterhin wie nur ein einzelner View aus, egal wieviele Views
 * tatsächlich dahinterstehen. Die Klasse realisiert mit anderen Worten
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
 * konventioneller Herangehensweise mit einem Array von Views wäre an der
 * Stelle eine <code>for</code>-Schleife erforderlich gewesen. Je mehr solche
 * Methodenaufrufe, desto mehr an Schleifenbürokratie wird durch diese
 * Klasse gespart.
 * </p>
 * <p>
 * Eine <strong>Metapher</strong> für diese Klasse wäre ein Y-Adapter
 * mit einem 3,5-mm-Klinkenstecker auf der einen Seite und zwei 3,5mm-Buchsen
 * auf der anderen: Eine solche Weiche ist in der Lage, das Audiosignal an zwei
 * Empfänger (z.B. zwei Kopfhörer) auszugeben. Dabei braucht das
 * Gerät, von dem das Tonsignal kommt, nur einen Ausgang zu haben und muss
 * nicht ausgelegt sein für mehrere Empfänger. (Diese Klasse
 * unterstützt abweichend von der Metapher nicht nur zwei Empfänger,
 * sondern jede Menge.)
 * </p>
 * <p>
 * Diese Klasse passt sich Änderungen im Interface View automatisch an,
 * d.h. von dieser Klasse generierte Objekte implementieren immer das
 * View-Interface in der aktuellen Form.
 * </p>
 *
 * @see View
 * @author Hendrik Krauß &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ViewYAdapter {
	/**
	 * <p>
	 * Erzeugt ein Objekt, das das Interface View implementiert. Es ist
	 * angeschlossen an die View-Objekte, die als Parameter übergeben
	 * werden. Der Witz des zurückgegebenen Objekts ist: Wenn auf diesem
	 * Objekt zu irgendeinem Zeitpunkt eine beliebige Methode <em>M</em> des
	 * View-Interfaces aufgerufen wird, wird der Aufruf an jedes der
	 * angeschlossenen View-Objekte weitergeleitet – das heißt,
	 * dass die Methode <em>M</em> nacheinander aufgerufen wird auf jedem der
	 * Views, wobei etwaige Argumente mitgegeben werden.
	 * </p>
	 * <p>
	 * Verwendungsbeispiel siehe {@link ViewYAdapter}.
	 * </p>
	 * @param views angeschlossene Views
	 * @return Das neue View
	 */
	public static View newInstance(final Iterable<View> views) {
		return (View) Proxy.newProxyInstance(View.class.getClassLoader(),
				new Class[] { View.class }, new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						for (View v : views)
							method.invoke(v, args);
						return null;
					}
				});
	}

	/** 
	 * Wie {@link #newInstance(Iterable)}, aber kann Varargs. 
	 * @param views angeschlossene Views
	 * @return Das neue View
	 */
	public static View newInstance(final View... views) {
		return newInstance(views);
	}
}

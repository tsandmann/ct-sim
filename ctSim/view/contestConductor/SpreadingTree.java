package ctSim.view.contestConductor;

import java.util.ArrayList;
import java.util.List;

//$$ doc SpreadingTree
/**
 * <p>
 * Der Turnierbaum ist folgendermaﬂen zu verstehen:
 * <ul>
 * <li>Ein Knoten des Baums repr&auml;sentiert einen Spieler</li>
 * <li>Die beiden Kinder eines Knotens <em>U</em> repr&auml;sentieren die
 * beiden Spiele, deren Gewinner im Spiel <em>U</em> aufeinandertreffen
 * werden. Beispiel: Die beiden Halbfinalspiele ergeben je einen Gewinner; diese
 * treffen im Finale aufeinander.</li>
 * <li>Die Nutzlast eines Knotens </li>
 * </ul>
 * </p>
 * <p>
 * B&auml;ume dieser Klasse sind <a
 * href="http://de.wikipedia.org/wiki/Bin%C3%A4rbaum">bin&auml;r</a>, <a
 * href="http://de.wikipedia.org/wiki/Bin%C3%A4rbaum#Weitere_Begriffe">strikt</a>
 * und <a href="http://de.wikipedia.org/wiki/Balancierter_Baum">balanciert</a>
 * (Invariante: H&ouml;he &plusmn;0).
 * </p>
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class SpreadingTree<T> {
	protected T payload = null;
	protected SpreadingTree<T> left = null;
	protected SpreadingTree<T> right = null;
	protected boolean lastAdditionLeft = false;

	protected SpreadingTree(T payload) {
		this.payload = payload;
	}

	protected SpreadingTree(SpreadingTree<T> left, SpreadingTree<T> right) {
		this.left = left;
		this.right = right;
    }

	// null undef
	protected SpreadingTree<T> add(SpreadingTree<T> node) {
		if (payload == null) {
			if (lastAdditionLeft) {
				right = right.add(node);
				lastAdditionLeft = false;
			} else {
				left = left.add(node);
				lastAdditionLeft = true;
			}
			return this;
		} else
			return new SpreadingTree<T>(this, node);
	}

	public static <T> SpreadingTree<T> buildTree(List<T> c) {
		if (c.size() % 2 != 0) //$$$ quatsch
			throw new IllegalArgumentException();
		SpreadingTree<T> rv = new SpreadingTree<T>(c.get(0));
		for (int i = 1; i < c.size(); i++)
			rv = rv.add(new SpreadingTree<T>(c.get(i)));
		return rv;
	}

	// von aussen aufzurufen
	public ArrayList<T> getTournamentPlan(int desiredLevelId) {
		if (payload != null) {
			// Wurzel ist kein innerer Knoten => Baum hat nur einen Knoten
			throw new IllegalStateException("Es muss mindestens zwei " +
					"Spieler geben, wenn ein Turnierplan erstellt werden " +
					"soll");
		}
		ArrayList<T> payloads = new ArrayList<T>();
		levelOrder(desiredLevelId * 2, 1, payloads);
		return payloads;
	}

	protected void levelOrder(int desiredLevelId, int currentLevel,
			List<T> payloads) {
		if (currentLevel == desiredLevelId)
			payloads.add(payload);
		if (payload == null) { // wir sind ein innerer Knoten
			left. levelOrder(desiredLevelId, currentLevel * 2, payloads);
			right.levelOrder(desiredLevelId, currentLevel * 2, payloads);
		}
	}

	@Override
    public String toString() { //$$ toString ist nur fuer demo
	    return payload == null ? "?" : ""+payload;
    }
}
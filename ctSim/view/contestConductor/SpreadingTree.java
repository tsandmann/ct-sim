package ctSim.view.contestConductor;

import java.util.ArrayList;
import java.util.List;

import ctSim.util.Misc;

/**
 * <p>
 * Der Turnierbaum ist folgendermaßen zu verstehen:
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
 * @param <T> Typ
 */
public class SpreadingTree<T> {
	/** Payload */
	protected T payload = null;
	/** links */
	protected SpreadingTree<T> left = null;
	/** rechts */
	protected SpreadingTree<T> right = null;
	/** zuletzt links? */
	protected boolean lastAdditionLeft = false;

	/** 
	 * neuer Baum 
	 * @param payload Payload
	 */
	protected SpreadingTree(T payload) {
		this.payload = payload;
	}

	/**
	 * Neuer Baum
	 * @param left links
	 * @param right rechts
	 */
	protected SpreadingTree(SpreadingTree<T> left, SpreadingTree<T> right) {
		this.left = left;
		this.right = right;
    }

	/**
	 * Node hinzufuegen
	 * null undef.
	 * @param node neuer Node
	 * @return Baum
	 */
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

	/**
	 * @param <T> Typ
	 * @param c Liste
	 * @return Tree
	 */
	public static <T> SpreadingTree<T> buildTree(List<T> c) {
    	// Muss Zweierpotenz sein
		if (Misc.log2(c.size()) != Math.round(Misc.log2(c.size())))
			throw new IllegalArgumentException();
		SpreadingTree<T> rv = new SpreadingTree<T>(c.get(0));
		for (int i = 1; i < c.size(); i++)
			rv = rv.add(new SpreadingTree<T>(c.get(i)));
		return rv;
	}

	/**
	 * von aussen aufzurufen
	 * @param desiredLevelId
	 * @return ArrayList
	 */
	public ArrayList<T> getTournamentPlan(int desiredLevelId) {
		if (payload != null) {
			// Wurzel ist kein innerer Knoten => Baum hat nur einen Knoten
			throw new IllegalStateException("Es muss mindestens zwei " +
					"Spieler geben, wenn ein Turnierplan erstellt werden " +
					"soll");
		}
		ArrayList<T> payloads = Misc.newList();
		levelOrder(desiredLevelId * 2, 1, payloads);
		return payloads;
	}

	/**
	 * @param desiredLevelId gewuenschtes Level
	 * @param currentLevel aktuelles Level
	 * @param payloads Payloads
	 */
	protected void levelOrder(int desiredLevelId, int currentLevel,
			List<T> payloads) {
		if (currentLevel == desiredLevelId)
			payloads.add(payload);
		if (payload == null) { // wir sind ein innerer Knoten
			left. levelOrder(desiredLevelId, currentLevel * 2, payloads);
			right.levelOrder(desiredLevelId, currentLevel * 2, payloads);
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString() {
	    return payload == null ? "?" : ""+payload;
    }
}
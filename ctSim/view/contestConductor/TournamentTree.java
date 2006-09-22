package ctSim.view.contestConductor;

import java.util.ArrayList;
import java.util.List;
//$$$ doc klasse + methoden
/**
 * <p>Dieses Ding baut einen Turnierbaum für die Hauptrunde auf. Mit anderen
 * Worten, es plant nach Abschluss der Vorrunde, welcher Bot gegen welchen
 * spielen soll. Die Planung erfolgt soweit m&ouml;glich ($$...).</p>
 *
 * <p>Die grunds&auml;tzliche Verwendung ist:
 * <ol><li><code>TournamentTree<...> tree = new
 * TournamentTree(</code><em>Spieler 1</em><code>);</code></li>
 * <li><code>tree = tree.add(</code><em>Spieler 2</em><code>);</code></li>
 * <li><code>tree = tree.add(</code><em>Spieler 3</em><code>);</code></li>
 * <li>...</li>
 * <li><code>tree.getTournamentPlan(1);</li></ol>
 *
 * </p>
 *
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
class TournamentTree<T> {
	protected T payload = null;
	protected TournamentTree<T> left = null;
	protected TournamentTree<T> right = null;
	protected boolean lastAdditionLeft = false;

	public TournamentTree(T payload) {
		this.payload = payload;
	}

	protected TournamentTree(TournamentTree<T> left, TournamentTree<T> right) {
		this.left = left;
		this.right = right;
    }

	public TournamentTree<T> add(TournamentTree<T> node) {
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
			return new TournamentTree<T>(this, node);
	}

	public TournamentTree<T> add(T p) {
		return add(new TournamentTree<T>(p));
	}

	// Umrechnung in die LevelId-Skalierung: Baum mit Hoehe 2 (d.h. 3 Knoten)
	// hat LowestLevel = 1 (Finale) usw.
	public int getGreencardLevelId() { return getHeight() / 4; }
	public int getLowestLevelId()    { return getHeight() / 2; }

	protected int getHeight() {
		return Math.max(
			(left  == null) ? 1 : left.getHeight()  * 2,
			(right == null) ? 1 : right.getHeight() * 2);
	}

	// von aussen aufzurufen
	public ArrayList<T> getTournamentPlan(int levelWanted) {
		if (payload != null) {
			// Wurzel ist kein innerer Knoten => Baum hat nur einen Knoten
			throw new IllegalStateException("Es muss mindestens zwei " +
					"Spieler geben, wenn ein Turnierplan erstellt werden " +
					"soll");
		}
		ArrayList<T> payloads = new ArrayList<T>();
		levelOrder(levelWanted == 0 ? 1 : levelWanted * 2, 1, payloads);
		return payloads;
	}

	protected void levelOrder(int levelWanted, int currentLevel,
			List<T> payloads) {
		if (currentLevel == levelWanted)
			payloads.add(payload);
		if (payload == null) { // wir sind ein innerer Knoten
			left.levelOrder( levelWanted, currentLevel * 2, payloads);
			right.levelOrder(levelWanted, currentLevel * 2, payloads);
		}
	}
}

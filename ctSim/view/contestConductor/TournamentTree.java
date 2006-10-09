package ctSim.view.contestConductor;

import java.util.ArrayList;
import java.util.List;

import ctSim.util.Misc;

//$$ doc TournamentTree
/**
 * <p>
 * Baut den grundlegenden Turnierbaum der Hauptrunde. Mit anderen Worten, plant
 * nach Abschluss der Vorrunde, welcher Bot gegen welchen spielen soll.
 * </p>
 * <p>
 * N&auml;heres siehe <a
 * href="TournamentPlanner.html#turnier-zwei-phasen">Beschreibung von Vor- und
 * Hauptrunde</a>.
 * </p>
 * <p>
 * <strong>Verwendungsbeispiel:</strong>
 * <ol>
 * <li><code>TournamentTree tree = new TournamentTree();</code></li>
 * <li><code>tree.add(<em>Bester der Vorrunde</em>);<br />
 * tree.add(<em>Zweitbester der Vorrunde</em>);<br />
 * tree.add(<em>Drittbester der Vorrunde</em>);<br />
 * ...</code> </li>
 * <li>
 * <pre>
 * for (int i = 0; i &lt; tree.getLowestLevelId(); i *= 2) {
 *     for (Player p : tree.getTournamentPlan(i)) {
 *         if (p == null)
 *             // F&uuml;r diesen Platz steht noch kein Kontrahent fest
 *         else
 *             // F&uuml;r diesen Platz ist p der Kontrahent
 *     }
 * }</code></li>
 * </ol>
 * </p>
 */
// level: 2er-Potenzen
public class TournamentTree extends ArrayList<Integer> {
	public static class Player {
		public final int rankNo;
		public final int botId;

		protected Player(int rankNo, int botId) {
	        this.rankNo = rankNo;
	        this.botId = botId;
        }
	}

    private static final long serialVersionUID = - 6908062086416166612L;

	/**
	 * Level, auf das die Spieler mit Freilos kommen. (Sprachlicher Hinweis:
	 * "bye" = "Freilos", siehe
	 * http://en.wikipedia.org/wiki/Single-elimination_tournament )
	 */
    public int getByeLevelId() {
    	return getLowestLevelId() / 2;
    }

    public int getLowestLevelId() {
    	assert size() >= 2;
    	int numLevels = (int)Math.ceil(Misc.log2(size())) - 1;
    	return (int)Math.pow(2, numLevels);
    }

    /**
	 * <p>
	 * <strong>Wieviele Spiele?</strong> Gegeben ist die Zahl der Spieler. Bei
	 * z. B. 42 Spielern kriegen wir die mit einem Sechzehntelfinale nicht mehr
	 * unter &ndash; Sechzehntelfinale bedeutet 16 Spiele, und das bedeutet 32
	 * Spieler. Also muss ein Zweiunddrei&szlig;igstelfinale her, auch wenn das
	 * nur teilweise besetzt sein wird (Sechzehntelfinale und alle folgenden
	 * werden dann voll besetzt sein). &ndash; Allgemein: Zahl der Spiele =
	 * 2^(aufgerundeter lb(Zahl der Spieler))
	 * </p>
	 * <p>
	 * <strong>Wieviele Freilose?</strong> Im Zweiunddrei&szlig;igstelfinale
	 * m&uuml;ssen genau soviele Spieler rausfliegen, dass (im Beispiel) das
	 * Sechzehntelfinale seine 32 Spieler hat &rarr; Da pro Spiel ein Spieler
	 * rausfliegt, m&uuml;ssen 42 &minus; 32 = 10 Spiele im
	 * Zweiunddrei&szlig;igstelfinale gespielt werden, die anderen bleiben leer.
	 * 10 Spiele = 20 Spieler, d.h. die &uuml;brigen 22 Spieler kriegen ein
	 * Freilos.
	 * </p>
	 * Freiloslevel immer ein Level h&ouml;her als lowestLevelId. 0 = 0
	 *
	 * @param desiredLevelId
	 * @return
	 */
    public ArrayList<Integer> getTournamentPlan(int desiredLevelId) {
    	assert desiredLevelId > 0;
    	assert desiredLevelId == 1 || desiredLevelId % 2 == 0; //$$$ besser machen
    	assert desiredLevelId <= getLowestLevelId();
    	assert size() >= 2;

    	// x-tel-Finale -> x Spiele -> 2 * x Spieler
    	final int numPlayersByeLevel = 2 * getByeLevelId();

    	// Baum bauen fuer Freilos- und hoehere Level, noch nicht niedrigstes
    	ArrayList<Player> playersByeLevel = new ArrayList<Player>();
    	for (int i = 0; i < numPlayersByeLevel; i++)
    		playersByeLevel.add(new Player(i, get(i)));
    	SpreadingTree<Player> tree = SpreadingTree.buildTree(playersByeLevel);

    	if (desiredLevelId < getByeLevelId())
	    	return botIdsFromPlayers(tree.getTournamentPlan(desiredLevelId));
    	else {
    		// Wir sollen lowestLevel oder byeLevel liefern
    		final int numGamesLowestLevel = size() - numPlayersByeLevel;
    		final int lastRankWithBye = size() - 2 * numGamesLowestLevel;
    		if (desiredLevelId == getByeLevelId()) {
    			ArrayList<Player> playersRaw = tree.getTournamentPlan(
    				desiredLevelId);
	    		for (int i = 0; i < playersRaw.size(); i++) {
    				if (playersRaw.get(i).rankNo >= lastRankWithBye)
    					playersRaw.set(i, null);
	    		}
    			return botIdsFromPlayers(playersRaw);
    		} else {
    			// Wir sollen lowestLevel zurueckliefern
    			ArrayList<Player> playersRaw = tree.getTournamentPlan(
    				getByeLevelId());
				ArrayList<Integer> rv = new ArrayList<Integer>();
				for (int i = 0; i < playersRaw.size(); i++) {
    				if (playersRaw.get(i).rankNo >= lastRankWithBye) {
    					rv.add(playersRaw.get(i).botId);
    					rv.add(get(playersRaw.get(i).rankNo +
    						numGamesLowestLevel));
    				} else {
    					rv.add(null);
    					rv.add(null);
    				}
	    		}
				return rv;
			}
    	}
    }

    private ArrayList<Integer> botIdsFromPlayers(List<Player> li) {
    	ArrayList<Integer> rv = new ArrayList<Integer>();
    	for (Player p : li) {
    		if (p == null)
    			rv.add(null);
    		else
    			rv.add(p.botId);
    	}
    	return rv;
    }

    public static void main(String... args) {
    	TournamentTree r = new TournamentTree();
    	for (int i = 1; i <= 21; i++)
    		r.add(i);
    	System.out.println("Bye " + r.getByeLevelId() + " Lowest " +
    		r.getLowestLevelId());
    	for (int i = 1; i <= r.getLowestLevelId(); i *= 2) {
    		boolean u = false;
    		for (Integer itg : r.getTournamentPlan(i)) {
    			u = ! u;
    			if (u)
    				System.out.print("| ");
    			if (itg == null)
    				System.out.print("n  ");
    			else
    				System.out.print(itg + (itg < 10 ? "  " : " "));
    		}
    		System.out.print("|\n");
    	}
    }
}

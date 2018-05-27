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

package ctSim.view.contestConductor;

import java.util.ArrayList;
import java.util.List;

import ctSim.util.Misc;

/**
 * <p>
 * Baut den grundlegenden Turnierbaum der Hauptrunde. Mit anderen Worten, plant nach Abschluss der Vorrunde,
 * welcher Bot gegen welchen spielen soll.
 * </p>
 * <p>
 * Für näheres siehe <a href="TournamentPlanner.html#turnier-zwei-phasen">Beschreibung von Vor- und Hauptrunde</a>.
 * </p>
 * <p>
 * <strong>Verwendungsbeispiel:</strong>
 * <ol>
 * <li><code>TournamentTree tree = new TournamentTree();</code></li>
 * <li><code>tree.add(<em>Bester der Vorrunde</em>);<br>
 * tree.add(<em>Zweitbester der Vorrunde</em>);<br>
 * tree.add(<em>Drittbester der Vorrunde</em>);<br>
 * ...</code></li>
 * <li>
 * <pre>
 * for (int i = 0; i &lt; tree.getLowestLevelId(); i *= 2) {
 *     for (Player p : tree.getTournamentPlan(i)) {
 *         if (p == null)
 *             // für diesen Platz steht noch kein Kontrahent fest
 *         else
 *             // für diesen Platz ist p der Kontrahent
 *     }
 * }
 * </prei>
 * </ol>
 * </p>
 */

// Level: 2er-Potenzen
public class TournamentTree extends ArrayList<Integer> {
	/** Spieler */
	public static class Player {
		/** Rang */
		public final int rankNo;
		/** Bot */
		public final int botId;

		/**
		 * Player
		 *
		 * @param rankNo	Rank
		 * @param botId		Bot-ID
		 */
		protected Player(int rankNo, int botId) {
			this.rankNo = rankNo;
			this.botId = botId;
		}
	}

	/** UID */
	private static final long serialVersionUID = - 6908062086416166612L;

	/**
	 * Level, auf das die Spieler mit Freilos kommen. (Sprachlicher Hinweis: "bye" = "Freilos", siehe
	 * <a href="https://de.wikipedia.org/wiki/K.-o.-System">K.-o.-System (Single-elimination tournament)</a>)
	 *
	 * @return Level
	 */
	public int getByeLevelId() {
		return getLowestLevelId() / 2;
	}

    /**
     * @return kleinstes Level
     */
    public int getLowestLevelId() {
    	assert size() >= 2;
    	int numLevels = (int)Math.ceil(Misc.log2(size())) - 1;
    	return (int)Math.pow(2, numLevels);
    }

	/**
	 * <p>
	 * <strong>Wieviele Spiele?</strong>
	 * Gegeben ist die Zahl der Spieler. Bei z.B. 42 Spielern bekommt man diese mit einem Sechzehntelfinale
	 * nicht mehr unter -- Sechzehntelfinale bedeutet 16 Spiele, und das bedeutet 32 Spieler. Also muss ein
	 * Zweiunddreißigstelfinale her, auch wenn das nur teilweise besetzt sein wird (Sechzehntelfinale und
	 * alle folgenden werden dann voll besetzt sein).
	 * Allgemein: Anzahl Spiele = 2^(aufgerundeter lb(Anzahl Spieler))/2 (lb = Binärer Logarithmus, also log_2(x))
	 * </p>
	 * <p>
	 * <strong>Wie viele Freilose?</strong>
	 * Im Zweiunddreißigstelfinale müssen genau so viele Spieler rausfliegen, dass (im Beispiel) das
	 * Sechzehntelfinale seine 32 Spieler hat -&gt; Da pro Spiel ein Spieler rausfliegt, müssen 42 - 32 = 10
	 * Spiele im Zweiunddreißigstelfinale gespielt werden, die anderen bleiben leer. 10 Spiele = 20 Spieler,
	 * d.h. die übrigen 22 Spieler bekommen ein Freilos.
	 * </p>
	 * Freiloslevel immer ein Level höher als lowestLevelId. 0 = 0
	 *
	 * @param desiredLevelId
	 * @return der Plan
	 */
    public ArrayList<Integer> getTournamentPlan(int desiredLevelId) {
    	assert desiredLevelId > 0;
    	// muss eine Zweierpotenz sein
    	assert Misc.log2(desiredLevelId) == Math.round(Misc.log2(desiredLevelId));
    	assert desiredLevelId <= getLowestLevelId();
    	assert size() >= 2;

    	// x-tel-Finale -> x Spiele -> 2 * x Spieler
    	final int numPlayersByeLevel = 2 * getByeLevelId();

    	// Baum bauen für Freilos- und höhere Level, noch nicht für niedrigstes Level
    	ArrayList<Player> playersByeLevel = new ArrayList<Player>();
    	for (int i = 0; i < numPlayersByeLevel; i++)
    		playersByeLevel.add(new Player(i, get(i)));
    	SpreadingTree<Player> tree = SpreadingTree.buildTree(playersByeLevel);

    	if (desiredLevelId < getByeLevelId())
	    	return botIdsFromPlayers(tree.getTournamentPlan(desiredLevelId));
    	else {
    		// wir sollen lowestLevel oder byeLevel liefern
    		final int numGamesLowestLevel = size() - numPlayersByeLevel;
    		final int lastRankWithBye = size() - 2 * numGamesLowestLevel;
    		if (desiredLevelId == getByeLevelId()) {
    			ArrayList<Player> playersRaw = tree.getTournamentPlan(desiredLevelId);
	    		for (int i = 0; i < playersRaw.size(); i++) {
    				if (playersRaw.get(i).rankNo >= lastRankWithBye)
    					playersRaw.set(i, null);
	    		}
    			return botIdsFromPlayers(playersRaw);
    		} else {
    			// wir sollen lowestLevel zurückliefern
    			ArrayList<Player> playersRaw = tree.getTournamentPlan(getByeLevelId());
				ArrayList<Integer> rv = new ArrayList<Integer>();
				for (int i = 0; i < playersRaw.size(); i++) {
    				if (playersRaw.get(i).rankNo >= lastRankWithBye) {
    					rv.add(playersRaw.get(i).botId);
    					rv.add(get(playersRaw.get(i).rankNo + numGamesLowestLevel));
    				} else {
    					rv.add(null);
    					rv.add(null);
    				}
	    		}
				return rv;
			}
    	}
    }

    /**
     * Gibt die Bot-IDs zu den Playern zurück
     *
     * @param li	Player-Liste
     * @return Liste der Bot-IDs
     */
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

    /**
     * main
     *
     * @param args
     */
    public static void main(String... args) {
    	TournamentTree r = new TournamentTree();
    	for (int i = 1; i <= 21; i++)
    		r.add(i);
    	System.out.println("Bye " + r.getByeLevelId() + " Lowest " + r.getLowestLevelId());
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

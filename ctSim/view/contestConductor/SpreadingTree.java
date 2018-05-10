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
 * Der Turnierbaum ist folgendermaßen zu verstehen:
 * <ul>
 * <li>Ein Knoten des Baums repräsentiert einen Spieler</li>
 * <li>Die beiden Kinder eines Knotens <em>U</em> repräsentieren die beiden Spiele, deren Gewinner im
 * Spiel <em>U</em> aufeinandertreffen werden. Beispiel: Die beiden Halbfinalspiele ergeben je einen
 * Gewinner; diese treffen im Finale aufeinander.</li>
 * <li>Die Nutzlast eines Knotens</li>
 * </ul>
 * </p>
 * <p>
 * Bäume dieser Klasse sind <a href="http://de.wikipedia.org/wiki/Binärbaum">binär</a>,
 * <a href="http://de.wikipedia.org/wiki/Binärbaum#Weitere_Begriffe">strikt</a> und
 * <a href="http://de.wikipedia.org/wiki/Balancierter_Baum">balanciert</a> (Invariante: Höhe ± 0).
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 * 
 * @param <T>	Typ
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
	 * Neuer Baum
	 * 
	 * @param payload	Payload
	 */
	protected SpreadingTree(T payload) {
		this.payload = payload;
	}

	/**
	 * Neuer Baum
	 * 
	 * @param left	links
	 * @param right	rechts
	 */
	protected SpreadingTree(SpreadingTree<T> left, SpreadingTree<T> right) {
		this.left = left;
		this.right = right;
    }

	/**
	 * Node hinzufügen
	 * 
	 * null: undefined
	 * 
	 * @param node	neuer Node
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
	 * @param <T>	Typ
	 * @param c		Liste
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
	 * Von außen aufzurufen
	 * 
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
	 * @param desiredLevelId	gewünschtes Level
	 * @param currentLevel		aktuelles Level
	 * @param payloads			Payloads
	 */
	protected void levelOrder(int desiredLevelId, int currentLevel,
			List<T> payloads) {
		if (currentLevel == desiredLevelId)
			payloads.add(payload);
		if (payload == null) {	// wir sind ein innerer Knoten
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
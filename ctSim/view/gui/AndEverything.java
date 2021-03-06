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

package ctSim.view.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.MapComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.util.AuxFrameButton;
import ctSim.util.Misc;

/**
 * Zeigt Log-Knopf, LCD, Fernbedienungsknopf, Remote-Call-Knopf, ABL-Knopf. Gehört zu dem Bereich, wo
 * Informationen über einen Bot angezeigt werden.
 */
public class AndEverything extends GuiBotBuisitor {
	/** UID */
	private static final long serialVersionUID = - 8170321975584432026L;

	/** Anzeige für "Rest" */
	public AndEverything() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Display & Remote-Control"));
	}

	/**
	 * Erstellt die Text-Area, die das LCD des Bot zeigt
	 *
	 * @param d	Display
	 */
	public void buisitLcdViewer(Actuators.LcDisplay d) {
		JTextArea t = new JTextArea(d.getExternalModel(), null, d.getNumRows(), d.getNumCols());
		t.setEnabled(false);

		/**
		 * Fix für ct-Sim-GUI-Bug: "Kein Scrollen möglich wenn Sim aktiv"
		 * Details ließen sich zwar nicht ermitteln, aber wenn man das Caret (=Cursor) abschaltet funktioniert es.
		 * Siehe <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4201999">
		 * Issue JDK-4201999: JTextArea's don't automatically scroll when appending() to them.</a> und den
		 * zugehörigen <a href="https://docs.oracle.com/javase/1.5.0/docs/guide/swing/1.5/#swingText">Fix</a>.
		 */
		Misc.setCaretPolicy(t, DefaultCaret.NEVER_UPDATE);

		t.setFont(new Font("Monospaced", Font.PLAIN, 12));
		t.setDisabledTextColor(Color.BLACK);
		t.setBackground(new Color(170, 200, 90));
		t.setBorder(BorderFactory.createLoweredBevelBorder());
		t.setToolTipText(d.getName());
		t.setMaximumSize(t.getPreferredSize());
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(t);
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	/**
	 * Baut den Knopf, der zum Log-Fenster führt
	 *
	 * @param log	Log
	 * @param bot	Bot
	 */
	public void buisitLogViewer(Actuators.Log log, Bot bot) {
		add(new AuxFrameButton(
			log.getName(),
			log.getDescription() + " des " + bot,	// Fenster-Titel
			new LogViewer(log),
			bot.get_feature_log()));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	/**
	 * Baut den Knopf, der zum Fernbedienungs-Fenster führt
	 *
	 * @param s		RC5-Control
	 * @param bot	Bot
	 */
	public void buisitRemoteControl(Sensors.RemoteControl s, Bot bot) {
		add(new AuxFrameButton(
			s.getDescription() + " (" + s.getName() + ")",
			s.getDescription() + " für " + bot,
			new RemoteControlViewer(s),
			bot.get_feature_rc5()));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	/**
	 * Baut den Knopf, der zum Remote-Call-Fenster führt
	 *
	 * @param c		Remote-Call Komponente
	 * @param bot	Bot
	 */
	public void buisitRemoteCallViewer(RemoteCallCompnt c, Bot bot) {
		add(new AuxFrameButton(
			c.getName(),
			c.getName() + " an " + bot,
			new RemoteCallViewer(c),
			bot.get_feature_remotecall()));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	/**
	 * Baut den Knopf, der das Map-Fenster anzeigt
	 *
	 * @param map	Map-Komponente
	 * @param bot	Bot
	 */
	public void buisitMapViewer(MapComponent map, Bot bot) {
		add(new AuxFrameButton(
			map.getName(),
			map.getDescription() + " von " + bot,	// Fenster-Titel
			new MapViewer(map, bot),
			bot.get_feature_map()));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}

	/**
	 * Baut den Knopf, der zum Progamm-Fenster führt
	 *
	 * @param program	Programm-Komponente
	 * @param bot		Bot
	 */
	public void buisitProgramViewer(Actuators.Program program, Bot bot) {
		add(new AuxFrameButton(
			program.getName(),
			program.getDescription() + " von " + bot,	// Fenster-Titel
			new ProgramViewer(program, bot),
			bot.get_feature_abl_program() || bot.get_feature_basic_program()));
		add(Box.createRigidArea(new Dimension(0, 5)));
	}
}

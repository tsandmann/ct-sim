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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import ctSim.controller.Config;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.RemoteCallCompnt.Behavior;
import ctSim.model.bots.components.RemoteCallCompnt.BehaviorExitStatus;
import ctSim.model.bots.components.RemoteCallCompnt.Parameter;
import ctSim.util.ComponentTable;
import ctSim.util.FmtLogger;
import ctSim.util.GridBaggins;
import ctSim.util.Runnable1;

/**
 * Remote-Call-Fenster 
 */
public class RemoteCallViewer extends JPanel {
	/** UID */
	private static final long serialVersionUID = - 3844905681548769569L;

	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.view.gui.RemoteCallViewer");

	/**
	 * Behaviour-Viewer
	 */
	static class BehaviorViewer extends JPanel {
		/** UID */
		private static final long serialVersionUID = 4518196039674033397L;
		/** Verhalten */
		private final Behavior behavior;

		/**
		 * Behaviour-Anzeige
		 * @param b Behaviour
		 */
		public BehaviorViewer(Behavior b) {
			this.behavior = b;
			setLayout(new FlowLayout(FlowLayout.LEADING)); // linksbuendig
			for (Parameter p : b.getParameters()) {
				String tooltip = p.fullName;
				String[] s = p.name.split(" ");
				String label;
				if (s.length == 2)
					label = s[1];
				else
					label = p.name;

				// Nach links hin bisschen Platz, mit der Low-tech-Methode
				JLabel jl = new JLabel("   "+label+" =");
				jl.setToolTipText(tooltip);
				add(jl);
				JSpinner js = new JSpinner(p);
				js.setToolTipText(tooltip);
				int numCols = 3; // Default
				try {
					numCols = Math.max(
						// Wieviele Stellen haben Min und Max?
						p.getMaximum().toString().length(),
						p.getMinimum().toString().length());
				} catch (NullPointerException e) {
					// p.getMax() oder p.getMin() können null sein,
					// in dem Fall ignorieren und Default nehmen
				}
				((DefaultEditor)js.getEditor()).getTextField().setColumns(
					numCols);
				add(js);
			}

			// Mindestbreite ausrechnen, damit ScrollPane bescheidweiss
			// Zwischenräume
			int prefWidth = (getComponentCount() - 1) *
				((FlowLayout)getLayout()).getHgap();
			// Breiten der Komponenten
			for (int i = 0; i < getComponentCount(); i++)
				prefWidth += getComponent(i).getPreferredSize().width;

			setMinimumSize(new Dimension(prefWidth, getMinimumSize().height));
		}

		// An alle Kinder weitergeben
		/**
		 * @see javax.swing.JComponent#setForeground(java.awt.Color)
		 */
		
		@Override
		public void setForeground(Color fg) {
			for (int i = 0; i < getComponentCount(); i++)
				getComponent(i).setForeground(fg);
		}

		// An alle Kinder weitergeben
		/**
		 * @see javax.swing.JComponent#setEnabled(boolean)
		 */
		
		@Override
		public void setEnabled(boolean enabled) {
			for (int i = 0; i < getComponentCount(); i++)
				getComponent(i).setEnabled(enabled);
		}

		/**
		 * @return Das Verhalten
		 */
		Behavior getBehavior() {
			return behavior;
		}
	}

	/**
	 * Behaviour-Modell
	 */
	static class BehaviorModel extends DefaultTableModel {
		/** UID */
		private static final long serialVersionUID = - 3091736642693972956L;

		/**
		 * Behaviour-Model
		 * @param numCols Anzahl der Spalten
		 */
		public BehaviorModel(int numCols) {
			super(0, numCols);
		}

		/**
		 * Fügt ein Verhalten hinzu
		 * @param b Verhalten
		 */
		protected void addBehavior(Behavior b) {
			addRow(new Object[] { buildName(b), buildBhvViewer(b) });
		}

		/**
		 * Baut den Verhaltens-Viewer zum Verhalten
		 * @param b Verhalten
		 * @return Viewer
		 */
		protected JComponent buildBhvViewer(Behavior b) {
			return new BehaviorViewer(b);
		}

		/**
		 * Name zum Verhalten erzeugen
		 * @param b Verhalten
		 * @return Name
		 */
		protected JLabel buildName(Behavior b) {
    		JLabel rv = new JLabel(b.getName());
    		rv.setOpaque(true);
    		rv.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
    		return rv;
		}
	}

	/**
	 * Modell der geplanten Verhalten
	 */
	class PlannedBhvModel extends BehaviorModel {
		/**
		 * Statusanzeige
		 */
		abstract class StatusLabel extends JLabel {
			/**
			 * UID
			 */
			private static final long serialVersionUID = -4299460609018762995L;

			/**
			 * Statusanzeige
			 * @param label		Text
			 * @param tooltip	Tooltip
			 * @param c			Farbe
			 */
			public StatusLabel(String label, String tooltip, Color c) {
				super(label);
				setToolTipText(tooltip);
				setOpaque(true);
				super.setBackground(c.darker());
				super.setForeground(Color.WHITE);
				setFont(getFont().deriveFont(Font.BOLD));
				setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
			}

			/**
			 * @see javax.swing.JComponent#setForeground(java.awt.Color)
			 */
			
			@Override
			public void setForeground(Color fg) {
				// No-op: Wir setzen das im Konstruktor, die Tabelle soll's ab
				// dann nicht mehr ändern
			}

			/**
			 * @see javax.swing.JComponent#setBackground(java.awt.Color)
			 */
			
			@Override
			public void setBackground(Color bg) {
				super.setBackground(bg.darker());
			}

			/**
			 * @see javax.swing.JComponent#setEnabled(boolean)
			 */
			
			@Override
			public void setEnabled(boolean b) {
				// No-op: Wir sind auf enabled und das soll so bleiben, weil
				// sonst der Text grau wird
			}
		}

		/**
		 * Status wartend
		 */
		class Waiting extends StatusLabel {
			/** UID */
			private static final long serialVersionUID = - 1396203136406798134L;

			/**
			 * Wartestatus
			 */
			public Waiting() {
				super("Wartet", "Remote-Call wartet, bis alle Calls " +
						"über ihm abgeschlossen sind", Color.GRAY);
			}
		}

		/**
		 * Status laufend
		 */
		class Running extends StatusLabel {
			/** UID */
			private static final long serialVersionUID = - 7019340883477925888L;

			/**
			 * Aktivstatus
			 */
			public Running() {
				super("Läuft", "Remote-Call wird vom Bot " +
						"gegenwärtig ausgeführt", Color.BLUE);
			}
		}

		/**
		 * Status fertig
		 */
		class Done extends StatusLabel {
			/** UID */
			private static final long serialVersionUID = - 2082537920466563306L;

			/**
			 * Fertig
			 * @param label		Text
			 * @param tooltip	Tooltip
			 * @param c			Farbe
			 */
			public Done(String label, String tooltip, Color c) {
				super(label, tooltip, c);
			}
		}

		/**
		 * Zeigt auf 1. Zelle der Zeile des Behaviors, das gerade
		 * ausgeführt wird
		 */
		private StatusLabel nowRunning = null;

		/**
		 * Geplante Verhalten
		 */
		public PlannedBhvModel() {
			super(4);
		}
		
		/** UID */
		private static final long serialVersionUID = 2841437768966488801L;

		/**
		 * Loesch-Button
		 */
		class DeleteButton extends JButton implements ActionListener {
			/** UID */
			private static final long serialVersionUID = 1802551443502887184L;

			/** laueft grad? */
			private boolean isRunning;

			/**
			 * Loeschen-Button
			 */
			public DeleteButton() {
				setIcon(Config.getIcon("schließen-hover"));
				addActionListener(this);

				setMaximumSize(new Dimension(24, 24));
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			}

			/**
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent e) {
				if (isRunning) {
					try {
						rcCompnt.abortCurrentBehavior();
					} catch (IOException excp) {
						lg.warn(excp, "E/A-Problem beim Senden des " +
								"Abbruchkommandos");
					}
				}
				else
					removeThisRowFromTable();
			}

			/**
			 * Entfernt Objekt aus der Tabelle
			 */
			private void removeThisRowFromTable() {
				for (int row = 0; row < getRowCount(); row++) {
					if (getValueAt(row, 3) == this) {
						removeRow(row);
						return;
					}
				}
				throw new IllegalStateException("Interner Fehler " +
						"(Selbstfindungsproblem): Löschen-Knopf " +
						"funktioniert nicht");
			}

			/**
			 * Setzt den running-Status
			 * @param isRunning true oder false
			 */
			public void setRunning(boolean isRunning) {
				this.isRunning = isRunning;
				setToolTipText(isRunning
					? "Ausführung des Remote-Call abbrechen"
					: "Remote-Call löschen");
			}
		}

		/**
		 * @see ctSim.view.gui.RemoteCallViewer.BehaviorModel#addBehavior(ctSim.model.bots.components.RemoteCallCompnt.Behavior)
		 */
		
		@Override
		protected void addBehavior(Behavior b) {
			addRow(new Object[] {
				new Waiting(),
				buildName(b),
				buildBhvViewer(b),
				new DeleteButton() });
			if (nowRunning == null)
				callNextBehavior();
		}

		/**
		 * Ruft das nächste Verhalten in der Liste auf
		 */
		protected void callNextBehavior() {
			for (int row = 0; row < getRowCount(); row++) {
				if (getValueAt(row, 0) instanceof Done)
					continue;

				lg.fine("Starte nächsten Remote-Call");
				if (! (getValueAt(row, 0) instanceof Waiting))
					throw new IllegalStateException("Interner Fehler");

				setRunningLabel(row);
				disableRow(row);
				setDeleteButton(row, true);
				sendBehaviorRequest(row);

				return;
			}
			lg.fine("Keine Remote-Calls mehr abzuarbeiten");
		}

		/**
		 * Setzt ein Label auf laufend
		 * @param row Zeile
		 */
		private void setRunningLabel(int row) {
			nowRunning = new Running();
			setValueAt(nowRunning, row, 0);
		}

		/**
		 * Deaktiviert eine Zeile
		 * @param row Zeile
		 */
		private void disableRow(int row) {
			for (int col = 0; col < getColumnCount(); col++) {
				// Spalte 3 (Loeschen-Button) nicht anfassen
				if (col != 3)
					((JComponent)getValueAt(row, col)).setEnabled(false);
			}
		}

		/**
		 * Sendet ein Verhalten
		 * @param row Zeile mit dem Verhalten
		 */
		private void sendBehaviorRequest(int row) {
			try {
				((BehaviorViewer)getValueAt(row, 2)).getBehavior().call();
			} catch (IOException e) {
				lg.warn(e, "Schlimm: E/A-Problem beim Senden des " +
				"Remote-Call; ignoriere Remote-Call und fahre fort");
			}
		}

		/**
		 * Setzt das letzte Verhalten auf einen Exit-Status
		 * @param status gewünschter Status
		 */
		protected void setLastCallDone(BehaviorExitStatus status) {
			for (int row = 0; row < getRowCount(); row++) {
				if (getValueAt(row, 0) == nowRunning) {
					setDoneLabel(row, status);
					enableRow(row);
					setDeleteButton(row, false);
					return;
				}
			}
			//lg.warn("Ein Remote Call wurde beendet, der laut Liste gar nicht läuft");
			// unproblematisch
		}

		/**
		 * Setzt das Done-Label
		 * @param row Zeile
		 * @param status Exit-Status
		 */
		private void setDoneLabel(int row, BehaviorExitStatus status) {
			String tooltip;
			Color color;
			switch (status) {
				case SUCCESS:
					tooltip = "Remote-Call erfolgreich abgeschlossen";
					color = new Color(0, 160, 0);
					break;
				case FAILURE:
					tooltip = "Remote-Call ist in die Hose gegangen";
					color = Color.RED;
					break;
				case CANCELLED:
					tooltip = "Remote-Call abgebrochen";
					color = Color.ORANGE;
					break;
				case BACKGRND:
					tooltip = "Remote-Call läuft im Hintergrund";
					color = Color.GRAY;
					break;
				default:
					throw new IllegalStateException();
			}

			nowRunning = null;
			setValueAt(new Done(status.toString(), tooltip, color), row, 0);
		}

		/**
		 * Aktiviert eine Zeile
		 * @param row Zeile
		 */
		private void enableRow(int row) {
			for (int col = 0; col < getColumnCount(); col++) {
				// Spalte 3 (Loeschen-Button) nicht anfassen
				if (col != 3)
					((JComponent)getValueAt(row, col)).setEnabled(true);
			}
		}

		/**
		 * Setzt den Loeschen-Knopf
		 * @param row Zeile
		 * @param isRunning läuft grad?
		 */
		private void setDeleteButton(int row, boolean isRunning) {
			((DeleteButton)getValueAt(row, 3)).setRunning(isRunning);
		}
	}

	/** RemoteCall-Komponente */
	private final RemoteCallCompnt rcCompnt;

	/** Wurde schon eine Liste der RemoteCalls angefordert? */
	private boolean listReceived = false;
	
	/**
	 * @return True, falls schon eine RemoteCall-Liste angefordert wurde
	 */
	public boolean getListReceived() {
		return listReceived;
	}

	/**
	 * Baut die Tabelle
	 * @param m Modell
	 * @return Tabelle
	 */
	private ComponentTable buildCompntTable(TableModel m) {
		final ComponentTable rv = new ComponentTable(m);
		m.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				rv.accomodateContent();
			}
		});
		return rv;
	}

	/**
	 * Fordert beim Bot eine Liste aller RemoteCalls an
	 */
	public void requestRCallList() {
		try {
			rcCompnt.listRemoteCalls();
			this.listReceived = true;
		} catch (IOException e) {
			lg.warn(e, "E/A-Problem aufgetreten, als die Anforderung der " +
				"Remote-Call-Liste gesendet wurde; wer weiß, ob " +
				"das jetzt funktioniert mit den Remote-Calls");
		}
	}

	/**
	 * Remote-Call Anzeige
	 * @param rcCompnt RemoteCall-Komponente
	 */
	public RemoteCallViewer(final RemoteCallCompnt rcCompnt) {
		this.rcCompnt = rcCompnt;
		if (rcCompnt == null) return;

		final BehaviorModel availM = new BehaviorModel(2) {
			private static final long serialVersionUID = 3932551442111274878L;

			
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 1;
			}
		};
		final ComponentTable availBhvs = buildCompntTable(availM);

        rcCompnt.addBehaviorListener(new Runnable1<Behavior>() {
        	public void run(Behavior newBehavior) {
        		availM.addBehavior(newBehavior);
        	}
        });
        availBhvs.setTableHeader(null); // Header weg
        availBhvs.getColumnClass(getComponentCount());
        // Nur 1 Zeile markierbar
        availBhvs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availBhvs.setColumnSelectionAllowed(true);
        availBhvs.setRowSelectionAllowed(true);

		setLayout(new GridBagLayout());

		final PlannedBhvModel plannedM = new PlannedBhvModel();

		JPanel availHeading = new JPanel();
		availHeading.add(new JLabel("Verfügbare Remote-Calls"));

		JButton refresh = new JButton("Holen");
		refresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				while (availM.getRowCount() > 0)
					availM.removeRow(0);
				requestRCallList();
			}
		});
		availHeading.add(refresh);

		add(availHeading, new GridBaggins().row(0).col(0).epady(3));

		add(new JScrollPane(availBhvs),
			new GridBaggins().row(1).col(0).weightx(0.4).weighty(1).fillHV()
			.epadx(10));

		final ComponentTable plannedBhvs = buildCompntTable(plannedM);

		/*
		 * Workaround für Bug in JTable: Wenn das Model sich ändert (der
		 * JButton im Model löscht Zeilen aus dem Model), dann kriegt wir u.U.
		 * eine Exception im Zusammenhang mit editingStopped-Ereignissen, weil
		 * die Ereignisse Zellen aktualisieren, die schon nicht mehr da sind.
		 * Details:
		 * http://forum.java.sun.com/thread.jspa?threadID=5133941&tstart=75 und
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4777756 -- wir
		 * verwenden den Workaround von bugs.sun.com
		 */
		plannedM.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int row = plannedBhvs.getEditingRow();
				if (e.getFirstRow() <= row && row <= e.getLastRow())
					plannedBhvs.editingCanceled(new ChangeEvent(this));
			}
		});

		// Unicode 2192: Pfeil nach rechts
		JButton b = new JButton(new AbstractAction("\u2192") {
			private static final long serialVersionUID = - 3903803946116099232L;

			public void actionPerformed(ActionEvent e) {
				int selected = availBhvs.getSelectedRow();
				if (selected == -1)
					return;
				BehaviorViewer bv = (BehaviorViewer)availBhvs.getModel()
					.getValueAt(selected, 1);
				plannedM.addBehavior(bv.getBehavior().clone());
			}
		});
		// Font vergrößern
		Font f = b.getFont();
		// Wichtig: deriveFont(float) aufrufen, nicht deriveFont(int), die
		// bedeuten ganz unterschiedliche Sachen. Also f an die Zahl anhängen
		b.setFont(f.deriveFont(f.getSize() * 3f));
		add(b, new GridBaggins().row(1).col(1).epadx(10).fillV());

		JPanel plannedHeading = new JPanel();
		plannedHeading.add(new JLabel("Geplante Remote-Calls"));

		JButton clearAll = new JButton("Alle entfernen");
		clearAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int row=plannedM.getRowCount()-1; row>=0; row--) {
					if (plannedM.getValueAt(row, 0) instanceof PlannedBhvModel.Done) {
						plannedM.removeRow(row);
					}
				}
			}
		});
		plannedHeading.add(clearAll);
		
		JButton kill = new JButton("aktiven RC stornieren");
		kill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					rcCompnt.abortCurrentBehavior();
				} catch (IOException excp) {
					lg.warn(excp, "E/A-Problem beim Senden des Abbruchkommandos");
				}
				rcCompnt.fireDoneEvent(0);
			}
		});
		plannedHeading.add(kill);
		
		add(plannedHeading, new GridBaggins().row(0).col(2).epady(3));
		
		plannedBhvs.setTableHeader(null);

		add(new JScrollPane(plannedBhvs),
			new GridBaggins().row(1).col(2).weightx(0.6).weighty(1).fillHV()
			.epadx(10));

		rcCompnt.addDoneListener(new Runnable1<BehaviorExitStatus>() {
			public void run(BehaviorExitStatus status) {
				// Hauptarbeit
				plannedM.setLastCallDone(status);
				plannedM.callNextBehavior();
			}
		});
	}
}

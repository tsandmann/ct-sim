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

//$$ doc
public class RemoteCallViewer extends JPanel {
	private static final long serialVersionUID = - 3844905681548769569L;

	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.view.gui.RemoteCallViewer");

	static class BehaviorViewer extends JPanel {
		private static final long serialVersionUID = 4518196039674033397L;
		private final Behavior behavior;

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
					// p.getMax() oder p.getMin() koennen null sein,
					// in dem Fall ignorieren und Default nehmen
				}
				((DefaultEditor)js.getEditor()).getTextField().setColumns(
					numCols);
				add(js);
			}

			// Mindestbreite ausrechnen, damit ScrollPane bescheidweiss
			// Zwischenraeume
			int prefWidth = (getComponentCount() - 1) *
				((FlowLayout)getLayout()).getHgap();
			// Breiten der Komponenten
			for (int i = 0; i < getComponentCount(); i++)
				prefWidth += getComponent(i).getPreferredSize().width;

			setMinimumSize(new Dimension(prefWidth, getMinimumSize().height));
		}

		// An alle Kinder weitergeben
		@Override
		public void setForeground(Color fg) {
			for (int i = 0; i < getComponentCount(); i++)
				getComponent(i).setForeground(fg);
		}

		// An alle Kinder weitergeben
		@Override
		public void setEnabled(boolean enabled) {
			for (int i = 0; i < getComponentCount(); i++)
				getComponent(i).setEnabled(enabled);
		}

		Behavior getBehavior() {
			return behavior;
		}
	}

	static class BehaviorModel extends DefaultTableModel {
		private static final long serialVersionUID = - 3091736642693972956L;

		public BehaviorModel(int numCols) {
			super(0, numCols);
		}

		protected void addBehavior(Behavior b) {
			addRow(new Object[] { buildName(b), buildBhvViewer(b) });
		}

		protected JComponent buildBhvViewer(Behavior b) {
			return new BehaviorViewer(b);
		}

		protected JLabel buildName(Behavior b) {
    		JLabel rv = new JLabel(b.getName());
    		rv.setOpaque(true);
    		rv.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
    		return rv;
		}
	}

	class PlannedBhvModel extends BehaviorModel {
		abstract class StatusLabel extends JLabel {
			public StatusLabel(String label, String tooltip, Color c) {
				super(label);
				setToolTipText(tooltip);
				setOpaque(true);
				super.setBackground(c.darker());
				super.setForeground(Color.WHITE);
				setFont(getFont().deriveFont(Font.BOLD));
				setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
			}

			@Override
			public void setForeground(@SuppressWarnings("unused") Color fg) {
				// No-op: Wir setzen das im Konstruktor, die Tabelle soll's ab
				// dann nicht mehr aendern
			}

			@Override
			public void setBackground(@SuppressWarnings("unused") Color bg) {
				// No-op: Wir setzen das im Konstruktor, die Tabelle soll's ab
				// dann nicht mehr aendern
			}

			@Override
			public void setEnabled(@SuppressWarnings("unused") boolean b) {
				// No-op: Wir sind auf enabled und das soll so bleiben, weil
				// sonst der Text grau wird
			}
		}

		class Waiting extends StatusLabel {
			private static final long serialVersionUID = - 1396203136406798134L;

			public Waiting() {
				super("Wartet", "Remote-Call wartet, bis alle Calls " +
						"\u00FCber ihm abgeschlossen sind", Color.GRAY);
			}
		}

		class Running extends StatusLabel {
			private static final long serialVersionUID = - 7019340883477925888L;

			public Running() {
				super("L\u00E4uft", "Remote-Call wird vom Bot " +
						"gegenw\u00E4rtig ausgef\u00FChrt", Color.BLUE);
			}
		}

		class Done extends StatusLabel {
			private static final long serialVersionUID = - 2082537920466563306L;

			public Done(String label, String tooltip, Color c) {
				super(label, tooltip, c);
			}
		}

		/**
		 * Zeigt auf 1. Zelle der Zeile des Behaviors, das gerade
		 * ausgef&uuml;hrt wird
		 */
		private StatusLabel nowRunning = null;

		public PlannedBhvModel() {
			super(4);
		}

		private static final long serialVersionUID = 2841437768966488801L;

		class DeleteButton extends JButton implements ActionListener {
			private static final long serialVersionUID = 1802551443502887184L;

			private boolean isRunning;

			public DeleteButton() {
				setIcon(Config.getIcon("schliessen-hover"));
				addActionListener(this);

				setMaximumSize(new Dimension(24, 24));
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			}

			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
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

			private void removeThisRowFromTable() {
				for (int row = 0; row < getRowCount(); row++) {
					if (getValueAt(row, 3) == this) {
						removeRow(row);
						return;
					}
				}
				throw new IllegalStateException("Interner Fehler " +
						"(Selbstfindungsproblem): L\u00F6schen-Knopf " +
						"funktioniert nicht");
			}

			public void setRunning(boolean isRunning) {
				this.isRunning = isRunning;
				setToolTipText(isRunning
					? "Ausf\u00FChrung des Remote-Call abbrechen"
					: "Remote-Call l\u00F6schen");
			}
		}

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

		protected void callNextBehavior() {
			for (int row = 0; row < getRowCount(); row++) {
				if (getValueAt(row, 0) instanceof Done)
					continue;

				lg.fine("Starte n\u00E4chsten Remote-Call");
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

		private void setRunningLabel(int row) {
			nowRunning = new Running();
			setValueAt(nowRunning, row, 0);
		}

		private void disableRow(int row) {
			for (int col = 0; col < getColumnCount(); col++) {
				// Spalte 3 (Loeschen-Button) nicht anfassen
				if (col != 3)
					((JComponent)getValueAt(row, col)).setEnabled(false);
			}
		}

		private void sendBehaviorRequest(int row) {
			try {
				((BehaviorViewer)getValueAt(row, 2)).getBehavior().call();
			} catch (IOException e) {
				lg.warn(e, "Schlimm: E/A-Problem beim Senden des " +
				"Remote-Call; ignoriere Remote-Call und fahre fort");
			}
		}

		protected void setLastCallDone(BehaviorExitStatus status) {
			for (int row = 0; row < getRowCount(); row++) {
				if (getValueAt(row, 0) == nowRunning) {
					setDoneLabel(row, status);
					enableRow(row);
					setDeleteButton(row, false);
					return;
				}
			}
			lg.warn("Ein Remote Call wurde beendet, der laut Liste gar nicht läuft");
			//throw new IllegalStateException("Interner Fehler");
		}

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
				default:
					throw new IllegalStateException();
			}

			nowRunning = null;
			setValueAt(new Done(status.toString(), tooltip, color), row, 0);
		}

		private void enableRow(int row) {
			for (int col = 0; col < getColumnCount(); col++) {
				// Spalte 3 (Loeschen-Button) nicht anfassen
				if (col != 3)
					((JComponent)getValueAt(row, col)).setEnabled(true);
			}
		}

		private void setDeleteButton(int row, boolean isRunning) {
			((DeleteButton)getValueAt(row, 3)).setRunning(isRunning);
		}
	}

	private final RemoteCallCompnt rcCompnt;

	private ComponentTable buildCompntTable(TableModel m) {
		final ComponentTable rv = new ComponentTable(m);
		m.addTableModelListener(new TableModelListener() {
			public void tableChanged(
			@SuppressWarnings("unused") TableModelEvent e) {
				rv.accomodateContent();
			}
		});
		return rv;
	}

	private void requestRCallList(RemoteCallCompnt c) {
		try {
			c.listRemoteCalls();
		} catch (IOException e) {
			lg.warn(e, "E/A-Problem aufgetreten, als die Anforderung der " +
				"Remote-Call-Liste gesendet wurde; wer wei\u00DF, ob " +
				"das jetzt funktioniert mit den Remote-Calls");
		}
	}

	public RemoteCallViewer(final RemoteCallCompnt rcCompnt) {
		this.rcCompnt = rcCompnt;

		requestRCallList(rcCompnt);

		final BehaviorModel availM = new BehaviorModel(2) {
			private static final long serialVersionUID = 3932551442111274878L;

			@Override
			public boolean isCellEditable(@SuppressWarnings("unused") int row,
			int column) {
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
		availHeading.add(new JLabel("Verf\u00FCgbare Remote-Calls"));

		JButton refresh = new JButton("Holen");
		refresh.addActionListener(new ActionListener() {
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				while (availM.getRowCount() > 0)
					availM.removeRow(0);
				requestRCallList(rcCompnt);
			}
		});
		availHeading.add(refresh);

		add(availHeading, new GridBaggins().row(0).col(0).epady(3));

		add(new JScrollPane(availBhvs),
			new GridBaggins().row(1).col(0).weightx(0.4).weighty(1).fillHV()
			.epadx(10));

		final ComponentTable plannedBhvs = buildCompntTable(plannedM);

		/*
		 * Workaround fuer Bug in JTable: Wenn das Model sich aendert (der
		 * JButton im Model loescht Zeilen aus dem Model), dann kriegt wir u.U.
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

			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				int selected = availBhvs.getSelectedRow();
				if (selected == -1)
					return;
				BehaviorViewer bv = (BehaviorViewer)availBhvs.getModel()
					.getValueAt(selected, 1);
				plannedM.addBehavior(bv.getBehavior().clone());
			}
		});
		// Font vergroessern
		Font f = b.getFont();
		// Wichtig: deriveFont(float) aufrufen, nicht deriveFont(int), die
		// bedeuten ganz unterschiedliche Sachen. Also f an die Zahl anhaengen
		b.setFont(f.deriveFont(f.getSize() * 3f));
		add(b, new GridBaggins().row(1).col(1).epadx(10).fillV());

		JPanel plannedHeading = new JPanel();
		plannedHeading.add(new JLabel("Geplante Remote-Calls"));

		JButton clearAll = new JButton("Alle entfernen");
		clearAll.addActionListener(new ActionListener() {
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
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
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
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

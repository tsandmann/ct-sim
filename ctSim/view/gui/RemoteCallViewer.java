package ctSim.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
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

	static class PlannedBhvModel extends BehaviorModel {
		abstract static class StatusLabel extends JLabel {
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
				// No-op: Wir setzen das im Konstruktor, die Tabelle soll's ab
				// dann nicht mehr aendern
			}
		}

		static class Waiting extends StatusLabel {
			private static final long serialVersionUID = - 1396203136406798134L;

			public Waiting() {
				super("Wartet", "Remote-Call wartet, bis alle Calls " +
						"\u00FCber ihm abgeschlossen sind", Color.GRAY);
			}
		}

		static class Running extends StatusLabel {
			private static final long serialVersionUID = - 7019340883477925888L;

			public Running() {
				super("L\u00E4uft", "Remote-Call wird vom Bot " +
						"gegenw\u00E4rtig ausgef\u00FChrt", Color.BLUE);
			}
		}

		abstract static class Done extends StatusLabel {
			public Done(String label, String tooltip, Color c) {
				super(label, tooltip, c);
			}
		}

		static class DoneSuccess extends Done {
			private static final long serialVersionUID = 4012930442824710705L;

			public DoneSuccess() {
				super("OK", "Remote-Call erfolgreich abgeschlossen",
					new Color(0, 160, 0));
			}
		}

		static class DoneFailure extends Done {
			private static final long serialVersionUID = - 7327825924461955645L;

			public DoneFailure() {
				super("Fehler", "Remote-Call ist in die Hose gegangen",
					Color.RED);
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

		protected JComponent buildDelButton() {
			JButton rv = new JButton(
				new AbstractAction("", Config.getIcon("schliessen-hover")) {
					private static final long serialVersionUID =
						8772641798764190954L;

					public void actionPerformed(ActionEvent e) {
						for (int row = 0; row < getRowCount(); row++) {
							if (getValueAt(row, 3) == e.getSource()) {
								removeRow(row);
								return;
							}
						}
						throw new IllegalStateException(
							"Interner Fehler (Selbstfindungsproblem): " +
							"L\u00F6schen-Knopf funktioniert nicht");
					}
			});
			rv.setBorder(null);
			rv.setToolTipText("Remote-Call l\u00F6schen");
			rv.setMaximumSize(new Dimension(24, 24));
			rv.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			return rv;
		}

		@Override
		protected void addBehavior(Behavior b) {
			addRow(new Object[] {
				new Waiting(),
				buildName(b),
				buildBhvViewer(b),
				buildDelButton() });
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
				
				callBehavior(row);

				return;
			}
			lg.fine("Keine Remote-Calls mehr abzuarbeiten");
		}
		
		protected void lastCallIsDone(boolean errorOccurred) {
			for (int row = 0; row < getRowCount(); row++) {
				if (getValueAt(row, 0) == nowRunning) {
					setBhvDone(row, errorOccurred);
					return;
				}
			}
			throw new IllegalStateException("Interner Fehler");
		}
		
		private void callBehavior(int row) {
			nowRunning = new Running();
			setValueAt(nowRunning, row, 0);
			for (int col = 0; col < getColumnCount(); col++)
				((JComponent)getValueAt(row, col)).setEnabled(false);

			try {
				((BehaviorViewer)getValueAt(row, 2)).getBehavior().call();
			} catch (IOException e) {
				lg.warn(e, "Schlimm: E/A-Problem beim Senden des " +
						"Remote-Call; ignoriere Remote-Call und fahre fort");
			}
		}
		
		private void setBhvDone(int row, boolean errorOccurred) {
			nowRunning = null;
			setValueAt(errorOccurred ? new DoneFailure() : new DoneSuccess(), 
				row, 0);
			for (int col = 0; col < getColumnCount(); col++)
				((JComponent)getValueAt(row, col)).setEnabled(true);
		}
	}
	
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

	public RemoteCallViewer(final RemoteCallCompnt rcCompnt) {
		lg.info("Fordere beim Bot eine Liste der m\u00F6glichen " +
				"Remote-Calls an");
		try {
			rcCompnt.requestRemoteCallList();
		} catch (IOException e) {
			lg.warn(e, "E/A-Problem aufgetreten, als die Anforderung der " +
					"Remote-Call-Liste gesendet wurde; wer wei\u00DF, ob " +
					"das jetzt funktioniert mit den Remote-Calls");
		}

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
        // Ganze Zeile markieren, nicht nur Zelle
        availBhvs.setColumnSelectionAllowed(false);
        availBhvs.setRowSelectionAllowed(true);

		setLayout(new GridBagLayout());

		final PlannedBhvModel plannedM = new PlannedBhvModel();

		add(new JLabel("Verf\u00FCgbare Remote-Calls"),
			new GridBaggins().row(0).col(0).epady(3));
		add(new JScrollPane(availBhvs),
			new GridBaggins().row(1).col(0).weightx(0.5).weighty(1).fillHV()
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
				BehaviorViewer bv = (BehaviorViewer)availBhvs.getModel()
					.getValueAt(availBhvs.getSelectedRow(), 1);
				plannedM.addBehavior(bv.getBehavior().clone());
			}
		});
		// Font vergroessern
		Font f = b.getFont();
		// Wichtig: deriveFont(float) aufrufen, nicht deriveFont(int), die
		// bedeuten ganz unterschiedliche Sachen. Also f an die Zahl anhaengen
		b.setFont(f.deriveFont(f.getSize() * 3f));
		add(b, new GridBaggins().row(1).col(1).epadx(10).fillV());

		add(new JLabel("Geplante Remote-Calls"),
			new GridBaggins().row(0).col(2).epady(3));

		plannedBhvs.setTableHeader(null);

		add(new JScrollPane(plannedBhvs),
			new GridBaggins().row(1).col(2).weightx(0.5).weighty(1).fillHV()
			.epadx(10));

		rcCompnt.addDoneListener(new Runnable1<Integer>() {
			public void run(Integer exitStatus) {
				// Gesundheitscheck
				if (exitStatus != 0 && exitStatus != 1) {
					lg.warn("Unerwarteter Exit-Status bei Remote-Call (Wert: " +
							"%d, erwartet: 0 oder 1); behandle wie " +
							"Exit-Status f\u00FCr Fehler", exitStatus);
				}

				// Hauptarbeit
				plannedM.lastCallIsDone(exitStatus != 1);
				plannedM.callNextBehavior();
			}
		});
	}
}

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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ctSim.model.bots.components.MousePictureComponent;
import ctSim.util.Runnable1;

/** Mausbild-Anzeige */
public class MousePictureViewer extends GuiBotBuisitor {
	/** UID */
	private static final long serialVersionUID = - 2167640810854877294L;

	/** Bild-Anzeige */
	public static class ImageViewer extends JPanel implements Runnable1<Image> {
		/** UID */
		private static final long serialVersionUID = 3878110649950448386L;

		/** Bild */
		private Image image;
		/** Breite */
		private final int targetWidth;
		/** Höhe */
		private final int targetHeight;

		/**
		 * @param scaleFactor	Skalierung
		 * @param c				Mausbild-Komponente
		 */
		public ImageViewer(double scaleFactor, MousePictureComponent c) {
			c.addImageListener(this);
			setToolTipText(c.getDescription());
			setBorder(BorderFactory.createLoweredBevelBorder());	//$$$ bevel, insets gehen nicht
			targetWidth  = (int)Math.round(scaleFactor * c.getWidth());
			targetHeight = (int)Math.round(scaleFactor * c.getHeight());
		}

		// Wir sind ja ein ImageListener, daher brauchen wir eine run(Image)-Methode
		/**
		 * Methode einer Swing-Komponente, aber thread-sicher
		 *
		 * @param img	Bild
		 */
		@Override
		public synchronized void run(Image img) {
			this.image = img.getScaledInstance(targetWidth, targetHeight,
					Image.SCALE_SMOOTH);
			repaint();
		}

		/**
		 * @see javax.swing.JComponent#paint(java.awt.Graphics)
		 */
		@Override
		public void paint(Graphics g) {
			// löschen
			g.setColor(getBackground());
			g.fillRect(0, 0, getSize().width, getSize().height);
			// malen
			g.drawImage(image, 0, 0, null);
		}

		/**
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		@Override
		public Dimension getPreferredSize() {
			Insets is = getBorder().getBorderInsets(this);
			return new Dimension(targetWidth  + is.left + is.right,
					targetHeight + is.top  + is.bottom);
		}

		/**
		 * @see javax.swing.JComponent#getMinimumSize()
		 */
		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}
	}

	/**
	 * @param compnt	Mausbild-Komponente
	 */
	public void buisitMousePictureViewer(final MousePictureComponent compnt) {
		// Container
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(compnt.getName()));
		JPanel controls = new JPanel();

		/* 1/3: Anzeige des Mausbilds */
		ImageViewer v = new ImageViewer(6, compnt);
		p.add(v, BorderLayout.CENTER);
		v.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					compnt.requestPicture();
				} catch (IOException e1) {
					e1.printStackTrace();	//$$$ Excp
				}
			}
		});

		/* 2/3: Knopf */
		final JButton bt = new JButton("Holen");
		bt.setToolTipText("Fordert beim Bot ein Bild dessen an, was der " +
				"Maussensor sieht");
		bt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					compnt.requestPicture();
				} catch (IOException e1) {
					e1.printStackTrace();	//$$$ Excp
				}
			}
		});
		controls.add(bt);

		/* 3/3: Checkbox */
		final JCheckBox cb = new JCheckBox("laufend");
		cb.setToolTipText("Fordert das nächste an, sobald ein Mausbild " +
				"übertragen ist");
		cb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// wenn Checkbox an, Button deaktivieren
				bt.setEnabled(! cb.isSelected());
				try {
					compnt.requestPicture();
				} catch (IOException e1) {
					e1.printStackTrace(); //$$$ Excp
				}
			}
		});
		controls.add(cb);
		compnt.addCompletionListener(new Runnable() {
			@Override
			public void run() {
				if (cb.isSelected()) {
					try {
						compnt.requestPicture();
					} catch (IOException e1) {
						e1.printStackTrace(); //$$$ Excp
					}
				}
			}
		});

		/* ausliefern */
		p.add(controls, BorderLayout.SOUTH);

		add(p);
		add(Box.createRigidArea(new Dimension(0, 5)));
	}
}

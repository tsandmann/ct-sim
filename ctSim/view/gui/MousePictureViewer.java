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

//$$ doc
public class MousePictureViewer extends GuiBotBuisitor {
	private static final long serialVersionUID = - 2167640810854877294L;

	public static class ImageViewer extends JPanel implements Runnable1<Image> {
		private static final long serialVersionUID = 3878110649950448386L;

		private Image image;
		private final int targetWidth;
		private final int targetHeight;

		public ImageViewer(double scaleFactor, MousePictureComponent c) {
			c.addImageListener(this);
			setToolTipText(c.getDescription());
			setBorder(BorderFactory.createLoweredBevelBorder()); //$$$ bevel, insets gehen nicht
			targetWidth  = (int)Math.round(scaleFactor * c.getWidth());
			targetHeight = (int)Math.round(scaleFactor * c.getHeight());
		}

		// Wir sind ja ein ImageListener, daher brauchen wir eine
		// run(Image)-Methode
		/** Methode einer Swing-Komponente, aber thread-sicher */
		public synchronized void run(Image img) {
			this.image = img.getScaledInstance(targetWidth, targetHeight,
				Image.SCALE_SMOOTH);
			repaint();
		}

		@Override
		public void paint(Graphics g) {
			// Loeschen
			g.setColor(getBackground());
			g.fillRect(0, 0, getSize().width, getSize().height);
			// Malen
			g.drawImage(image, 0, 0, null);
		}

		@Override
		public Dimension getPreferredSize() {
			Insets is = getBorder().getBorderInsets(this);
			return new Dimension(targetWidth  + is.left + is.right,
			                     targetHeight + is.top  + is.bottom);
		}

		//$$ Koennte beim resize auch wirklich skalieren, wozu haben wir den scaleFactor
		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}
	}

	public void buisitMousePictureViewer(final MousePictureComponent compnt) {
		// Container
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(compnt.getName()));
		JPanel controls = new JPanel();

		// 1/3: Anzeige des Mausbilds
		ImageViewer v = new ImageViewer(6, compnt);
		p.add(v, BorderLayout.CENTER);
		v.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(@SuppressWarnings("unused") MouseEvent e) {
				try {
					compnt.requestPicture();
				} catch (IOException e1) {
					e1.printStackTrace(); //$$$ Excp
				}
			}
		});

		// 2/3: Knopp
		final JButton bt = new JButton("Holen");
		bt.setToolTipText("Fordert beim Bot ein Bild dessen an, was der " +
			"Maussensor sieht");
		bt.addActionListener(new ActionListener() {
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				try {
					compnt.requestPicture();
				} catch (IOException e1) {
					e1.printStackTrace(); //$$$ Excp
				}
			}
		});
		controls.add(bt);

		// 3/3: Checkbox
		final JCheckBox cb = new JCheckBox("laufend");
		cb.setToolTipText("Fordert das n\u00E4chste an, sobald ein Mausbild " +
			"\u00FCbertragen ist");
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				// Wenn Checkbox an, Button deaktivieren
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

		// Ausliefern
		p.add(controls, BorderLayout.SOUTH);

		add(p);
		add(Box.createRigidArea(new Dimension(0, 5)));
	}
}

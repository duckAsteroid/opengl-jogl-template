package com.asteroid.duck.opengl.util.resources.font;

import com.asteroid.duck.opengl.util.resources.font.factory.FontTextureData;
import com.asteroid.duck.opengl.util.resources.font.factory.FontTextureDataFactory;
import com.asteroid.duck.opengl.util.resources.font.factory.GlyphImage;
import com.asteroid.duck.opengl.util.resources.font.factory.ImageRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A disposable swing app for playing around with FontTexture generated Glyph images
 * rendering and bounds...
 */
public class FontTextureDebugWindow {

	private JFrame frame;
	private JSplitPane splitPane;
	private GlyphRenderer glyphy;
	private JComboBox<Character> objectComboBox;
	private JLabel property1Label;
	private JLabel property2Label;
	private JLabel property3Label;
	private JLabel property4Label;

	private final FontTextureData fontTexture;


	public FontTextureDebugWindow(FontTextureData fontTexture) {
		this.fontTexture = fontTexture;
		createAndShowGUI();
	}

	public static Rectangle renderFromStrip(GlyphData data, Point point, BufferedImage strip, Graphics2D g2d) {
		Rectangle extent = data.bounds();
		Point datumOffset = data.datumOffset();
		ImageRenderer imageHelper = new ImageRenderer(g2d, strip);
		Rectangle dest = new Rectangle(point.x, point.y, extent.width, extent.height);
		dest.translate(-datumOffset.x, -datumOffset.y);
		imageHelper.drawImage(extent, dest);
		return dest;
	}

	private void createAndShowGUI() {
		frame = new JFrame("Image with Details");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				//OpenGLTest.destroyGL();
			}
		});

		// Sample image - replace with your actual image
		glyphy = new GlyphRenderer(fontTexture.combined());

		// Create sample objects
		List<Character> objects = fontTexture.glyphData().keySet().stream().sorted().toList();

		// Combo box for object selection
		objectComboBox = new JComboBox<>(objects.toArray(new Character[0]));
		objectComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateObjectDetails();
			}
		});

		// Labels for object properties
		property1Label = new JLabel("Property 1: ");
		property2Label = new JLabel("Property 2: ");
		property3Label = new JLabel("Property 3: ");
		property4Label = new JLabel("Property 3: ");

		// Panel for object details
		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout(new GridLayout(5, 1));
		detailsPanel.add(objectComboBox);
		detailsPanel.add(property1Label);
		detailsPanel.add(property2Label);
		detailsPanel.add(property3Label);
		detailsPanel.add(property4Label);
		detailsPanel.setBorder(new TitledBorder("Object Details"));

		// Split pane
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, glyphy, detailsPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(400); // Initial divider position

		frame.getContentPane().add(splitPane);
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void updateObjectDetails() {
		Character character = (Character) objectComboBox.getSelectedItem();
		if (character != null) {
			GlyphData g = fontTexture.glyphData().get(character);
			GlyphImage image = fontTexture.glyphImages().get(character);
			property1Label.setText("datumOffset: " + g.datumOffset());
			property2Label.setText("strip bounds: " + g.bounds());
			property3Label.setText("src_bounds: "+ image.bounds());
			property4Label.setText("advance:" + g.advance());
			glyphy.setGlyph(g);
		}
	}

	public static void main(String[] args) {
		//OpenGLTest.initGL();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Font monospacedFont = new Font(Font.SERIF, Font.PLAIN, 100);
				FontTextureDataFactory factory = new FontTextureDataFactory(monospacedFont, true);
				new FontTextureDebugWindow(factory.createFontTextureData());
			}
		});
	}

	public static class GlyphRenderer extends JComponent {
		private GlyphData glyph;
		private final BufferedImage strip;
		private double scale = 4.0;

		public GlyphRenderer(BufferedImage strip) {
			this.strip = strip;
			setBackground(Color.BLACK);
			repaint();
		}

		public void setGlyph(GlyphData g) {
			this.glyph = g;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(getBackground());
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.scale(scale, scale);

			// paint the glyph image
			final int x = 100;
			final int y = 100;
			Point datum = new Point(x, y);
			g2d.setColor(Color.BLUE);
			g2d.drawLine(datum.x, 0, datum.x, getHeight());
			g2d.drawLine(0, datum.y, getHeight(), datum.y);

			if (glyph != null) {
				//g2d.scale(scale, -scale);
				//g2d.translate(0, -getHeight());
				Rectangle target = renderFromStrip(glyph, datum, strip, g2d);

				g2d.setColor(Color.CYAN);
				g2d.drawRect(target.x, target.y, target.width, target.height);
			}

			g2d.dispose();
		}
	}
}

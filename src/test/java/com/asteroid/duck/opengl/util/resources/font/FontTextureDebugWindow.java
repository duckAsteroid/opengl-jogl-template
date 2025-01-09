package com.asteroid.duck.opengl.util.resources.font;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
		glyphy = new GlyphRenderer();

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
			property2Label.setText("strip extent: " + g.extent());
			property3Label.setText("src_bounds: "+ image.bounds());
			property4Label.setText("advance:" + g.advance());
			glyphy.setGlyph(image);
		}
	}

	public static void main(String[] args) {
		//OpenGLTest.initGL();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Font monospacedFont = new Font(Font.SERIF, Font.PLAIN, 100);
				FontTextureFactory factory = new FontTextureFactory(monospacedFont, true);
				factory.setImageDumpPath(Path.of("font-images"));
				factory.debugBoundary = true;
				new FontTextureDebugWindow(factory.createFontTextureData());
			}
		});
	}

	public static class GlyphRenderer extends JComponent {
		private BufferedImage image;
		private Rectangle rectangle;
		private Point datum = new Point(0,0);
		private double scale = 4.0;

		public GlyphRenderer() {
			setBackground(Color.BLACK);
			repaint();
		}

		public void setGlyph(GlyphImage image) {
			this.rectangle = image.bounds().getBounds();
			rectangle.translate(image.x(), image.y());
			this.datum = new Point(image.x(), image.y());
			this.image = image.image();
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(getBackground());
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.setTransform(AffineTransform.getScaleInstance(scale, scale));
			if (image != null) {
				g2d.setColor(Color.CYAN);
				g2d.drawRect(0,0, image.getWidth(), image.getHeight());
				g2d.drawImage(image, 0,0, this);
			}
			if (rectangle != null) {
				g2d.setColor(Color.BLUE);
				g2d.drawLine(datum.x, 0 , datum.x, image.getHeight());
				g2d.setColor(Color.magenta);
				g2d.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
				g2d.setColor(Color.BLUE);
				g2d.drawLine(0, datum.y,image.getWidth(), datum.y);
			}
			g2d.dispose();
		}
	}
}

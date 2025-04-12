package com.asteroid.duck.opengl.util.resources.font;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class FontTextureStringDebugWindow {

	private JFrame frame;
	private GlyphStringRenderer glyphy;
	private JTextField text;

	private final FontTextureData fontTexture;


	public FontTextureStringDebugWindow(Font f, FontTextureData fontTexture) {
		this.fontTexture = fontTexture;
		createAndShowGUI(f.toString());
	}

	private void createAndShowGUI(String title) {
		frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				//OpenGLTest.destroyGL();
			}
		});

		// Sample image - replace with your actual image
		glyphy = new GlyphStringRenderer(fontTexture);

		text = new JTextField("The quick brown fox jumps over the lazy dog. (1234567890)");
		text.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				glyphy.setText(text.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				glyphy.setText(text.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				glyphy.setText(text.getText());
			}
		});
		glyphy.setText(text.getText());

		JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
		scaleSlider.addChangeListener(e -> {
			double scale = scaleSlider.getValue() / 100.0;
			scale = 1.0 + (scale * 10.0);
			glyphy.setScale(scale);
		});

		JCheckBox chkDatum = new JCheckBox("Datum");
		chkDatum.addChangeListener(e -> {
			glyphy.setDebugDatum(chkDatum.isSelected());
		});

		JCheckBox chkGlyph = new JCheckBox("Glyphs");
		chkGlyph.addChangeListener(e -> {
			glyphy.setDebugGlyphBoundaries(chkGlyph.isSelected());
		});

		JCheckBox chkString = new JCheckBox("String");
		chkString.addChangeListener(e -> {
			glyphy.setDebugStringBoundary(chkString.isSelected());
		});

		// Panel for object details
		JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		content.add(text, gbc);

		gbc.gridy++;
		content.add(scaleSlider, gbc);

		gbc.gridy++;
		content.add(chkDatum, gbc);
		gbc.gridy++;
		content.add(chkGlyph, gbc);
		gbc.gridy++;
		content.add(chkString, gbc);

		gbc.gridy++;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		content.add(glyphy, gbc);
		content.setBorder(new TitledBorder("Object Details"));

		frame.getContentPane().add(content);
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		//OpenGLTest.initGL();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Font monospacedFont = new Font(Font.SERIF, Font.BOLD, 50);
				FontTextureFactory factory = new FontTextureFactory(monospacedFont, false);
				factory.imageDumpPath = Path.of("font-images");
				factory.debugBoundary = true;
				new FontTextureStringDebugWindow(monospacedFont, factory.createFontTextureData());
			}
		});
	}

	public static class GlyphStringRenderer extends JComponent {
		private final FontTextureData glyphData;
		private String text;
		private double scale = 4.0;
		private boolean debugGlyphBoundaries = false;
		private boolean debugDatum = false;
		private boolean debugStringBoundary = false;
		private boolean withCoordinates = true;

		public GlyphStringRenderer(FontTextureData glyphData) {
			this.glyphData = glyphData;
			setBackground(Color.BLACK);
			repaint();
		}

		public double getScale() {
			return scale;
		}

		public void setScale(double scale) {
			this.scale = scale;
			repaint();
		}

		public boolean isDebugGlyphBoundaries() {
			return debugGlyphBoundaries;
		}

		public void setDebugGlyphBoundaries(boolean debugGlyphBoundaries) {
			this.debugGlyphBoundaries = debugGlyphBoundaries;
			repaint();
		}

		public boolean isDebugDatum() {
			return debugDatum;
		}

		public void setDebugDatum(boolean debugDatum) {
			this.debugDatum = debugDatum;
			repaint();
		}

		public boolean isDebugStringBoundary() {
			return debugStringBoundary;
		}

		public void setDebugStringBoundary(boolean debugStringBoundary) {
			this.debugStringBoundary = debugStringBoundary;
			repaint();
		}

		public String getText() {
			return text;
		}

		public void setText(String txt) {
			this.text = txt;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(getBackground());
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.scale(scale, scale);
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .6f));
			// paint the glyph image
			final int x = 10;
			final int y = glyphData.combinedSize().height + 5;
			Point datum = new Point(x, y);
			if (debugDatum) {
				g2d.setColor(Color.BLUE);
				g2d.drawLine(0, datum.y, getWidth(), datum.y);
			}

			if (text != null) {
				//g2d.scale(scale, -scale);
				//g2d.translate(0, -getHeight());
				Rectangle stringExtent = new Rectangle(datum.x, datum.y, 0,0);
				for (Character c : text.toCharArray()) {

					// render a glyph
					GlyphData glyph = glyphData.glyphData().get(c);
					Rectangle target = FontTextureDebugWindow.renderFromStrip(glyph, datum, glyphData.combined(), g2d);

					if (debugDatum) {
						g2d.setColor(Color.BLUE);
						g2d.drawLine(datum.x, 0, datum.x, getHeight());
						g2d.setColor(Color.magenta);
						Point offset = new Point(glyph.datumOffset());
						offset.translate(-datum.x, -datum.y);
						g2d.drawLine(datum.x, datum.y, -offset.x, -offset.y);
						if (withCoordinates) {
							Point coord = glyph.datumOffset();
							String txt = "/("+coord.x+","+coord.y+")";
							g2d.drawString(txt, datum.x, datum.y);
						}
					}

					// draw the glyph debug box
					if (debugGlyphBoundaries) {
						g2d.setColor(Color.CYAN);
						g2d.drawRect(target.x, target.y, target.width, target.height);
						String txt = "/"+target.width +"x"+target.height+"@("+target.x+","+target.y+")";
						g2d.drawString(txt, target.x, target.y);
					}

					// move the cursor
					int extent = target.x - datum.x + target.width;
					datum.translate(extent, 0);

					// add to string bounds
					stringExtent = stringExtent.union(target);
				}

				// draw the string bounds box
				if (debugStringBoundary) {
					g2d.setColor(Color.YELLOW);
					g2d.drawRect(stringExtent.x, stringExtent.y, stringExtent.width, stringExtent.height);
				}
			}

			g2d.dispose();
		}
	}
}

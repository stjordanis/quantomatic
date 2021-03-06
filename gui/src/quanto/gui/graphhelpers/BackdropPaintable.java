package quanto.gui.graphhelpers;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationServer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import quanto.core.data.Edge;
import quanto.core.data.Vertex;

/**
 *
 * @author alemer
 */
public class BackdropPaintable implements VisualizationServer.Paintable {

	private Color pageBackground = Color.WHITE;
	private Layout<Vertex, Edge> layout;

	public BackdropPaintable(Layout<Vertex, Edge> layout) {
		// FIXME: better to update the size when it changes?
		this.layout = layout;
	}

	public Color getBackgroundColor() {
		return pageBackground;
	}

	public void setBackgroundColor(Color color) {
		pageBackground = color;
	}

	public void paint(Graphics g) {
		Graphics2D gr = (Graphics2D) g;
		Color oldColor = g.getColor();
		Dimension size = layout.getSize();
		Rectangle2D bounds = new Rectangle2D.Double(0, 0, size.getWidth(), size.getHeight());
		g.setColor(pageBackground);
		gr.fill(bounds);
		g.setColor(Color.black);
		gr.draw(bounds);
		g.setColor(oldColor);
	}

	public boolean useTransform() {
		return true;
	}
}

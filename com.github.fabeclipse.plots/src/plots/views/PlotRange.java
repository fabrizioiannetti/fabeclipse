package plots.views;

import org.eclipse.swt.graphics.Rectangle;

/**
 * - Each plot has a span in the domain and co-domain.
 * - The View keeps a visible range in the domain that is common to all plots
 * - Each plot has also a display rectangle (in widget coordinates)
 * 
 * Here we need to map the plot data into the display rectangle, i.e.
 *  
 *  for each x in the display rectangle find the corresponding data value.
 *  which translates in having a scale factor from display coordinates to domain indexes
 *  
 * @author iannetti
 *
 */
public class PlotRange {
	// the data span
	private int dataStart;
	private int dataLength;

	// the range itself
	private int rangeStart;
	private int rangeLength;

	// the display rectangle where range is mapped
	private Rectangle displayRect;

	public PlotRange(int dataStart, int dataLength, Rectangle displayRect) {
		this.dataStart = dataStart;
		this.dataLength = dataLength;
		this.rangeStart = dataStart;
		this.rangeLength = dataLength;
		this.displayRect = displayRect;
	}

	public PlotRange setRange(int rangeStart, int rangeLength) {
		this.rangeStart = rangeStart;
		this.rangeLength = rangeLength;
		return this;
	}

	public PlotRange setDisplayRect(Rectangle r) {
		displayRect.x = r.x;
		displayRect.y = r.y;
		displayRect.width = r.width;
		displayRect.height = r.height;
		return this;
	}
	
	/**
	 * Return a rectangle that contains the whole data span
	 * and has the same scale as the given one has for the range.
	 * 
	 * @return the domain display rectangle
	 */
	public Rectangle domainDisplayRect() {
		Rectangle s = new Rectangle(displayRect.x, displayRect.y, displayRect.width, displayRect.height);
		s.x += (dataStart - rangeStart) * dataLength / rangeLength;
		s.width = displayRect.width * dataLength / rangeLength;
		return s;
	}

	/**
	 * Clip the display rectangle so that it does not exceed the
	 * data domain when mapped to the range (the range can
	 * exceed the data domain).
	 * 
	 * @return the clipped rectangle
	 */
	public Rectangle clippedDisplayRect() {
		Rectangle r = new Rectangle(displayRect.x, displayRect.y, displayRect.width, displayRect.height);
		r.intersect(domainDisplayRect());
		return r;
	}

	/**
	 * Return the factor that converts from display coordinates
	 * to domain values where the given rectangle map to the
	 * full domain.
	 * 
	 * X = f * (x - offsetX)
	 * 
	 * if x is in the x range of the rectangle returned by {@link #clippedDisplayRect()}
	 * then it is guaranteed that X is within the domain range.
	 * 
	 * offsetX can be obtained by calling the method {@link #offsetX()}.
	 * 
	 * @return the scale factor f
	 */
	public float scaleFactorX() {
		float f = displayRect.width / rangeLength;
		return f;
	}

	/**
	 * The offset to be used in the formula described in {@link #scaleFactorX()}.
	 * 
	 * @return the offset
	 */
	public int offsetX() {
		return displayRect.x;
	}
}

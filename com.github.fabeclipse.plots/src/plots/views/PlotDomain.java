package plots.views;

import org.eclipse.swt.graphics.Rectangle;

public class PlotDomain {
	// the data span
	private int dataStart;
	private int dataLength;
	
	// the visible window
	private PlotRange range;

	public PlotDomain(int dataStart, int dataLength, PlotRange range) {
		this.dataStart = dataStart;
		this.dataLength = dataLength;
		this.range = range;
	}

	/**
	 * Return a rectangle that contains the whole data span
	 * and has the same scale as the given one has for the range.
	 * 
	 * @return the domain display rectangle
	 */
	public Rectangle domainDisplayRect(Rectangle displayRect) {
		Rectangle s = new Rectangle(displayRect.x, displayRect.y, displayRect.width, displayRect.height);
		s.x += (dataStart - range.getRangeStart()) * dataLength / range.getRangeLength();
		s.width = displayRect.width * dataLength / range.getRangeLength();
		return s;
	}

}

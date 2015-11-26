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

	public PlotRange(int dataStart, int dataLength) {
		this.dataStart = dataStart;
		this.dataLength = dataLength;
		this.rangeStart = dataStart;
		this.rangeLength = dataLength;
	}

	public PlotRange setRange(int rangeStart, int rangeLength) {
		this.rangeStart = rangeStart;
		this.rangeLength = rangeLength;
		return this;
	}

	/**
	 * Return a rectangle that contains the whole data span
	 * and has the same scale as the given one has for the range.
	 * 
	 * @return the domain display rectangle
	 */
	public Rectangle domainDisplayRect(Rectangle displayRect) {
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
	public Rectangle clippedDisplayRect(Rectangle displayRect) {
		Rectangle r = new Rectangle(displayRect.x, displayRect.y, displayRect.width, displayRect.height);
		r.intersect(domainDisplayRect(displayRect));
		return r;
	}

	/**
	 * Return the factor that converts from display coordinates
	 * to domain values where the given rectangle maps to the
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
	public float scaleFactorX(Rectangle displayRect) {
		float f = displayRect.width / rangeLength;
		return f;
	}

	/**
	 * The offset to be used in the formula described in {@link #scaleFactorX()}.
	 * 
	 * @return the offset
	 */
	public int offsetX(Rectangle displayRect) {
		return displayRect.x;
	}

	public int getDomainStartIndex(int[] domain) {
		int from = rangeStart;
		// check if the requested position is outside
		// the domain
		if (from <= domain[0])
			return domain[0];
		if (from >= domain[domain.length - 1])
			return domain[domain.length - 1];

		// TODO: can I use the stdlib binary search here?
		int i,j, k;
		i = 0;
		j = domain.length;
		k = (i + j) / 2;
		while (!(domain[k] <= from && domain[k + 1] > from)) {
			System.out.printf("xstart: check k=%d for vl=%d\n", k, from);
			if (domain[k] < from) {// 12 -> [10 11 12] -> [11 12]
				i = k;
			} else {
				j = k;
			}
			k = (i + j) / 2;
		}
		System.out.printf("xstart: k=%d for vl=%d\n", k, from);
		return k;
	}

	public int getDomainEndIndex(int[] indexes) {
		int to = rangeStart + rangeLength;
		// check if the requested position is outside
		// the domain
		if (to <= indexes[0])
			return 0;
		if (to >= indexes[indexes.length - 1])
			return indexes.length - 1;

		// TODO: can I use the stdlib binary search here?
		int i,j, k;
		i = 0;
		j = indexes.length;
		k = (i + j) / 2;
		while (!(indexes[k - 1] < to && indexes[k] >= to)) {
			System.out.printf("xend  : check k=%d for vl=%d\n", k, to);
			if (indexes[k] < to) {
				i = k;
			} else {
				j = k;
			}
			k = (i + j) / 2;
		}
		System.out.printf("xend  : k=%d for vl=%d\n", k, to);
		return k;
	}

	public int getSpan() {
		return rangeLength;
	}

	public int getRangeStart() {
		return rangeStart;
	}
	public int getRangeLength() {
		return rangeLength;
	}
}

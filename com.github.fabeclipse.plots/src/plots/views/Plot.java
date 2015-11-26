package plots.views;

import java.security.InvalidParameterException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class Plot implements IPlottable {
	// the data to plot
	private int[] domain;
	private int[] values;

	// the range in the data domain
	private PlotRange range;
	// range in the data co-domain
	private int min;
	private int max;

	// modes
	boolean baseZero = false;

	public Plot(int[] vals) {
		setData(vals);
	}

	public void setData(int[] indexes, int[] values) {
		if (indexes == null || values == null || indexes.length != values.length)
			throw new IllegalArgumentException("indexes and values must be non null and the same size");
		this.domain = indexes;
		this.values = values;
		// use the complete domain as range
		setDomainRange(indexes[0], indexes[indexes.length - 1]);
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		for (int i = 0; i < indexes.length; i++) {
			int v = values[i];
			if (v > max) max = v;
			if (v < min) min = v;
		}
//		System.out.printf("Plot[%s].setData(%s, size=%d)\n", this, values, values.length );
	}

	public void setData(int[] values) {
		int[] indexes = new int[values.length];
		for (int i = 0; i < indexes.length; i++)
			indexes[i] = i;
		setData(indexes, values);
	}

	/* (non-Javadoc)
	 * @see plots.views.IPlottable#plot(org.eclipse.swt.graphics.GC, org.eclipse.swt.graphics.Rectangle)
	 */
	@Override
	public void plot(GC gc, Rectangle dst) {
		if (values.length == 0)
			return;
		
		int base = baseZero ? 0 : min;
		int lx = 0;
		int ly = 0;
		int li = 0;
		int origx = dst.x;

		// preconditions
		try {
			check(range.getSpan() > 0);
			check(values.length > 1);
			check(domain.length > 0);
			check(domain[domain.length -1] - domain[0] > 0);
		} catch (Exception e) {
			// these exceptions lead to no graph being drawn
			return;
		}

		dst = range.clippedDisplayRect(dst);
		
		// adjust origin x
		// the client has set a domain range that should be mapped to the dst rectangle
		// if the range is smaller than the domain, then the plot will start  with an offset
		// from dst.x origin
		dst.x += (range.getRangeStart() - domain[0]) * dst.width / (domain[domain.length - 1] - domain[0]);
		dst.width = range.getRangeLength() * dst.width / (domain[domain.length - 1] - domain[0]);

		int startIndex = range.getDomainStartIndex(domain);
		int endIndex = range.getDomainEndIndex(domain);
		final double scalex = (double) (endIndex - startIndex) / dst.width;
		final double scaley = (double) dst.height / (max - base);

		// only paint what is visible
		dst = dst.intersection(gc.getClipping());
		if (dst.isEmpty())
			return;

		lx = 0;
		ly = dst.y + dst.height - (int)((values[(int)((lx + (dst.x - origx)) * scalex)] - base) * scaley);
		for (int x = 1; x < dst.width; x++) {
			final int i = (int) ((x + (dst.x - origx)) * scalex);
			int val = (int) ((values[i] - base) * scaley);
			if (i - li > 1) {
				int min = val, max = val;
				// TODO: now average on interval, better to show both min/max?
				for (int j = li + 1; j < i ; j++) {
					int v = (int) ((values[j] - base) * scaley);
					if (min > v)
						min = v;
					if (max < v)
						max = v;
				}
				final int miny = dst.y + dst.height - min;
				final int maxy = dst.y + dst.height - max;
				gc.drawLine(x + dst.x, miny, x + dst.x, maxy);
				final int y = dst.y + dst.height - val;
				gc.drawLine(lx + dst.x, ly, x + dst.x, y);
				lx = x;
				ly = y;
				li = i;
			} else {
				final int y = dst.y + dst.height - val;
				gc.drawLine(lx + dst.x, ly, x + dst.x, y);
				lx = x;
				ly = y;
				li = i;
			}
		}
	}

	private void check(boolean b) {
		if (!b)
			throw new InvalidParameterException("check failed");
	}

	@Override
	public Plot setDomainRange(int[] range) {
		if (range == null || range.length != 2)
			throw new IllegalArgumentException("Range array must have two elements");
		setDomainRange(range[0], range[1]);
		return this;
	}

	@Override
	public Plot setDomainRange(int start, int end) {
		int dataStart = domain[0];
		int dataLength = domain[domain.length - 1] - dataStart;
		range = new PlotRange(dataStart, dataLength).setRange(start, end);
		return this;
	}

	@Override
	public int getCount() {
		return values == null ? 0 : values.length;
	}
}

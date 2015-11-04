package plots.views;

import java.security.InvalidParameterException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class Plot implements IPlottable {
	// the data to plot
	private int[] indexes;
	private int[] values;

	// the range in the data domain
	private int start;
	private int end;
	// range in the data co-domain
	private int min;
	private int max;

	// modes
	boolean baseZero = false;
	private int startIndex;
	private int endIndex;

	public Plot(int[] vals) {
		setData(vals);
	}

	public void setData(int[] indexes, int[] values) {
		if (indexes == null || values == null || indexes.length != values.length)
			throw new IllegalArgumentException("indexes and values must be non null and the same size");
		this.indexes = indexes;
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
			check(end - start > 0);
			check(values.length > 1);
			check(indexes.length > 0);
			check(indexes[indexes.length -1] - indexes[0] > 0);
		} catch (Exception e) {
			// these exceptions lead to no graph being drawn
			return;
		}

		dst = new Rectangle(dst.x, dst.y, dst.width, dst.height);
		// adjust origin x
		// the client has set a domain range that should be mapped to the dst rectangle
		// if the range is smaller than the domain, then the plot will start  with an offset
		// from dst.x origin
		dst.x += (start - indexes[0]) * dst.width / (indexes[indexes.length - 1] - indexes[0]);
		dst.width = (end - start) * dst.width / (indexes[indexes.length - 1] - indexes[0]);

		final double scalex = (double) (endIndex - startIndex) / dst.width;
		final double scaley = (double) dst.height / (max - base);

		// only paint what is visible
		dst = dst.intersection(gc.getClipping());
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
	public int[] getDomainExtension() {
		return new int[] {start, end};
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
		this.start = start;
		this.end = end;
		startIndex = getDomainStartIndex(start);
		endIndex = getDomainEndIndex(end);
		return this;
	}

	private int getDomainStartIndex(int from) {
		// check if the requested position is outside
		// the domain
		if (from <= indexes[0])
			return indexes[0];
		if (from >= indexes[indexes.length - 1])
			return indexes[indexes.length - 1];

		// TODO: can I use the stdlib binary search here?
		int i,j, k;
		i = 0;
		j = indexes.length;
		k = (i + j) / 2;
		while (!(indexes[k] <= from && indexes[k + 1] > from)) {
			System.out.printf("xstart: check k=%d for vl=%d\n", k, from);
			if (indexes[k] < from) {
				i = (k + j) / 2;
			} else {
				j = (k + i) / 2;
			}
			k = (i + j) / 2;
		}
		System.out.printf("xstart: k=%d for vl=%d\n", k, from);
		return k;
	}
	private int getDomainEndIndex(int to) {
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
				i = (k + j) / 2;
			} else {
				j = (k + i) / 2;
			}
			k = (i + j) / 2;
		}
		System.out.printf("xend  : k=%d for vl=%d\n", k, to);
		return k;
	}
	@Override
	public int getCount() {
		return values == null ? 0 : values.length;
	}
}

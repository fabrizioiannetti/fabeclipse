package plots.views;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class Plot implements IPlottable {
	// the data to plot
	private int[] indexes;
	private int[] values;

	// the range in the data
	private int start;
	private int end;
	private int min;
	private int max;

	// modes
	boolean baseZero = false;

	public Plot() {
		int[] vals = new int[1000];
		int v = 37;
		for (int i = 0; i < vals.length; i++) {
			vals[i] = v;
			v = (int) (v + (Math.random() - 0.5) * 10) % 100;
		}
		setData(vals);
	}

	public Plot(int[] vals) {
		setData(vals);
	}

	public void setData(int[] indexes, int[] values) {
		if (indexes == null || values == null || indexes.length != values.length)
			throw new IllegalArgumentException("indexes and values must be non null and the same size");
		this.indexes = indexes;
		this.values = values;
		start = 0;
		end = indexes.length;
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		for (int i = 0; i < indexes.length; i++) {
			int v = values[i];
			if (v > max) max = v;
			if (v < min) min = v;
		}
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
		final double scalex = (double) (end - start) / dst.width;
		final double scaley = (double) dst.height / (max - base);
		int lx = 0;
		int ly = 0;
		int li = 0;
		int origx = dst.x;

		// only paint what is visible
		dst = dst.intersection(gc.getClipping());
		lx = 0;
		ly = dst.y + dst.height - (int)((values[(int)((lx + (dst.x - origx)) * scalex)] - base) * scaley);
		for (int x = 1; x < dst.width; x++) {
			int val = 0;
			final int i = (int) ((x + (dst.x - origx)) * scalex);
			val = (int) ((values[i] - base) * scaley);
			if (i - li > 1) {
				// TODO: now average on interval, better to show both min/max?
				for (int j = li + 1; j < i ; j++)
					val += (int) ((values[i] - base) * scaley);
				val = val / (i - li);
			}
			final int y = dst.y + dst.height - val;
			gc.drawLine(lx + dst.x, ly, x + dst.x, y);
			lx = x;
			ly = y;
			li = i;
		}
	}
}

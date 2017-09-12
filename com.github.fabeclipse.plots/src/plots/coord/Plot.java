package plots.coord;

import java.security.InvalidParameterException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class Plot implements IPlottable {
	private PlotData data;
	private double rangeStart;
	private double rangeEnd;
	
	boolean baseZero;

	public Plot(double[] ds) {
		setData(ds);
	}

	public void setData(double[] indexes, double[] values) {
		if (indexes == null || values == null || indexes.length != values.length)
			throw new IllegalArgumentException("indexes and values must be non null and the same size");
		data = new PlotData(indexes, values);
		// use the complete domain as range
		rangeStart = indexes[0];
		rangeEnd   = indexes[indexes.length - 1];
//		System.out.printf("Plot[%s].setData(%s, size=%d)\n", this, values, values.length );
	}

	public void setData(double[] v) {
		double[] d = new double[v.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = i;
		}
		setData(d, v);
	}
	@Override
	public void plot(GC gc, Rectangle dst) {
		if (getCount() == 0)
			return;
	
		double minVal = data.getMin();
		double maxVal = data.getMax();
		double yScale = dst.height / (maxVal - minVal);
		int lx = 0;
		int ly = 0;

		System.out.printf("Plot.plot(width=%d, height=%d), range=[%f,%f], minmax=[%f,%f]\n",
				dst.width, dst.height, rangeStart, rangeEnd, minVal, maxVal);

		// preconditions
		try {
			check(rangeStart < rangeEnd);
			check(data.value.length > 1);
			check(data.domain.length > 0);
			check(data.domain[data.domain.length -1] - data.domain[0] > 0);
		} catch (Exception e) {
			// these exceptions lead to no graph being drawn
			return;
		}

		double domainStep = (rangeEnd - rangeStart) / dst.width;
		double ds = rangeStart;
		double de = 0;

		int yBase = dst.y + dst.height;

		// only paint what is visible
		dst = dst.intersection(gc.getClipping());
		if (dst.isEmpty())
			return;

		for (int x = 0; x < dst.width; x++) {
			de = ds + domainStep;
			double v = data.valueForRange(ds, de);
			int y = yBase - (int) ((v - minVal) * yScale);
			gc.drawLine(dst.x + lx, ly, dst.x + x, y);
			lx = x;
			ly = y;
			ds = de;
		}
	}

	@Override
	public int getCount() {
		return data.domain.length;
	}

	@Override
	public Plot setDomainRange(double start, double end) {
		rangeStart = start;
		rangeEnd = end;
		return this;
	}

	private void check(boolean b) {
		if (!b)
			throw new InvalidParameterException("check failed");
	}

	@Override
	public double getDomainStart() {
		return data.domain[0];
	}
	
	@Override
	public double getDomainEnd() {
		return data.domain[data.domain.length - 1];
	}
}

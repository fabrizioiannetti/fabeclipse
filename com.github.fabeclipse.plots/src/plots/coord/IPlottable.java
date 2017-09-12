package plots.coord;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public interface IPlottable {

	/**
	 * Plots data in the given rectangle (in GC coordinates).
	 * 
	 * The destination rect can also be bigger than the gc
	 * bunding box, e.g. when drawing only a portion of the plot.
	 * 
	 * @param gc the context to draw on
	 * @param dst bounding box for the plot
	 */
	void plot(GC gc, Rectangle dst);

	int getCount();

	double getDomainStart();

	double getDomainEnd();

	IPlottable setDomainRange(double start, double end);
}
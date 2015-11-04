package plots.views;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

public class PlotView extends ViewPart {

	private static final String PLOT_VIEW_KEY = "plotView";

	private static final int SASH_HEIGHT = 6;

	private Action actionImportData;

	private Composite parent;
	private List<PlotViewer> viewers = new ArrayList<>();

	// the global domain range, i.e. the portion of data
	// to display
	private int[] xrange = new int[] {0, 0};
	private float xscale = 1;

	private Action actionZoomIn;
	
	/**
	 * The constructor.
	 */
	public PlotView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		this.parent = parent;
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(parent);
		parent.setData(PLOT_VIEW_KEY, this);

		// TODO: this is some initial data just for testing
		addPlotViewer(parent, demoData());
		addPlotViewer(parent, demoData());

		parent.addMouseWheelListener(PlotView::mouseWheelMoved);
		// view actions
		makeActions();
		contributeToActionBars();
	}

	// just for testing purposes
	private int[] demoData() {
		int[] vals;
		vals = new int[1000];
		int v = 37;
		for (int i = 0; i < vals.length; i++) {
			vals[i] = v;
			v = (int) (v + (Math.random() - 0.5) * 10) % 100;
		}
		return vals;
	}

	private void addPlotViewer(Composite parent, int[] data) {
		Sash sash = null;
		PlotViewer plotViewer = new PlotViewer(parent);
		plotViewer.addPlot(data);

		sash = new Sash(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SASH_HEIGHT).applyTo(sash);
		sash.addListener(SWT.Selection, PlotView::onSashMoved);
		sash.setData(plotViewer);

		plotViewer.colors.add(new Color(Display.getDefault(), 0, 0, 0));
		plotViewer.colors.add(new Color(Display.getDefault(), 255, 0, 0));
		plotViewer.sash = sash;
		viewers.add(plotViewer);
		plotViewer.canvas.addDisposeListener(e -> {
			boolean removed = viewers.remove(e.widget.getData());
			System.out.println("removed viewer :" + removed);
		});
		plotViewer.onRectChange(r -> {setAllRects(r); });
		updateXRange(data);
		parent.redraw();
		parent.layout();
	}

	private void updateXRange(int[] data) {
		// TODO: this should actually look in the domain array...
		if (xrange[1] < data.length) {
			xrange[1] = data.length;
		}
		updateDomainRangeAll();
	}

	private void updateDomainRangeAll() {
		// notify all viewers about the new range
		for (PlotViewer plotViewer : viewers) {
			plotViewer.setDomainRange((int)(xrange[0] * xscale), (int)(xrange[1] * xscale));
		}
	}

	private void setAllRects(Rectangle r) {
		for (PlotViewer plotViewer : viewers)
			plotViewer.setPlotRect(r);
	}

	private static void onSashMoved(Event e) {
		Sash s = (Sash) e.widget;
		PlotViewer pv = (PlotViewer) s.getData();
		Rectangle sashBounds = e.getBounds();
		System.out.println("Sash moved to:" + sashBounds);
		Rectangle bounds = pv.canvas.getBounds();
		int newh = sashBounds.y - bounds.y;
		if (newh > 5) {
			GridData gd = (GridData) pv.canvas.getLayoutData();
			gd.heightHint = newh;
			pv.canvas.getParent().layout();
			pv.canvas.getParent().redraw();
		}
	}

	private static void mouseWheelMoved(MouseEvent e) {
		// check if CTRL/CMD is pressed
		if ((e.stateMask & SWT.MOD1) == 0)
			return;
		Composite parent = (Composite) e.widget;
		PlotView plotView = (PlotView) parent.getData(PLOT_VIEW_KEY);
		if (plotView != null) {
			if (e.count > 0)
				plotView.xscale *= 1.2;  // UP -> zoom in
			else
				plotView.xscale /= 1.2; // DOWN -> zoom out
			System.out.printf("Update domain to plots: [%d,%d]x%f\n",
					plotView.xrange[0],
					plotView.xrange[1], plotView.xscale);
			plotView.updateDomainRangeAll();
			plotView.parent.redraw();
		}
		// find the plot view under the cursor
//		PlotViewer pv = null;
//		int x = 0;
//		for (Control c : parent.getChildren()) {
//			Rectangle bounds = c.getBounds();
//			if (bounds.contains(e.x, e.y)) {
//				if (c instanceof Canvas) {
//					Canvas canvas = (Canvas) c;
//					pv = (PlotViewer) canvas.getData();
//					x = e.x - bounds.x;
//				}
//				break;
//			}
//		}
//		if (pv == null)
//			return;
//	
//		if (e.count > 0)
//			pv.zoomIn(x);  // UP -> zoom in
//		else
//			pv.zoomOut(x); // DOWN -> zoom out
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		bars.getMenuManager().add(actionImportData);
		bars.getToolBarManager().add(actionImportData);
		bars.getToolBarManager().add(actionZoomIn);
	}

	private void makeActions() {
		actionImportData = new LambdaAction("+", "Add plot",
				e -> {
					ImportDataFromTextDialog dialog = new ImportDataFromTextDialog(getSite().getShell());
					dialog.listOpenEditors(getSite().getWorkbenchWindow());
					if (dialog.open() == Dialog.OK) {
						int[] data = dialog.getData();
						// TODO: option to import into an existing plot
						addPlotViewer(parent, data);
						parent.redraw();
					}
			});

		actionZoomIn = new LambdaAction("><", "Zoom In",  
			e -> {
				xscale *= 1.2;  // UP -> zoom in
				System.out.printf("Update domain to plots: [%d,%d]x%f\n",
						xrange[0],
						xrange[1], xscale);
				updateDomainRangeAll();
				parent.redraw();
				Control[] children = parent.getChildren();
				for (Control c : children) {
					c.redraw();
				}
			});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		parent.setFocus();
//		getCanvas().setFocus();
	}
}

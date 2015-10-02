package plots.views;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class PlotView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "plots.views.PlotView";

	private Action actionImportData;

	private static class PlotViewer {
		// UI
		public Canvas canvas;
		private Action actionFit;
		private Action actionZoomIn;
		private Action actionZoomOut;

		// model
		public List<IPlottable> plots = new ArrayList<>();
		public List<Color> colors = new ArrayList<>();
		public Rectangle plotRect = new Rectangle(10, 10, 600, 200);

		public PlotViewer() {
			makeActions();
		}

		public void addPlot(int[] data) {
			plots.add(new Plot(data));
		}

		public void addPlot() {
			plots.add(new Plot());
		}

		private void fillContextMenu(IMenuManager manager) {
			manager.add(actionFit);
			manager.add(actionZoomIn);
			manager.add(actionZoomOut);
			// Other plug-ins can contribute there actions here
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}

		private void makeActions() {
			actionFit = new Action() {
				public void run() {
					Point size = canvas.getSize();
					// TODO: extend for multiple canvases
					plotRect.x = 0;
					plotRect.y = 0;
					plotRect.width = size.x;
					plotRect.height = size.y;
					canvas.redraw();
				}
			};
			actionFit.setText("Fit Plot");
			actionFit.setToolTipText("Fit the plot to the current size");
			actionFit.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
			
			actionZoomIn = new Action() {
				public void run() {
					// UP -> zoom in
					// TODO: extend for multiple canvases
					plotRect.width = (int) (plotRect.width * 1.2f);
					canvas.redraw();
				}
			};
			actionZoomIn.setText("><");
			actionZoomIn.setToolTipText("Zoom in");

			actionZoomOut = new Action() {
				public void run() {
					// TODO: extend for multiple canvases
					if (plotRect.width < 10)
						return; // do not zoom too much
					// zoom out
					plotRect.width = (int) (plotRect.width / 1.2f);
					canvas.redraw();
				}
			};
			actionZoomOut.setText("<>");
			actionZoomOut.setToolTipText("Zoom out");
		}

	}

	private List<PlotViewer> viewers = new ArrayList<>();

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	private final class PlotMouseMoveListener implements MouseMoveListener {
		private int startx = 0;
		private boolean dragging = false;

		@Override
		public void mouseMove(MouseEvent e) {
			if (e.stateMask == SWT.BUTTON1) {
				// drag
				System.out.printf("Drag:%d,%d\n", e.x, e.y);
				if (!dragging) {
					startx = e.x;
					dragging = true;
				} else {
					int diffx = e.x - startx;
					if (diffx != 0) {
						PlotViewer pv = (PlotViewer) e.widget.getData();
						pv.plotRect.x += diffx;
						((Canvas) e.widget).redraw();
					}
					startx = e.x;
				}
			} else
				dragging = false;
		}
	}
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return new String[] { "One", "Two", "Three" };
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}
	class NameSorter extends ViewerSorter {
	}

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
		GridLayoutFactory.fillDefaults().applyTo(parent);

		// TODO: extend for multiple canvases
		addPlotViewer(parent);

		// Create the help context id for the viewer's control
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(canvas, "plots.viewer");
		makeActions();
		contributeToActionBars();
	}

	private void addPlotViewer(Composite parent) {
		PlotViewer plotViewer = new PlotViewer();
		plotViewer.addPlot();
		plotViewer.colors.add(new Color(Display.getDefault(), 0, 0, 0));
		plotViewer.colors.add(new Color(Display.getDefault(), 255, 0, 0));
		newCanvas(parent, plotViewer);
		hookContextMenu(plotViewer);
		viewers.add(plotViewer);
	}

	private void newCanvas(Composite parent, PlotViewer plotParams) {
		Canvas canvas = new Canvas(parent, SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).applyTo(canvas);
		canvas.addPaintListener(PlotView::paintPlot);
		canvas.addMouseWheelListener(PlotView::mouseWheelMoved);
		canvas.addMouseMoveListener(new PlotMouseMoveListener());
		canvas.setData(plotParams);
		plotParams.canvas = canvas;
	}

	/**
	 * Get the current canvas.
	 * 
	 * @return the canvas
	 */
	private Control getCanvas() {
		// TODO: extend for multiple canvases
		return viewers.get(0).canvas;
	}

	private static void mouseWheelMoved(MouseEvent e) {
		PlotViewer pv = (PlotViewer) e.widget.getData();
		// check if CTRL/CMD is pressed
		if ((e.stateMask & SWT.MOD1) == 0)
			return;
		if (e.count > 0) {
			// UP -> zoom in
			pv.actionZoomIn.run();
		} else {
			// DOWN -> zoom out
			pv.actionZoomOut.run();
		}
	}

	private static void paintPlot(PaintEvent e) {
		PlotViewer pp = (PlotViewer) e.widget.getData();
		Color c = e.gc.getForeground();
		for (int i = 0; i < pp.plots.size(); i++) {
			if (i < pp.colors.size())
				e.gc.setForeground(pp.colors.get(i));
			else
				e.gc.setForeground(c);
			pp.plots.get(i).plot(e.gc, pp.plotRect);
		}
	}

	private void hookContextMenu(PlotViewer plotViewer) {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				plotViewer.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(plotViewer.canvas);
		plotViewer.canvas.setMenu(menu);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(actionImportData);
//		manager.add(new Separator());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionImportData);
	}

	private void makeActions() {
		actionImportData = new Action() {
			public void run() {
				ImportDataFromTextDialog dialog = new ImportDataFromTextDialog(getSite().getShell());
				dialog.listOpenEditors(getSite().getWorkbenchWindow());
				if (dialog.open() == Dialog.OK) {
					int[] data = dialog.getData();
					// TODO: extend for multiple canvases
					viewers.get(0).addPlot(data);
				}
			}
		};
		actionImportData.setText("+");
		actionImportData.setToolTipText("Add plot");
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		getCanvas().setFocus();
	}
}

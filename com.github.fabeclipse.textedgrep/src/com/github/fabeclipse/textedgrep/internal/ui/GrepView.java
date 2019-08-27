/**
 * Copyright 2015 Fabrizio Iannetti.
 */
package com.github.fabeclipse.textedgrep.internal.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.CursorLinePainter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.fabeclipse.textedgrep.Activator;
import com.github.fabeclipse.textedgrep.GrepMonitor;
import com.github.fabeclipse.textedgrep.GrepTool;
import com.github.fabeclipse.textedgrep.IGrepContext;
import com.github.fabeclipse.textedgrep.IGrepTarget;

/**
 * View to show the result of a grep operation on the
 * content of an editor.
 *
 * @author fabrizio iannetti
 */
public class GrepView extends ViewPart implements IAdaptable {
	private static final String LINE_NUMBER_RULER_COLOR = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR;

	public static final String VIEW_ID = "com.github.fabeclipse.textedgrep.grepview";

	private static final String KEY_GREPREGEX = "grepregex";
	private static final String KEY_CASESENSITIVE = "casesensitive";
	private static final String KEY_HIGHLIGHTMULTIPLE = "highlightmultiple";
	private static final String KEY_REGEX_HISTORY = "regexhistory";
	private static final String KEY_DEFAULT_COLOR = "regexcolour";

	class GrepOp {
		private static final int GREP_SHOW_PROGRESS_THRESHOLD_PERCENT = 50;
		private static final int GREP_SHOW_PROGRESS_THRESHOLD_MS = 300;
		private final boolean multiple;
		private final GrepTool tool;
		private final IGrepContext context;
		private final GrepMonitor monitor;
		
		public GrepOp(IGrepTarget target, String[] rxList, boolean caseSensitive, boolean multiple) {
			super();
			monitor = new GrepMonitor();
			this.multiple = multiple;
			tool = new GrepTool(rxList, caseSensitive);
			context = tool.grepStart(target);
		}

		public IGrepContext getContext() {
			return context;
		}

		public boolean grep() {
			final long tic = System.currentTimeMillis();
			monitor.onProgress(new IntConsumer() {
				private boolean showProgress;
				@Override
				public void accept(final int p) {
					if (!showProgress) {
						long elapsed = System.currentTimeMillis() - tic;
						if (elapsed > GREP_SHOW_PROGRESS_THRESHOLD_MS &&
								p < GREP_SHOW_PROGRESS_THRESHOLD_PERCENT) {
							// seems to take long, enable a progress bar for the user
							showProgress = true;
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									progress.onCancel(monitor);
									showProgressBar(p);
								}
							});
						}
					} else {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								showProgressBar(p);
							}
						});
					}
				}
			});
			tool.grep(context, monitor, multiple);
			return true;
		}

		void cancel() {
			monitor.cancel();
		}
	}

	private ArrayBlockingQueue<GrepOp> grepQueue = new ArrayBlockingQueue<GrepView.GrepOp>(10);
	private AtomicReference<GrepOp> grepCurr = new AtomicReference<GrepView.GrepOp>();

	private IGrepContext submitGrep(IGrepTarget target, String[] regex, boolean caseSensitive, boolean multi) {
		GrepOp op = new GrepOp(target, regex, caseSensitive, multi);

		// cancel all queued greps (actually one at most)
		grepQueue.clear();
		
		// queue the new grep
		boolean queued = grepQueue.offer(op);

		if (!queued) {
			// TODO: log
		} else {
			// cancel running grep, if any
			// (but not the one we just queued)
			GrepOp cop = grepCurr.get();
			if (cop != null && cop != op)
				cop.cancel();
		}		
		return op.getContext();
	}

	private final Thread grepThread = new Thread() {
		public void run() {
			ArrayList<GrepOp> ops = new ArrayList<GrepOp>();
			for (;;) {
				GrepOp op;
				try {
					op = grepQueue.take();
				} catch (InterruptedException e1) {
					// TODO log
					System.out.println("grepThread interrupted, wait again");
					continue;
				}
				grepQueue.drainTo(ops);
				// only use the latest
				if (!ops.isEmpty()) {
					op = ops.get(ops.size() - 1);
					ops.clear();
				}
				grepCurr.set(op);
				final IGrepContext ctxt = op.getContext();
				try {
					if (op.grep())
						GrepView.this.getSite().getShell().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								String text = ctxt.getText();
								viewer.getDocument().set(text);
								updateHighlightRanges();
							}
						});
				} catch (Exception e) {
					// grep was interrupted, just go on
				} finally {
					grepCurr.set(null);
					GrepView.this.getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							viewer.getControl().setEnabled(true);
							viewer.getControl().setFocus();
						}
					});
				}
			}
		};
	};

	/**
	 * @since 1.2
	 */
	protected static final int REGEX_HISTORY_MAX_SIZE = 20;

	private Color failedSearchColor = new Color(Display.getDefault(), 255, 128, 128);
	private Color successfulSearchColor;

	private TextViewer viewer;
	private String lastRegex;
	private IGrepContext grepContext;
	private Color cursorLineColor;
	private Color highlightColor;
	private boolean initialCaseSensitivity;

	// index of last successful search
	private int findIndex;
	// position where to start the first find
	private int firstFindIndex;

	/**
	 * This object editor activation on the workbench page
	 * where the view is.
	 */
	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {}
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {}
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {}
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {}
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {}
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {}
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (linkToEditorAction.isChecked() && part instanceof EditorPart && !target.isSame(part)) {
				// ok, it's a non null editor, and it is not the current one
				// grep it
				doGrep();
			}
		}
	};

	private Action linkToEditorAction;
	private Action csAction;
	private Action hmAction;

	private boolean initialHighlightMultiple;

	private Composite findbar;
	private List<String> regexHistory = new ArrayList<String>();

	// list of regex text entries
	private List<RegexEntry> regexEntries = new ArrayList<RegexEntry>();

	private IGrepTarget target;

	private IEditorPart targetPart;

	private ProgressWithCancel progress;

	private Color lnColor;

	private LineNumberRulerColumn lineNumberColumn;

	@Override
	public void createPartControl(final Composite parent) {
		grepThread.start();
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		addRegexField(parent);

		// progress indicator visible only during long running operations
		addProgressBar(parent);

		// vertical ruler that shows the original's line number
		CompositeRuler ruler = new CompositeRuler();
		lineNumberColumn = new LineNumberRulerColumn() {
			@Override
			protected int computeNumberOfDigits() {
				// see SourceViewer, monkey see monkey do :)
				if (grepContext != null) {
					int digits= 2;
					double lines = grepContext.getMaxOriginalLine() + 1;
					while (lines  > Math.pow(10, digits) -1) {
						++digits;
					}
					return digits;
				}
				return super.computeNumberOfDigits();
			}
			@Override
			protected String createDisplayString(int line) {
				if (grepContext != null) {
					return Integer.toString(grepContext.getOriginalLine(line) + 1);
				}
				return super.createDisplayString(line);
			}
		};
		initializeColors();
		lineNumberColumn.setForeground(lnColor);
		ruler.addDecorator(0, lineNumberColumn);
		viewer = new SourceViewer(parent, ruler , SWT.FLAT | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// use the default text font (the same used in text editor)
		viewer.getTextWidget().setFont(JFaceResources.getTextFont());
		// this is a read-only view!
		viewer.setEditable(false);
		// highlight the cursor line
		CursorLinePainter cursorLinePainter = new CursorLinePainter(viewer);
		cursorLineColor = new Color(parent.getDisplay(), new RGB(200, 200, 0));
		cursorLinePainter.setHighlightColor(cursorLineColor);
		viewer.addPainter(cursorLinePainter);

		viewer.getTextWidget().addLineBackgroundListener(new LineBackgroundListener() {
			@Override
			public void lineGetBackground(LineBackgroundEvent event) {
				IGrepContext gc = grepContext;
				IDocument document = viewer.getDocument();
				if (gc != null) {
					int line;
					try {
						line = document.getLineOfOffset(event.lineOffset);
						// grep context can only operate on non empty documents
						if (document.getLength() > 0) {
							int index = gc.getColorForGrepLine(line);
							RegexEntry entry = regexEntries.get(index);
							event.lineBackground = entry.getRegexColor();
						}
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		viewer.setDocument(new Document("grep result"));

		// track cursor line and synchronise the cursor position in the editor
		viewer.getTextWidget().addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				int caretOffset = event.caretOffset;
				if (grepContext != null) {
					try {
						int grepLine = viewer.getDocument().getLineOfOffset(caretOffset);
						int line = grepContext.getOriginalLine(grepLine);
						int offset = grepContext.getTarget().getLineOffset(line);
						target.select(offset, 0);
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		// register double click and enter key to activate the current target
		viewer.getTextWidget().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (targetPart != null) {
					IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
					window.getActivePage().activate(targetPart);
				}
			}
		});
		viewer.getTextWidget().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR && targetPart != null) {
					IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
					window.getActivePage().activate(targetPart);
				}
			}
		});
		
		// selection provider
		getSite().setSelectionProvider(viewer);

		MenuManager mm = new MenuManager();
		mm.setRemoveAllWhenShown(true);
		mm.addMenuListener(new IMenuListener() {
			
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new GroupMarker("GrepViewMenu"));
			}
		});
		Menu contextMenu = mm.createContextMenu(viewer.getControl());
		viewer.getTextWidget().setMenu(contextMenu);
		getSite().registerContextMenu(mm, viewer);

		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		menuManager.add(new Action("Grep") {
			@Override
			public void run() {
				doGrep();
				viewer.getControl().setFocus();
			}
		});
		menuManager.add(new Separator());

		csAction = new Action("Case Sensitive", Action.AS_CHECK_BOX) {};
		csAction.setChecked(initialCaseSensitivity);
		menuManager.add(csAction);
		hmAction = new Action("Highlight Multiple", Action.AS_CHECK_BOX) {};
		hmAction.setChecked(initialHighlightMultiple);
		menuManager.add(hmAction);
		menuManager.add(new Separator());

		Action colorAction = new Action("Highlight color...") {
			@Override
			public void run() {
				Shell shell = getSite().getShell();
				ColorDialog colorDialog = new ColorDialog(shell);
				colorDialog.setText("Highlight color");
				colorDialog.setRGB(highlightColor.getRGB());
				RGB rgb = colorDialog.open();
				if (rgb != null) {
					Color oldColor = highlightColor;
					highlightColor = new Color(shell.getDisplay(), rgb);
					// TODO: move this in a long-running job, possible?
					updateHighlightRanges();
					oldColor.dispose();
				}
			}
		};
		menuManager.add(colorAction);

		menuManager.add(new Action("Edit History...") {
			@Override
			public void run() {
				String history = "";
				for (String helem : GrepView.this.regexHistory) {
					history += helem + "\n";
				}
				InputDialog inputDialog = new InputDialog(getViewSite().getShell(), "Regex history", null, history, null) {
					@Override
					protected int getInputTextStyle() {
						return SWT.MULTI | SWT.BORDER;
					}
					@Override
					protected boolean isResizable() {
						return true;
					}
					@Override
					protected Control createDialogArea(Composite parent) {
						// TODO Auto-generated method stub
						Control area = super.createDialogArea(parent);
				        getText().setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL
				                | GridData.HORIZONTAL_ALIGN_FILL));
						return area;
					}
				};
				inputDialog.open();
				history = inputDialog.getValue();
				if (inputDialog.getReturnCode() == InputDialog.OK) {
					regexHistory.clear();
					if (history == null)
						history = "";
					String[] harray = history.split("\n");
					for (String helem : harray) {
						if (!helem.isEmpty())
							regexHistory.add(helem);
					}
					for (RegexEntry rxe : regexEntries)
						setRegexHistoryInComboBox(rxe);
				}
			}
		});

		fillActionBar();

// this code would use the standard find dialog, maybe offer it as an option?
//		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), new FindReplaceAction(new ResourceBundle() {
//			@Override
//			protected Object handleGetObject(String key) { return null; }
//			@Override
//			public Enumeration<String> getKeys() { return null; }
//		}, "GrepView.FindReplace", this));

		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), new Action() {
			@Override
			public void run() {
				createFindbar(parent);
			}
		});
		IPartService partService = (IPartService) getViewSite().getService(IPartService.class);
		partService.addPartListener(partListener);

		// make tab key toggle between the regular expression text(s) and the viewer
		makeTabList();
	}

	private void initializeColors() {
		// get the line number ruler color from the editor plugin,
		// to be consistent with the editor settings.
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		final Display display = getViewSite().getShell().getDisplay();
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (LINE_NUMBER_RULER_COLOR.equals(event.getProperty())) {
					Object value = event.getNewValue();
					// value for this property should always be a string
					if (!(value instanceof String))
						return;
			        final RGB newLnColor = StringConverter.asRGB((String) value);
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							updateLineNumberColor(newLnColor);
						}
					});
				}
			}
		});
		String key = LINE_NUMBER_RULER_COLOR;
		RGB rgb= null;
		if (store.contains(key)) {
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
		} else {
			rgb = new RGB(0, 0, 0); // TODO: use text color
		}
		updateLineNumberColor(rgb);
	}

	private void updateLineNumberColor(RGB rgb) {
		lnColor = EditorsUI.getSharedTextColors().getColor(rgb);
		if (lineNumberColumn != null) {
			lineNumberColumn.setForeground(lnColor);
			lineNumberColumn.redraw();
		}
	}

	private void addProgressBar(Composite parent) {
		progress = new ProgressWithCancel(parent, SWT.NONE);
		progress.setVisible(false);
		GridDataFactory.fillDefaults().grab(true, false).exclude(true).applyTo(progress);
	}

	private void showProgressBar(int percent) {
		final GridData gd = (GridData) progress.getLayoutData();
		if (percent == 100) {
			gd.exclude = true;
			progress.setVisible(false);
			progress.getParent().layout();
		} else {
			if (gd.exclude) {
				progress.setProgress(percent, true);
				gd.exclude = false;
				progress.setVisible(true);
				progress.getParent().layout();
			} else {
				progress.setProgress(percent, false);
			}
		}
	}

	private void addRegexField(final Composite parent) {
		// when pressing ENTER in the regexp field do a grep
		IRegexEntryListener listener = new IRegexEntryListener() {
			@Override
			public void grep(String text, RegexEntry rxe) {
				// the user pressed ENTER
				doGrep();
				// add regex to history if:
				// * not empty
				// * history is empty, or last element of history is not the same
				if (!text.isEmpty() && (regexHistory.isEmpty() || !regexHistory.get(regexHistory.size() - 1).equals(text))) {
					while (regexHistory.size() >= REGEX_HISTORY_MAX_SIZE)
						regexHistory.remove(0);
					regexHistory.add(text);
					setRegexHistoryInComboBox(rxe);
				}
			}
		};

		RegexEntry rxe = new RegexEntry(parent, listener, -1);
		rxe.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		rxe.setRegexpText(lastRegex);
		setRegexHistoryInComboBox(rxe);
		regexEntries.add(rxe);
		// make sure the widget is just above the viewer
		if (viewer != null && !viewer.getControl().isDisposed()) {
			rxe.moveAbove(viewer.getControl());
		}
	}

	private void fillActionBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		linkToEditorAction = new Action("Link To Editor",Action.AS_CHECK_BOX) {};
		ImageDescriptor image = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/synced.gif");
		linkToEditorAction.setImageDescriptor(image);
		linkToEditorAction.setToolTipText("Sync Grep Content to active editor\nAs soon as an editor is activated its content is filtered");
		toolBarManager.add(linkToEditorAction);

		// TODO: leave commented for now, activate when ready
		// (remove not possible at the moment)
//		Action addRegexAction = new Action("+") {
//			@Override
//			public void run() {
//				Composite parent = viewer.getControl().getParent();
//				addRegexField(parent);
//				makeTabList();
//				parent.layout();
//			}
//		};
//		toolBarManager.add(addRegexAction);
	}

	private void makeTabList() {
		final Composite parent = viewer.getControl().getParent();
		Control[] tabList = new Control[regexEntries.size() + 2];
		int i = 0;
		for (RegexEntry regexEntry : regexEntries) {
			tabList[i++] = regexEntry;
		}
		tabList[i++] = viewer.getControl();
		
		tabList[i] = tabList[0];
		parent.setTabList(tabList);
	}

	private void setRegexHistoryInComboBox(RegexEntry regexEntry) {
		String[] harray = new String[regexHistory.size()];
		for (int i = 0; i < harray.length; i++)
			harray[i] = regexHistory.get(harray.length - i - 1);
		regexEntry.setRegexHistory(harray);
	}

	private void createFindbar(final Composite parent) {
		final IFindReplaceTargetExtension target = (IFindReplaceTargetExtension)viewer.getFindReplaceTarget();
		target.beginSession();
		firstFindIndex = viewer.getSelectedRange().x;
		findIndex = viewer.getSelectedRange().x;
		if (findbar != null && !findbar.isDisposed()) {
			findbar.setFocus();
			return;
		}
		findbar = new Composite(parent, SWT.NONE);
		findbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		findbar.setLayout(layout);

		final Text findText = new Text(findbar, SWT.SINGLE | SWT.BORDER);
		findText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

		successfulSearchColor = findText.getBackground();

		ToolBar findBar = new ToolBar(findbar, SWT.FLAT | SWT.HORIZONTAL);
		ToolItem closeItem = new ToolItem(findBar, SWT.PUSH);
		closeItem.setText("X");

		ToolItem nextfindItem = new ToolItem(findBar, SWT.FLAT);
		nextfindItem.setText("Next");
		ToolItem prevfindItem = new ToolItem(findBar, SWT.PUSH | SWT.FLAT);
		prevfindItem.setText("Prev");

		final Runnable closeBar = new Runnable() {
			@Override
			public void run() {
				disposeFindbar();
				parent.layout();
				target.endSession();
			}
		};

		final Runnable findNext = new Runnable() {
			@Override
			public void run() {
				// find next
				String toFind = findText.getText();
				int index = viewer.getSelectedRange().x + 1;
				int newIndex = ((IFindReplaceTargetExtension3)target).findAndSelect(index, toFind, true, false, false, false);
				if (newIndex == -1) {
					// not found
					findText.setBackground(failedSearchColor);
				} else {
					findText.setBackground(successfulSearchColor);
					findIndex = newIndex;
				}
			}
		};

		final Runnable findPrev = new Runnable() {
			@Override
			public void run() {
				// find previous
				String toFind = findText.getText();
				int newIndex = ((IFindReplaceTargetExtension3)target).findAndSelect(findIndex, toFind, false, false, false, false);
				if (newIndex == -1) {
					// not found
					findText.setBackground(failedSearchColor);
				} else {
					findText.setBackground(successfulSearchColor);
					findIndex = newIndex;
				}
			}
		};

		closeItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				closeBar.run();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		nextfindItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				findNext.run();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		prevfindItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				findPrev.run();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		findText.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ESC) {
					closeBar.run();
				} else if (e.keyCode == SWT.ARROW_UP) {
					findPrev.run();
				} else if (e.keyCode == SWT.ARROW_DOWN) {
					findNext.run();
				}
			}
		});
		findText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				// text to find has changed, search again
				// from begin of session
				String toFind = findText.getText();
				int newIndex = ((IFindReplaceTargetExtension3)target).findAndSelect(firstFindIndex, toFind, true, false, false, false);
				if (newIndex == -1) {
					// not found
					findText.setBackground(failedSearchColor);
				} else {
					findText.setBackground(successfulSearchColor);
				}
			}
		});
		parent.layout();
		findText.setFocus();
	}


//	ActionFactory.IWorkbenchAction a = ActionFactory.FIND.create(getViewSite().getWorkbenchWindow());

	private void disposeFindbar() {
		if (findbar != null && !findbar.isDisposed())
			findbar.dispose();
		findbar = null;
		viewer.getControl().setFocus();
	}

	/**
	 * Filter the content of the currently watched editor using
	 * the regular expression in the text box.
	 *
	 * The resulting text is shown in the text viewer.
	 */
	private void doGrep() {
		String[] rxList = new String[regexEntries.size()];
		for (int rxi = 0; rxi < rxList.length; rxi++) {
			// TODO: array for last regex too...
			lastRegex = regexEntries.get(rxi).getRegexpText();
			rxList[rxi]   = lastRegex;
		}
		updateTarget();

		if (target == null)
			return;

		grepContext = submitGrep(target, rxList, csAction.isChecked(), hmAction.isChecked());

		Document document = new Document(grepContext.getText());
		viewer.setDocument(document);
		viewer.getControl().setToolTipText("source: " + target.getTitle());

		// disable the text viewer, will be enabled again when the grep is done
		viewer.getControl().setEnabled(false);
	}

	/**
	 * Set the highlight colour for match regions in the text viewer.
	 * 
	 * Ranges are picked up from the current grepContext (if no context
	 * is currently present, the method returns).
	 * 
	 */
	private void updateHighlightRanges() {
		// need a grep context here
		if (grepContext == null)
			return;
		
		AbstractDocument document = (AbstractDocument) viewer.getDocument();

		// return immediately if the document is empty, as it will still
		// report 1 line of content below and this can lead to invalid
		// access to the grep data structures
		if (document.getLength() == 0)
			return;

		int lines = document.getNumberOfLines();
		int j = 0;
		int totalMatches;
		try {
			totalMatches = grepContext.getNumberOfMatches();
			// 1 range for each highlight (background), 1 range for each line (foreground)
//			int[] ranges = new int[totalMatches*2 + lines*2];
//			StyleRange[] styles = new StyleRange[totalMatches + lines];
//			int[] ranges = new int[lines*2];
//			StyleRange[] styles = new StyleRange[lines];
			int[] ranges = new int[totalMatches*2];
			StyleRange[] styles = new StyleRange[totalMatches];
			// this same style range object is used for all matches
			// to save some memory, the real ranges are
			// in the integer arrays
//			StyleRange[] lineForegroundStyles = new StyleRange[regexEntries.size()];
//			for (int i = 0; i < lineForegroundStyles.length; i++) {
//				lineForegroundStyles[i] = new StyleRange();
//				lineForegroundStyles[i].foreground = regexEntries.get(i).getRegexColor();
//			}
//			StyleRange[] matchHighLightStyles = new StyleRange[regexEntries.size()];
//			for (int i = 0; i < matchHighLightStyles.length; i++) {
//				matchHighLightStyles[i] = new StyleRange();
//				matchHighLightStyles[i].foreground = regexEntries.get(i).getRegexColor();
//				matchHighLightStyles[i].background = highlightColor;
//			}
			// this is the range used for all match highlights (background)
			StyleRange highlightStyle = new StyleRange();
			highlightStyle.background = highlightColor;
			for (int i = 0 ; i < lines ; i++) {
				int nm = grepContext.getNumberOfMatchesForGrepLine(i);
				for (int k = 0 ; k < nm ; k++) {
					ranges[j*2]     = document.getLineOffset(i) + grepContext.getMatchBeginForGrepLine(i, k);
					ranges[j*2 + 1] = grepContext.getMatchEndForGrepLine(i, k) - grepContext.getMatchBeginForGrepLine(i, k);
					styles[j++]     = highlightStyle;
				}
			}
			viewer.getTextWidget().setStyleRanges(ranges, styles);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void updateTarget() {
		IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();

		// first check if the editor can adapt to a grep target
		IGrepTarget newTarget = (IGrepTarget) activeEditor.getAdapter(IGrepTarget.class);
		// if not, and it's a text editor, use the default implementation
		if (newTarget == null && activeEditor instanceof AbstractTextEditor)
			newTarget = new DocumentGrepTarget((AbstractTextEditor) activeEditor);

		if (newTarget != null) {
			target = newTarget;
			targetPart = activeEditor;
		}
	}

	@Override
	public void setFocus() {
		regexEntries.get(0).setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putString(KEY_GREPREGEX, lastRegex);
		memento.putBoolean(KEY_CASESENSITIVE, csAction.isChecked() );
		memento.putBoolean(KEY_HIGHLIGHTMULTIPLE, hmAction.isChecked() );
		String history = "";
		for (String helement : regexHistory) {
			history += helement + "\n";
		}
		memento.putString(KEY_REGEX_HISTORY, history);
		Color color = highlightColor;
		int defaultColor = color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
		memento.putInteger(KEY_DEFAULT_COLOR, defaultColor);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		// default colour is yellow
		int highlightCol = 0x00FFFF00;
		// check if there is a value saved in the memento
		if (memento != null) {
			lastRegex = memento.getString(KEY_GREPREGEX);
			Boolean cs = memento.getBoolean(KEY_CASESENSITIVE);
			initialCaseSensitivity = cs == null ? false : cs;
			Boolean hm = memento.getBoolean(KEY_HIGHLIGHTMULTIPLE);
			initialCaseSensitivity = hm == null ? false : hm;
			String history = memento.getString(KEY_REGEX_HISTORY);
			if (history != null) {
				String[] harray = history.split("\n");
				for (String helem : harray) {
					if (!helem.isEmpty())
						regexHistory.add(helem);
				}
			}
			Integer defaultColor = memento.getInteger(KEY_DEFAULT_COLOR);
			if (defaultColor != null)
				highlightCol = defaultColor;
		}

		highlightColor = new Color(site.getShell().getDisplay(),
				(highlightCol >> 16) & 0x00FF,
				(highlightCol >> 8) & 0x00FF,
				(highlightCol) & 0x00FF);

		// to make things simpler do not allow a null
		// regular expression
		if (lastRegex == null)
			lastRegex = "";
	}

	@Override
	public void dispose() {
		if (highlightColor != null) {
			highlightColor.dispose();
			highlightColor = null;
		}
		if (failedSearchColor != null) {
			failedSearchColor.dispose();
			failedSearchColor = null;
		}
		if (cursorLineColor != null) {
			cursorLineColor.dispose();
			cursorLineColor = null;
		}
		super.dispose();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		Object object = super.getAdapter(adapter);
		if (object == null && adapter.equals(IFindReplaceTarget.class)) {
			object = viewer.getFindReplaceTarget();
		}
		return object;
	}

	public void setGrepRegularExpression(String text) {
		regexEntries.get(0).setRegexpText(text, true);
	}
	
	public IDocument getGrepContentAsDocument() {
		return viewer.getDocument();
	}
	
	public String getOriginalForCurrentSelection() {
		String text = "";
		Point selectedRange = viewer.getSelectedRange();
		IDocument document = viewer.getDocument();
		if (selectedRange == null || document == null || isSelectionEmpty())
			return text;
		try {
			final int selectedRangeStart = selectedRange.x;
			final int selectedRangeEnd = selectedRangeStart + selectedRange.y;
			final int startLine = document.getLineOfOffset(selectedRangeStart);
			final int endLine   = document.getLineOfOffset(selectedRangeEnd);
			final int origStartLine = grepContext.getOriginalLine(startLine);
			final int origEndLine   = grepContext.getOriginalLine(endLine);
			final int startDelta = selectedRangeStart - document.getLineOffset(startLine);
			final int endDelta   = document.getLineOffset(endLine) + document.getLineLength(endLine) - selectedRangeEnd;
			text = grepContext.getTarget().getTextBetweenLines(origStartLine, origEndLine, startDelta, endDelta);
//			if (startDelta > 0 || endDelta > 0) {
//				System.out.printf("cropping text: startDelta=%d endDelta=%d\n", startDelta, endDelta);
//				text = text.substring(startDelta, text.length() - endDelta);
//			}
		} catch (BadLocationException e) {
			// TODO: log
		}
		return text;
	}

	public boolean hasGrepResult() {
		if (viewer == null || grepContext == null)
			return false;
		return true;
	}

	public boolean isSelectionEmpty() {
		if (viewer == null || grepContext == null)
			return true;
		Point range = viewer.getSelectedRange();
		return range.x < 0 || range.y <= 0;
	}
	
	public Point getSelectedRange() {
		return viewer.getSelectedRange();
	}
}

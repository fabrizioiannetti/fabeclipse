package com.github.fabeclipse.textedgrep.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.fabeclipse.textedgrep.Activator;
import com.github.fabeclipse.textedgrep.GrepTool;
import com.github.fabeclipse.textedgrep.GrepTool.GrepContext;
import com.github.fabeclipse.textedgrep.IGrepTarget;

/**
 * View to show the result of a grep operation on the
 * content of an editor.
 *
 * @author fabrizio iannetti
 */
public class GrepView extends ViewPart implements IAdaptable {
	public static final String VIEW_ID = "com.github.fabeclipse.textedgrep.grepview";

	private static final String KEY_GREPREGEX = "grepregex";
	private static final String KEY_CASESENSITIVE = "casesensitive";
	private static final String KEY_HIGHLIGHTMULTIPLE = "highlightmultiple";
	private static final String KEY_REGEX_HISTORY = "regexhistory";
	private static final String KEY_DEFAULT_COLOR = "regexcolour";

	/**
	 * @since 1.2
	 */
	protected static final int REGEX_HISTORY_MAX_SIZE = 20;

	private Color failedSearchColor = new Color(Display.getDefault(), 255, 128, 128);
	private Color successfulSearchColor;

	private TextViewer viewer;
	private String lastRegex;
	private GrepTool grepTool;
	private GrepContext grepContext;
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


	@Override
	public void createPartControl(final Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		addRegexField(parent);

		// vertical ruler that shows the original's line number
		CompositeRuler ruler = new CompositeRuler();
		ruler.addDecorator(0, new LineNumberRulerColumn() {
			@Override
			protected int computeNumberOfDigits() {
				// see SourceViewer, monkey see monkey do :)
				if (grepContext != null) {
					int digits= 2;
					double lines = grepContext.getMaxOriginalLine() + 1;
					while (lines  > Math.pow(10, digits) -1) {
						++digits;
					}
					System.out.println("Number of digits:" + digits + "for lines " + lines);
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
		});
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
				GrepContext gc = grepContext;
				IDocument document = viewer.getDocument();
				if (gc != null) {
					int line;
					try {
						line = document.getLineOfOffset(event.lineOffset);
						int index = gc.getColorForGrepLine(line);
						RegexEntry entry = regexEntries.get(index);
						event.lineBackground = entry.getRegexColor();
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
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CR && targetPart != null) {
					IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
					window.getActivePage().activate(targetPart);
				}
			}
		});

		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		menuManager.add(new Action("Grep") {
			@Override
			public void run() {
				doGrep();
				viewer.getControl().setFocus();
			}
		});
		csAction = new Action("Case Sensitive", Action.AS_CHECK_BOX) {};
		csAction.setChecked(initialCaseSensitivity);
		menuManager.add(csAction);
		hmAction = new Action("Highlight Multiple", Action.AS_CHECK_BOX) {};
		hmAction.setChecked(initialHighlightMultiple);
		menuManager.add(hmAction);

		Action colorAction = new Action("Highlight color") {
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

		menuManager.add(new Action("Edit History") {
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

	private void addRegexField(final Composite parent) {
		// when pressing ENTER in the regexp field do a grep
		IRegexEntryListener listener = new IRegexEntryListener() {
			@Override
			public void grep(String text, RegexEntry rxe) {
				// the user pressed ENTER
				doGrep();
				viewer.getControl().setFocus();
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
		grepTool = new GrepTool(rxList, csAction.isChecked());
		updateTarget();

		if (target == null)
			return;

		grepContext = grepTool.grep(target, hmAction.isChecked());
		Document document = new Document(grepContext.getText());
		viewer.setDocument(document);
		updateHighlightRanges();
		viewer.getControl().setToolTipText("source: " + target.getTitle());
	}

	private int computeRangeCount() {
		int j = 0;
		AbstractDocument document = (AbstractDocument) viewer.getDocument();
		int lines = document.getNumberOfLines();
		try {
			int totalMatches = grepContext.getNumberOfMatches();
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
//				ranges[j*2]     = document.getLineOffset(i);
//				ranges[j*2 + 1] = document.getLineLength(i);
//				styles[j++]     = lineForegroundStyles[grepContext.getColorForGrepLine(i)];
				int nm = grepContext.getNumberOfMatchesForGrepLine(i);
				int lineOffset = document.getLineOffset(i);
				int grepBegin = grepContext.getMatchBeginForGrepLine(i, 0);
				if (grepBegin > 0) {
					// there is a segment
				}
				int endOfLastMatch = 0;
				for (int k = 0 ; k < nm ; k++) {
					grepBegin = grepContext.getMatchBeginForGrepLine(i, k);
					if (grepBegin > endOfLastMatch) {
						// add the range for the text here
					}
					ranges[j*2]     = document.getLineOffset(i) + grepBegin;
					ranges[j*2 + 1] = grepContext.getMatchEndForGrepLine(i, k) - grepContext.getMatchBeginForGrepLine(i, k);
					styles[j++]     = highlightStyle;
				}
			}
			viewer.getTextWidget().setStyleRanges(ranges, styles);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return j;
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
		
		int j = 0;
		AbstractDocument document = (AbstractDocument) viewer.getDocument();
		int lines = document.getNumberOfLines();
		try {
			int totalMatches = grepContext.getNumberOfMatches();
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
//				ranges[j*2]     = document.getLineOffset(i);
//				ranges[j*2 + 1] = document.getLineLength(i);
//				styles[j++]     = lineForegroundStyles[grepContext.getColorForGrepLine(i)];
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
		IGrepTarget newTarget = (IGrepTarget) activeEditor.getAdapter(IGrepTarget.class);
		if (newTarget == null && activeEditor instanceof AbstractTextEditor) {
			newTarget = new GrepTool.DocumentGrepTarget((AbstractTextEditor) activeEditor);
		}
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

	@SuppressWarnings("rawtypes")
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
}

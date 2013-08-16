package com.github.fabeclipse.textedgrep.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.CursorLinePainter;
import org.eclipse.jface.text.Document;
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
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import com.github.fabeclipse.textedgrep.GrepTool.DocumentGrepTarget;
import com.github.fabeclipse.textedgrep.GrepTool.GrepContext;

/**
 * View to show the result of a grep operation on the
 * content of an editor.
 *
 * @author fabrizio iannetti
 */
public class GrepView extends ViewPart implements IAdaptable {

	private static final String GREPREGEX = "grepregex";

	private static final String KEY_CASESENSITIVE = "casesensitive";

	public static final String VIEW_ID = "com.github.fabeclipse.textedgrep.grepview";

	private static final String KEY_HIGHLIGHTMULTIPLE = "highlightmultiple";

	private static final String KEY_REGEX_HISTORY = "regexhistory";

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
	private AbstractTextEditor textEd;
	private Color highlightColor;
	private Combo regexpText;
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
			if (linkToEditorAction.isChecked() && part instanceof EditorPart && part != textEd) {
				// ok, it's a non null editor, and it is not the current one
				// grep it
				doGrep();
			}
		}
	};

	private Action linkToEditorAction;

	private Action csAction;

	private boolean initialHighlightMultiple;

	private Action hmAction;

	private Composite findbar;
	private List<String> regexHistory = new ArrayList<String>();

	@Override
	public void createPartControl(final Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		regexpText = new Combo(parent, SWT.SINGLE);
		regexpText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		regexpText.setText(lastRegex);
		setRegexHistoryInComboBox();

		// when pressing ENTER in the regexp field do a grep
		regexpText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// this is never called for Text
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// the user pressed ENTER
				doGrep();
				viewer.getControl().setFocus();
				String text = regexpText.getText();
				// add regex to history if:
				// * not empty
				// * history is empty, or last element of history is not the same
				if (!text.isEmpty() && (regexHistory.isEmpty() || !regexHistory.get(regexHistory.size() - 1).equals(text))) {
					while (regexHistory.size() >= REGEX_HISTORY_MAX_SIZE)
						regexHistory.remove(0);
					regexHistory.add(text);
					setRegexHistoryInComboBox();
				}
			}
		});

// remove this as the combo triggers the drop down with the arrow down key
//		regexpText.addKeyListener(new KeyListener() {
//			@Override
//			public void keyReleased(KeyEvent e) {
//				// key down modes to the grep viewer
//				if (e.keyCode == SWT.ARROW_DOWN)
//					viewer.getControl().setFocus();
//			}
//			@Override
//			public void keyPressed(KeyEvent e) {}
//		});

		// vertical ruler that shows the original's line number
		CompositeRuler ruler = new CompositeRuler();
		ruler.addDecorator(0, new LineNumberRulerColumn() {
			@Override
			protected int computeNumberOfDigits() {
				// see SourceViewer, monkey see monkey do :)
				if (grepContext != null) {
					int digits= 2;
					double lines = grepContext.getMaxOriginalLine();
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
		});
		viewer = new SourceViewer(parent, ruler , SWT.FLAT | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// use the default text font (the same used in text editor)
		viewer.getTextWidget().setFont(JFaceResources.getTextFont());
		// this is a read-only view!
		viewer.setEditable(false);
		// highlight the cursor line
		CursorLinePainter cursorLinePainter = new CursorLinePainter(viewer);
		highlightColor = new Color(parent.getDisplay(), new RGB(200, 200, 0));
		cursorLinePainter.setHighlightColor(highlightColor);
		viewer.addPainter(cursorLinePainter);

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
						textEd.selectAndReveal(offset, 0);
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
					setRegexHistoryInComboBox();
				}
			}
		});

		linkToEditorAction = new Action("Link To Editor",Action.AS_CHECK_BOX) {};
		ImageDescriptor image = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/synced.gif");
		linkToEditorAction.setImageDescriptor(image);
		linkToEditorAction.setToolTipText("Sync Grep Content to active editor\nAs soon as an editor is activated its content is filtered");
		getViewSite().getActionBars().getToolBarManager().add(linkToEditorAction);

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

		// make tab key to toggle between the regular expression text and the viewer
		parent.setTabList(new Control[] {regexpText, viewer.getControl(), regexpText});
	}

	private void setRegexHistoryInComboBox() {
		String[] harray = new String[regexHistory.size()];
		for (int i = 0; i < harray.length; i++)
			harray[i] = regexHistory.get(harray.length - i - 1);
		regexpText.setItems(harray);
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
		lastRegex = regexpText.getText();
		grepTool = new GrepTool(lastRegex, csAction.isChecked());
		IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();
		if (activeEditor instanceof AbstractTextEditor) {
			textEd = (AbstractTextEditor) activeEditor;
		}
		DocumentGrepTarget target = new GrepTool.DocumentGrepTarget(textEd);
		grepContext = grepTool.grep(target, hmAction.isChecked());
		Document document = new Document(grepContext.getText());
		viewer.setDocument(document);
		int lines = document.getNumberOfLines();
		try {
			int totalMatches = grepContext.getNumberOfMatches();
			int[] ranges = new int[totalMatches*2];
			StyleRange[] styles = new StyleRange[totalMatches];
			// this same style range object is used for all matches
			// to save some memory, the real ranges are
			// in the integer arrays
			StyleRange matchHighLightStyle = new StyleRange();
			matchHighLightStyle.background = viewer.getTextWidget().getDisplay().getSystemColor(SWT.COLOR_YELLOW);
			for (int i = 0, j = 0 ; i < lines ; i++) {
				int nm = grepContext.getNumberOfMatchesForGrepLine(i);
				for (int k = 0 ; k < nm ; k++) {
					ranges[j*2]     = document.getLineOffset(i) + grepContext.getMatchBeginForGrepLine(i, k);
					ranges[j*2 + 1] = grepContext.getMatchEndForGrepLine(i, k) - grepContext.getMatchBeginForGrepLine(i, k);
					styles[j++]     = matchHighLightStyle;
				}
			}
			viewer.getTextWidget().setStyleRanges(ranges, styles);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		viewer.getControl().setToolTipText("source: " + textEd.getTitle());
	}

	@Override
	public void setFocus() {
		regexpText.setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putString(GREPREGEX, lastRegex);
		memento.putBoolean(KEY_CASESENSITIVE, csAction.isChecked() );
		memento.putBoolean(KEY_HIGHLIGHTMULTIPLE, hmAction.isChecked() );
		String history = "";
		for (String helement : regexHistory) {
			history += helement + "\n";
		}
		memento.putString(KEY_REGEX_HISTORY, history);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		// check if there is a value saved in the memento
		if (memento != null) {
			lastRegex = memento.getString(GREPREGEX);
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
		}

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
			failedSearchColor.dispose();
		}
		super.dispose();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		Object object = super.getAdapter(adapter);
		System.out.println(adapter + "->" + object);
		if (object == null && adapter.equals(IFindReplaceTarget.class)) {
			object = viewer.getFindReplaceTarget();
			System.out.println("     ->" + object);
		}
		return object;
	}

	public void setGrepRegularExpression(String text) {
		regexpText.setText(text);
		regexpText.setSelection(new Point(0, text.length()));;
	}
}

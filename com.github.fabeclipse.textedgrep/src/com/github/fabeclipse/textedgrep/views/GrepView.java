package com.github.fabeclipse.textedgrep.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.CursorLinePainter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.fabeclipse.textedgrep.Activator;
import com.github.fabeclipse.textedgrep.GrepTool;
import com.github.fabeclipse.textedgrep.GrepTool.GrepContext;

/**
 * View to show the result of a grep operation on the
 * content of an editor.
 * 
 * @author fabrizio iannetti
 */
public class GrepView extends ViewPart {

	private static final String GREPREGEX = "grepregex";

	private static final String KEY_CASESENSITIVE = "casesensitive";

	public static final String VIEW_ID = "com.github.fabeclipse.textedgrep.grepview";

	private static final String KEY_HIGHLIGHTMULTIPLE = "highlightmultiple";
	
	private IDocument document = new Document();
	private TextViewer viewer;
	private String lastRegex;
	private GrepTool grepTool;
	private GrepContext grepContext;
	private AbstractTextEditor textEd;
	private Color highlightColor;
	private Text regexpText;
	private boolean initialCaseSensitivity;

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
			if (linkToEditorAction.isChecked() && part instanceof EditorPart) {
				// ok, it's a non null editor, grep it
				doGrep();
			}
		}
	};

	private Action linkToEditorAction;

	private Action csAction;

	private boolean initialHighlightMultiple;

	private Action hmAction;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		regexpText = new Text(parent, SWT.SINGLE);
		regexpText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		regexpText.setText(lastRegex);

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
			}
		});

		regexpText.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				// key down modes to the grep viewer
				if (e.keyCode == SWT.ARROW_DOWN)
					viewer.getControl().setFocus();
			}
			@Override
			public void keyPressed(KeyEvent e) {}
		});

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

		viewer.setDocument(document);

		// track cursor line and synchronise the cursor position in the editor
		viewer.getTextWidget().addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				int caretOffset = event.caretOffset;
				if (grepContext != null) {
					try {
						int grepLine = document.getLineOfOffset(caretOffset);
						int line = grepContext.getOriginalLine(grepLine);
						int offset = grepContext.getDocument().getLineOffset(line);
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

		linkToEditorAction = new Action("Link To Editor",Action.AS_CHECK_BOX) {};
		ImageDescriptor image = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/synced.gif");
		linkToEditorAction.setImageDescriptor(image);
		linkToEditorAction.setToolTipText("Sync Grep Content to active editor\nAs soon as an editor is activated its content is filtered");
		getViewSite().getActionBars().getToolBarManager().add(linkToEditorAction);

		getViewSite().getActionBars().updateActionBars();

		IPartService partService = (IPartService) getViewSite().getService(IPartService.class);
		partService.addPartListener(partListener);

		// make tab key to toggle between the regular expression text and the viewer
		parent.setTabList(new Control[] {regexpText, viewer.getControl(), regexpText});
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
		grepContext = grepTool.grepEditor(textEd, hmAction.isChecked());
		String grep = grepContext.getText();
		document.set(grep);
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
		}
		super.dispose();
	}
}

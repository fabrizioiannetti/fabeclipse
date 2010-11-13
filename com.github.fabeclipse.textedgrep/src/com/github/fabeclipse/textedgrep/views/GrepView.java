package com.github.fabeclipse.textedgrep.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.fabeclipse.textedgrep.GrepTool;
import com.github.fabeclipse.textedgrep.GrepTool.GrepContext;

public class GrepView extends ViewPart {

	private IDocument document = new Document();
	private TextViewer viewer;
	private String lastRegex;
	private GrepTool grepTool;
	private GrepContext grepContext;
	private AbstractTextEditor textEd;
	private Color highlightColor;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		// add a text box for the regular expression
		final Text regexpText = new Text(parent, SWT.SINGLE);
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
				doGrep(regexpText);
			}
		});

		// vertical ruler that shows the original's line number
		CompositeRuler ruler = new CompositeRuler();
		ruler.addDecorator(0, new LineNumberRulerColumn() {
			@Override
			protected int computeNumberOfDigits() {
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
				doGrep(regexpText);
			}
		});
		getViewSite().getActionBars().updateActionBars();
	}

	private void doGrep(final Text regexpText) {
		lastRegex = regexpText.getText();
		grepTool = new GrepTool(lastRegex);
		IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();
		if (activeEditor instanceof AbstractTextEditor) {
			textEd = (AbstractTextEditor) activeEditor;
		}
		grepContext = grepTool.grepCurrentEditor(window);
		String grep = grepContext.getText();
		document.set(grep);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putString("grepregex", lastRegex);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		// check if there is a value saved in the memento
		if (memento != null)
			lastRegex = memento.getString("grepregex");

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

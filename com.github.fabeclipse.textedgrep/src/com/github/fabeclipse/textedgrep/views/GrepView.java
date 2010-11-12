package com.github.fabeclipse.textedgrep.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.github.fabeclipse.textedgrep.GrepTool;

public class GrepView extends ViewPart {

	private IDocument document = new Document();
	private TextViewer viewer;
	private String lastRegex;
	private GrepTool grepTool;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		
		final Text regexpText = new Text(parent, SWT.SINGLE);
		regexpText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		regexpText.setText(lastRegex);
		
		regexpText.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				// never called for Text
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// the user pressed ENTER
				doGrep(regexpText);
			}
		});

		viewer = new TextViewer(parent, SWT.FLAT | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTextWidget().setFont(JFaceResources.getTextFont());
		
		viewer.setDocument(document);
		
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
		String grep = grepTool.grepCurrentEditor(getViewSite().getWorkbenchWindow());
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
	
}

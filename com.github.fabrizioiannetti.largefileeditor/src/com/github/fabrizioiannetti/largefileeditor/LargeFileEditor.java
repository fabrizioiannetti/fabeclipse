package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;

import com.github.fabeclipse.textedgrep.IGrepTarget;

public class LargeFileEditor extends EditorPart implements IFindReplaceTarget {

	private File textFile;
	private FileTextViewer viewer;
	private IPropertyChangeListener fFontPropertyChangeListener= new FontPropertyChangeListener();
	private IContextActivation context;

	/**
	 * Internal property change listener for handling workbench font changes.
	 * @since 2.1
	 */
	class FontPropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (viewer == null)
				return;

			String property= event.getProperty();

			if (JFaceResources.TEXT_FONT.equals(property)) {
				viewer.setFont(JFaceResources.getTextFont());
				return;
			}
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// this editor is read only : save does not do anything
		if (monitor != null) {
			monitor.done();
		}
	}

	@Override
	public void doSaveAs() {
		// TODO allow copy?
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		if (input instanceof IURIEditorInput) {
			setInput(input);
			IURIEditorInput fileInput = (IURIEditorInput) input;
			URI uri = fileInput.getURI();
			textFile = new File(uri);
			// TODO: only allow local files?
		}
		if (textFile == null) {
			throw new PartInitException("Could not read input:" + input.getName());
		}
	}

	@Override
	public boolean isDirty() {
		// this editor is read-only
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO allow copy?
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(GridLayoutFactory.fillDefaults().create());
		viewer = new FileTextViewer(textFile, parent, SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		viewer.setFont(JFaceResources.getTextFont());
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
		
		createActions();
		activateGrepTargetContext();
	}

	private void activateGrepTargetContext() {
		IContextService contextService = (IContextService) getSite().getService(IContextService.class);
		context = contextService.activateContext("org.eclipse.ui.textEditorScope");
	}

	private void deactivateGrepTargetContext() {
		if (context == null)
			return;
		IContextService contextService = (IContextService) getSite().getService(IContextService.class);
		contextService.deactivateContext(context);
	}

	private void createActions() {
		FindReplaceAction action = new FindReplaceAction(new LargeFileEditorMessages(), null, this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_ACTION);
		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		putAction(ActionFactory.FIND.getId(), action);
	}

	private Map<String, IAction> fActions = new HashMap<String, IAction>();

	private void putAction(String key, IAction action) {
		fActions.put(key, action);
	}

	/**
	 * Get an action implemented by this editor by id.
	 * 
	 * Currently implemented:
	 * <ul>
	 * <li> find, with id {@link ActionFactory.FIND.getId()}
	 * </ul>
	 * 
	 * @param actionId
	 * @return
	 */
	public IAction getAction(String actionId) {
		return fActions.get(actionId);
	}

	public void select(int start, int length) {
		viewer.setSelection(start, start + length);
	}

	public FileTextViewer getViewer() {
		return viewer;
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
		deactivateGrepTargetContext();
	}

	@Override
	public boolean canPerformFind() {
		return true;
	}

	@Override
	public int findAndSelect(int widgetOffset, String findString,
			boolean searchForward, boolean caseSensitive, boolean wholeWord) {
		FileTextModel model = viewer.getModel();
		long pos = model.findString(findString, widgetOffset, caseSensitive, searchForward, null);
		if (pos >= 0) {
			viewer.setSelection((int) pos, (int) pos + findString.length());
		}
		return (int) pos;
	}

	@Override
	public Point getSelection() {
		// limit to a single line
		Point selection = viewer.getSelectionMaxOneLine();
		return selection;
	}

	@Override
	public String getSelectionText() {
		// limit to a single line
		String text = viewer.getSelectionTextMaxOneLine();
		return text;
	}

	@Override
	public boolean isEditable() {
		// edit not supported
		return false;
	}

	@Override
	public void replaceSelection(String text) {
		// edit not supported
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (IGrepTarget.class.equals(adapter)) {
			return new LargeFileGrepTarget(this);
		}
		return super.getAdapter(adapter);
	}
}

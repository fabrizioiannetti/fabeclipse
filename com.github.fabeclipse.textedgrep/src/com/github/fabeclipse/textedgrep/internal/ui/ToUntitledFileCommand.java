package com.github.fabeclipse.textedgrep.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.github.fabeclipse.textedgrep.Activator;

public class ToUntitledFileCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		IFileStore fileStore = queryFileStore();
		IEditorInput input = createEditorInput(fileStore);
		String editorId = getEditorId(activeWorkbenchWindow.getWorkbench(), fileStore);
		IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
		try {
			page.openEditor(input, editorId);
		} catch (PartInitException e) {
			// TODO: log
		}
		return null;
	}

	private IFileStore queryFileStore() {
		IPath stateLocation= Activator.getInstance().getStateLocation();
		IPath path= stateLocation.append("/_" + new Object().hashCode()); //$NON-NLS-1$
		return EFS.getLocalFileSystem().getStore(path);
	}

	private String getEditorId(IWorkbench workbench, IFileStore fileStore) {
		IEditorRegistry editorRegistry= workbench.getEditorRegistry();
		IEditorDescriptor descriptor= editorRegistry.getDefaultEditor(fileStore.getName());
		if (descriptor != null)
			return descriptor.getId();
		return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	}

	private IEditorInput createEditorInput(IFileStore fileStore) {
		return new NonExistingFileEditorInput(fileStore, "Grep Result");
	}
}

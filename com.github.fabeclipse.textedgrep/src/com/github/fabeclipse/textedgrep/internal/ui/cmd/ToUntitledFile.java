package com.github.fabeclipse.textedgrep.internal.ui.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.fabeclipse.textedgrep.Activator;
import com.github.fabeclipse.textedgrep.internal.ui.GrepView;
import com.github.fabeclipse.textedgrep.internal.ui.NonExistingFileEditorInput;

public class ToUntitledFile extends AbstractHandler {
	private static final int MAX_COPY_TEXT_LENGTH = 10000000; // TODO: this is arbitrary

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			GrepView gv = (GrepView) HandlerUtil.getActivePart(event);
			AbstractTextEditor editor = createUntitledEditor(event);
			IDocument editorDocument = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			IDocument grepDocument = gv.getGrepContentAsDocument();
			if (editorDocument != null && grepDocument != null) {
				int start, len;
				if (gv.isSelectionEmpty()) {
					start = 0;
					len = Math.min(grepDocument.getLength(), MAX_COPY_TEXT_LENGTH);
				}
				else {
					Point range = gv.getSelectedRange();
					start = range.x;
					len = range.y;
				}
				editorDocument.set(grepDocument.get(start, len));
			}
		} catch (PartInitException e) {
			// TODO log error
		} catch (ClassCastException e) {
			// TODO log error
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private AbstractTextEditor createUntitledEditor(ExecutionEvent event) throws PartInitException, ClassCastException {
		IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		IFileStore fileStore = queryFileStore();
		IEditorInput input = createEditorInput(fileStore);
		String editorId = getEditorId(activeWorkbenchWindow.getWorkbench(), fileStore);
		IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
		IEditorPart editor = page.openEditor(input, editorId);
		return (AbstractTextEditor) editor;
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

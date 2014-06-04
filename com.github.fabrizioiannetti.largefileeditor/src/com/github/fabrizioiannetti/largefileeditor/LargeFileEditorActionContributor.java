package com.github.fabrizioiannetti.largefileeditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;

public class LargeFileEditorActionContributor extends EditorActionBarContributor {

	public void setActiveEditor(IEditorPart part) {
		IActionBars bars= getActionBars();
		if (bars == null)
			return;
		if (part instanceof LargeFileEditor) {
			LargeFileEditor editor = (LargeFileEditor) part;
			IAction findAction = editor.getAction(ActionFactory.FIND.getId());
			bars.setGlobalActionHandler(ActionFactory.FIND.getId(), findAction);
			bars.updateActionBars();
		}
	}
}

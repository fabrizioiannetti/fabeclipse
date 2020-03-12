package fab.faber.launchview.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

public class ActionFactory {
	public Action make(String text, String tooltip, Runnable action) {
		return make(text, tooltip, IAction.AS_PUSH_BUTTON, action);
	}
	public Action makeChecked(String text, String tooltip, Runnable action) {
		Action a = make(text, tooltip, IAction.AS_CHECK_BOX, action);
		a.setChecked(false);
		return a;
	}
	public Action make(String text, String tooltip, int type, Runnable action) {
		Action a = new Action(text, type) {
			@Override
			public void run() {
				action.run();
			}
		};
		a.setToolTipText(tooltip);
		return a;
	}
	public Action make(Runnable action) {
		return new Action() {
			@Override
			public void run() {
				action.run();
			}
		};
	}

}

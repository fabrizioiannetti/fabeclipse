package plots.views;

import java.util.function.Consumer;

import org.eclipse.jface.action.Action;

public class LambdaAction extends Action {
	private Consumer<LambdaAction> toDo;
	public LambdaAction(Consumer<LambdaAction> toDo) {
		this.toDo = toDo;
	}
	public LambdaAction(String text, String toolTip, Consumer<LambdaAction> toDo) {
		super(text);
		this.toDo = toDo;
		setToolTipText(toolTip);
	}
	@Override
	public void run() {
		toDo.accept(this);
	}
}

package com.github.fabeclipse.textedgrep.internal.ui;

import java.util.Collection;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.TextSelection;

public class GrepViewPropertyTester extends PropertyTester {

	public GrepViewPropertyTester() {
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if ("nonEmptyTextSelection".equals(property)) {
			if (receiver instanceof Collection<?>) {
				Collection<?> selection = (Collection<?>) receiver;
				if (selection.size() > 0) {
					return testObject(selection.iterator().next());
				}
			}
			return false;
		}
		return false;
	}

	private boolean testObject(Object o) {
		if (o instanceof TextSelection) {
			TextSelection txtSelection = (TextSelection) o;
			return txtSelection.getLength() > 0;
		}
		return false;
	}

}

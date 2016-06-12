package com.github.fabeclipse.textedgrep.internal.ui;

import org.eclipse.core.expressions.PropertyTester;

public class GrepViewPropertyTester extends PropertyTester {
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		boolean result = false;
		if (receiver instanceof GrepView) {
			GrepView gv = (GrepView) receiver;
			if ("nonEmptyTextSelection".equals(property))
				result = !gv.isSelectionEmpty();
			else if ("nonEmptyGrepResult".equals(property))
				result = gv.hasGrepResult();
		}
		return result;
	}
}

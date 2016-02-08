package com.github.fabeclipse.textedgrep;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A class to monitor the grep progress.
 * By default it ignores every notification
 * @since 3.0
 */
public class GrepMonitor {

	private Consumer<IGrepContext> changeFunc = new Consumer<IGrepContext>() {
		@Override
		public void accept(IGrepContext t) {}
	};
	private IntConsumer progressFunc = new IntConsumer() {
		@Override
		public void accept(int value) {}
	};
	private boolean canceled;

	public GrepMonitor onChange(Consumer<IGrepContext> changeFunc) {
		this.changeFunc = changeFunc;
		return this;
	}
	
	/**
	 * Registers a function to handle the progress reporting.
	 * 
	 * @param progressFunc function called when progress changes, progress is
	 *                     reported in percent, from 0 to 100 included.
	 * @return this instance.
	 */
	public GrepMonitor onProgress(IntConsumer progressFunc) {
		this.progressFunc = progressFunc;
		return this;
	}

	public GrepMonitor cancel() {
		canceled = true;
		return this;
	}

	GrepMonitor fireChange(IGrepContext context) {
		changeFunc.accept(context);
		return this;
	}

	GrepMonitor fireProgress(int progress) {
		progressFunc.accept(progress);
		return this;
	}

	public boolean isCanceled() {
		return canceled;
	}
}

package com.github.fabeclipse.textedgrep;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @since 2.0
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.github.fabeclipse.textedgrep";
	private static Activator instance;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

	public static Activator getInstance() {
		return instance;
	}

}

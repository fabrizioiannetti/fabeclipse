package com.github.fabeclipse.test.textedgrep;

import java.io.IOException;
import java.util.logging.Logger;

import org.lttng.ust.agent.jul.LttngLogHandler;

public class TestLttngLogger {

	private Logger logger;
	private LttngLogHandler lttngUstLogHandler;

	public TestLttngLogger() {
		logger = Logger.getLogger("perfgrep");

		try {
			lttngUstLogHandler = new LttngLogHandler();
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Add the LTTng-UST log handler to our logger
		logger.addHandler(lttngUstLogHandler);
	}
	public void teardown() {
		logger.removeHandler(lttngUstLogHandler);
		lttngUstLogHandler.close();
	}

	public void info(String string) {
		logger.info(string);
	}
}

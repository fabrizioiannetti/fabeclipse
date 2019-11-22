package com.github.fabeclipse.test.textedgrep;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPart;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.fabeclipse.textedgrep.GrepMonitor;
import com.github.fabeclipse.textedgrep.GrepTool;
import com.github.fabeclipse.textedgrep.IGrepContext;
import com.github.fabeclipse.textedgrep.IGrepTarget;
import com.github.fabeclipse.textedgrep.internal.ui.DocumentGrepTarget;
import java.util.logging.Handler;
import java.util.logging.Logger;
import org.lttng.ust.agent.jul.LttngLogHandler;

class PerfGrep {

	private static final int THREADING_TEST_NUM_LINES = 1000000;
	private static final int THREADING_TEST_REPETITIONS = 20;
	private static final String THREADING_TEST_LINE   = "[00000000][WARN][MAIN] This is the only line, repeated multiple times";
	private static final String THREADING_TEST_LINE2  = "[00000000][WARN] This is the non-matching line, repeated multiple times";
	private static final String THREADING_TEST_REGEX = "\\[(\\d+)\\]\\[(\\w+)\\]\\[(\\w+)\\].*repeated";
	private static TestLttngLogger logger;
	
	@BeforeAll
    static private void setup()
    {
        logger = new TestLttngLogger();
    }

	@AfterAll
	static private void teardown() {
		logger.teardown();
	}

	private static class TestGrepTarget implements IGrepTarget {
		private int currLine = 0;
		private int numLines;
		private String line;
		public TestGrepTarget(int numLines, String line) {
			this.numLines = numLines;
			this.line = line;
		}
		@Override
		public void start() {}
		@Override
		public void stop() {}
		@Override
		public boolean hasNextLine() { return currLine < numLines; }
		@Override
		public String nextLine() { currLine++; return line; }
		@Override
		public long getLength() { return line.length() * numLines; }
		@Override
		public void select(int start, int length) {}
		@Override
		public int getLineOffset(int line) { return line * this.line.length(); }
		@Override
		public boolean isSame(IWorkbenchPart part) { return false; }
		@Override
		public String getTitle() { return ""; }
		@Override
		public String getTextBetweenLines(int origStartLine, int origEndLine, int origStartOffset, int origEndOffset) {
			// TODO
			return null;
		}
	}
	@Test
	void testGrep() {
		TestGrepTarget target = new TestGrepTarget(1000000, "This is the only line, repeated multiple times");
		System.out.println("Testing with numLines=: " + target.numLines + ", totalLength=" + target.getLength());
		GrepTool tool = new GrepTool(new String[] {THREADING_TEST_REGEX}, true);
		IGrepContext context = tool.grepStart(target);
		long tic = System.currentTimeMillis();
		tool.grep(context, new GrepMonitor(), false);
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}

	private class GrepRunner implements Runnable {
		private final GrepTool tool;
		private final IGrepTarget target;
		private final int repetitions;
		public GrepRunner(final int numLines, final int repetitions) {
			this(new TestGrepTarget(numLines, "This is the only line, repeated multiple times"), repetitions);
		}
		public GrepRunner(final IGrepTarget target, final int repetitions) {
			this.target = target;
			this.repetitions = repetitions;
			tool = new GrepTool(new String[] {THREADING_TEST_REGEX}, true);
		}
		@Override
		public void run() {
	        logger.info("start");
			for (int i = 0; i < repetitions; i++) {
				IGrepContext context = tool.grepStart(target);
				tool.grep(context, new GrepMonitor(), false);
			}
	        logger.info("stop");
		}
	}
	private class GrepRunnerPool {
		private GrepRunner[] runners;
		private Thread[] threads;
		public GrepRunnerPool(int numThreads, int numLines) {
			runners = new GrepRunner[numThreads];
			threads = new Thread[numThreads];
			for (int i = 0; i < runners.length; i++) {
				runners[i] = new GrepRunner(numLines / numThreads, THREADING_TEST_REPETITIONS);
				threads[i] = new Thread(runners[i]);
			}
		}
		public GrepRunnerPool(int numThreads, IDocument document) throws BadLocationException {
			List<IGrepTarget> targets = DocumentGrepTarget.partitioned(document, numThreads);
			runners = new GrepRunner[numThreads];
			threads = new Thread[numThreads];
			for (int i = 0; i < runners.length; i++) {
				runners[i] = new GrepRunner(targets.get(i), THREADING_TEST_REPETITIONS);
				threads[i] = new Thread(runners[i]);
			}
		}
		public void run() throws InterruptedException {
			for (Thread t : threads) {
				t.start();
			}
			for (Thread t : threads) {
				t.join();
			}
		}
		public long getLength() {
			long length = 0;
			for (GrepRunner runner : runners) {
				length += runner.target.getLength();
			}
			return length;
		}
	}
	@Test
	void testGrepParallel1() throws InterruptedException {
		int numThreads = 1;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, numLines);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	@Test
	void testGrepParallel2() throws InterruptedException {
		int numThreads = 2;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, numLines);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	@Test
	void testGrepParallel4() throws InterruptedException {
		int numThreads = 4;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, numLines);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	@Test
	void testGrepParallel6() throws InterruptedException {
		int numThreads = 6;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, numLines);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	@Test
	void testGrepParallel8() throws InterruptedException {
		int numThreads = 8;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, numLines);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	
	static private Document createTestDocument() {
		Document document = new Document();
		StringBuffer text = new StringBuffer(THREADING_TEST_NUM_LINES * THREADING_TEST_LINE.length());
		for (int i = 0; i < THREADING_TEST_NUM_LINES; i += 100) {
			text.append(THREADING_TEST_LINE);
			text.append("\n");
			for (int j = 0; j < 99; j++) {
				text.append(THREADING_TEST_LINE2);
				text.append("\n");
			}
		}
		document.set(text.toString());
		return document;
	}
	@Test
	void testGrepDocumentParalle1() throws InterruptedException, BadLocationException {
		Document document = createTestDocument();
		int numThreads = 1;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, document);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	@Test
	void testGrepDocumentParallel4() throws InterruptedException, BadLocationException {
		Document document = createTestDocument();
		int numThreads = 4;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, document);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}
	@Test
	void testGrepDocumentParallel8() throws InterruptedException, BadLocationException {
		Document document = createTestDocument();
		int numThreads = 8;
		int numLines = THREADING_TEST_NUM_LINES;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, document);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}

}

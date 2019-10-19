package com.github.fabeclipse.test.textedgrep;

import org.eclipse.ui.IWorkbenchPart;
import org.junit.jupiter.api.Test;

import com.github.fabeclipse.textedgrep.GrepMonitor;
import com.github.fabeclipse.textedgrep.GrepTool;
import com.github.fabeclipse.textedgrep.IGrepContext;
import com.github.fabeclipse.textedgrep.IGrepTarget;

class PerfGrep {

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
		GrepTool tool = new GrepTool(new String[] {"only"}, true);
		IGrepContext context = tool.grepStart(target);
		long tic = System.currentTimeMillis();
		tool.grep(context, new GrepMonitor(), false);
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}

	static private class GrepRunner implements Runnable {
		private TestGrepTarget target;
		GrepTool tool;
		IGrepContext context;
		public GrepRunner(int numLines) {
			target = new TestGrepTarget(numLines, "This is the only line, repeated multiple times");
			tool = new GrepTool(new String[] {"only"}, true);
			context = tool.grepStart(target);
		}
		@Override
		public void run() {
			tool.grep(context, new GrepMonitor(), false);
		}
	}
	static private class GrepRunnerPool {
		private GrepRunner[] runners;
		private Thread[] threads;
		public GrepRunnerPool(int numThreads, int numLines) {
			runners = new GrepRunner[numThreads];
			threads = new Thread[numThreads];
			for (int i = 0; i < runners.length; i++) {
				runners[i] = new GrepRunner(numLines / numThreads);
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
		int numLines = 1000000;
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
		int numLines = 1000000;
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
		int numLines = 1000000;
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
		int numLines = 1000000;
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
		int numLines = 1000000;
		GrepRunnerPool pool = new GrepRunnerPool(numThreads, numLines);
		System.out.println("Testing with numThreads=" + numThreads + ", numLines=" + numLines + ", totalLength=" + pool.getLength());
		long tic = System.currentTimeMillis();
		pool.run();
		long toc = System.currentTimeMillis() - tic;
		System.out.println("Elapsed: " + toc + "ms");
	}

}

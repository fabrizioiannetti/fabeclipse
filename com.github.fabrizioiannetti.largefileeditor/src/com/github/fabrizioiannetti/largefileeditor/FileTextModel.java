package com.github.fabrizioiannetti.largefileeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class is the access point to the text file content.
 * A client (e.g. the viewer) should use this class in order
 * to get portion of text to display or to search through it.
 * 
 * The content is not loaded in memory but rather kept on the
 * file system to optimise memory usage when browsing large
 * files as, for example, logs.
 * 
 * @author Fabrizio Iannetti
 */
public class FileTextModel {
	private static final String FILE_FORMAT = "ISO-8859-1";

	private File textFile;
	
	private int lineCount;
	private long[] lineOffsets;
	private long length;

	private FileTextScanner textScanner;

	/**
	 * Create a model for the given file, which must exist
	 * on the local file system.
	 * 
	 * @param textFile the file to model
	 * @param monitor monitor to report scanning progress
	 */
	public FileTextModel(File textFile, IProgressMonitor monitor) {
		super();
		this.textFile = textFile;
		length = textFile.length();
		textScanner = new FileTextScanner(monitor);
		textScanner.start();
	}

	public synchronized boolean isReady() {
		return textScanner == null || !textScanner.isAlive();
	}

	public int getLineCount() {
		return lineCount;
	}

	public int getLineIndex(long offset) {
		if (offset < 0 || offset > length)
			return -1;
		if (offset == length) {
			return lineCount - 1;
		}
		int index = lineCount / 2;
		int last = lineCount;
		int first = 0;
		while (!(lineOffsets[index] <= offset && offset < lineOffsets[index+1])) {
			if (offset < lineOffsets[index]) {
				last = index;
				index = (first + index) /2;
			}
			else {
				first = index;
				index = (last + index) /2;
			}
		}
		return index;
	}

	public static class LineOffsets {
		public long start;
		public long end;
	}
	public void getOffsetsForLine(int index, LineOffsets offsets) {
		offsets.start = lineOffsets[index];
		offsets.end = lineOffsets[index + 1];
	}

	public String getLine(int index) {
		SeekableByteChannel byteChannel = null;
		String line = "error";
		LineOffsets lo = new LineOffsets();
		getOffsetsForLine(index, lo);
		try {
			byteChannel = Files.newByteChannel(textFile.toPath(), StandardOpenOption.READ);
			byteChannel.position(lo.start);
			ByteBuffer lineByteBuffer = ByteBuffer.allocate((int) (lo.end - lo.start));
			int read = byteChannel.read(lineByteBuffer);
			// do not take line terminator in the line string
			if (read > 0 && (lineByteBuffer.get(read - 1) == '\n' || lineByteBuffer.get(read - 1) == '\r'))
				read--;
			if (read > 0 && (lineByteBuffer.get(read - 1) == '\n' || lineByteBuffer.get(read - 1) == '\r'))
				read--;
			line = new String(lineByteBuffer.array(), 0, read, Charset.forName(FILE_FORMAT));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (byteChannel != null)
				try {
					byteChannel.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return line ;
	}

	public long getLength() {
		return length;
	}

	public String getText(long offset, int length) {
		String line = "error";
		try {
			SeekableByteChannel byteChannel = Files.newByteChannel(textFile.toPath(), StandardOpenOption.READ);
			byteChannel.position(offset);
			ByteBuffer lineByteBuffer = ByteBuffer.allocate(length);
			byteChannel.read(lineByteBuffer);
			line = new String(lineByteBuffer.array(), Charset.forName(FILE_FORMAT));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line ;
	}
	private synchronized void setReady(int lineCount, long[] lineOffsets) {
		this.lineCount = lineCount;
		this.lineOffsets = lineOffsets;
		textScanner = null;
	}

	/**
	 * Class to scan the file for line offsets. The
	 * offsets are stored in an array, where the index
	 * is the line number (starting from 0).
	 * 
	 * @author Fabrizio Iannetti
	 *
	 */
	private class FileTextScanner extends Thread {
		private int lineCount = 1;
		private long[] lineOffsets = new long[1000000];
		private IProgressMonitor monitor;

		public FileTextScanner(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public void run() {
			char[] buf = new char[100000];
			int readChars;
			Charset charset = Charset.forName(FILE_FORMAT);
			BufferedReader reader = null;
			try {
				long fileLength = textFile.length();
				monitor.beginTask("mapping lines in file", (int) (fileLength/lineOffsets.length));
				long bufOffset = 0;
				reader = Files.newBufferedReader(textFile.toPath(), charset);
				// read until EOF
				while ((readChars = reader.read(buf)) >= 0) {
					parseBuffer(buf, readChars, bufOffset);
					bufOffset += readChars;
					monitor.worked(1);
				}
				// always add the total length as an offset 
				addLineOffset(fileLength);
				// but do not increment line count
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
						// nothing we can do...
						e.printStackTrace();
					}
			}
			monitor.done();
			setReady(lineCount, lineOffsets);
		}

		private void parseBuffer(char[] buf, int readChars, long bufOffset) {
			for (int i = 0 ; i < readChars ; i++) {
				if (buf[i] == '\n') {
					// line terminated, add line offset
					addLineOffset(bufOffset + i + 1);
					lineCount++;
					// set current offset to next line (after \n)
				}
			}
		}

		private void addLineOffset(long offset) {
			// ensure there is enough space
			// TODO optimise for performance
			if (lineCount >= lineOffsets.length) {
				long[] newLineOffsets = new long[lineOffsets.length + 1000000];
				System.arraycopy(lineOffsets, 0, newLineOffsets, 0, lineOffsets.length);
				lineOffsets = newLineOffsets;
			}
			// insert offset
			lineOffsets[lineCount] = offset;
			//debug only
			//System.out.println("LOFF@" + lineCount + "=" + offset);
		}
	}

	public interface IFindMonitor {
		/**
		 * 
		 * @param start start offset of the occurrence
		 * @param string the text that was found.
		 * @return true if search should be repeated
		 */
		public boolean onFind(long start, String string);
	}

	public long findString(String string, long start, boolean caseSensitive, boolean forward, IProgressMonitor monitor) {
		int count = getLineCount();
		if (monitor != null)
			monitor.beginTask("searching: " + string, count);
		if (!isReady()) {
			if (monitor != null)
				monitor.done();
			return -1;
		}
		if (string == null || string.length() == 0) {
			if (monitor != null)
				monitor.done();
			return start;
		}
		long pos = -1;
		int lineIndex = getLineIndex(start);
		int inc = forward ? 1 : -1;
lineloop:
		for (; lineIndex < count ; lineIndex += inc) {
			String line = getLine(lineIndex);
			int i;
			if (forward)
				i = line.indexOf(string);
			else
				i = line.lastIndexOf(string);
			while (i >= 0) {
				// string found on this line
				LineOffsets offsets = new LineOffsets();
				getOffsetsForLine(lineIndex, offsets);
				// check that the found string is actually:
				// * after the start when searching forward
				// * before the start when searching backwards
				if (( forward && offsets.start + i >= start) ||
					(!forward && offsets.start + i <= start)){
					pos = offsets.start + i;
					if (monitor != null && monitor instanceof IFindMonitor)
						((IFindMonitor)monitor).onFind(pos, string);
					break lineloop;
				} else {
					if (forward)
						i = line.indexOf(string, i + 1);
					else
						i = line.lastIndexOf(string, i - 1);
				}
			}
		}
		if (monitor != null)
			monitor.done();
		return pos ; // not found
	}
}

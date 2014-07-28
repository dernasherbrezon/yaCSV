package com.st.yacsv;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

public class CSVWriter implements Flushable, Closeable {

	private final BufferedWriter out;
	private final char separator;
	private final char quotechar;
	private final char escapechar;
	private final String lineEnd;
	private final boolean applyQuotesAll;

	public CSVWriter(Writer writer) {
		this(writer, CSVDefaults.SEPARATOR);
	}

	public CSVWriter(Writer writer, char separator) {
		this(writer, separator, CSVDefaults.QUOTE_CHARACTER);
	}

	public CSVWriter(Writer writer, char separator, char quotechar) {
		this(writer, separator, quotechar, CSVDefaults.ESCAPE_CHARACTER);
	}

	public CSVWriter(Writer writer, char separator, char quotechar, char escapechar) {
		this(writer, separator, quotechar, escapechar, CSVDefaults.LINE_END);
	}

	public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
		this(writer, separator, quotechar, escapechar, lineEnd, CSVDefaults.APPLY_QUOTES_ALL);
	}

	public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd, boolean applyQuotesAll) {
		if (writer instanceof BufferedWriter) {
			this.out = (BufferedWriter) writer;
		} else {
			this.out = new BufferedWriter(writer);
		}
		this.separator = separator;
		this.quotechar = quotechar;
		this.escapechar = escapechar;
		this.lineEnd = lineEnd;
		this.applyQuotesAll = applyQuotesAll;
	}

	/**
	 * Writes the entire list to a CSV file. The list is assumed to be a String[]
	 * 
	 * @param allLines
	 *            a List of String[], with each String[] representing a line of the file.
	 */
	public void writeAll(Collection<String[]> allLines) throws IOException {
		for (String[] line : allLines) {
			writeNext(line);
		}
	}

	/**
	 * Writes the next line to the file.
	 * 
	 * @param nextLine
	 *            a string array with each comma-separated element as a separate entry.
	 */
	public void writeNext(String... nextLine) throws IOException {

		if (nextLine == null) {
			return;
		}

		for (int i = 0; i < nextLine.length; i++) {

			if (i != 0) {
				out.append(separator);
			}

			String nextElement = nextLine[i];

			if (nextElement == null) {
				continue;
			}

			Boolean stringContainsSpecialCharacters = stringContainsSpecialCharacters(nextElement);

			if ((applyQuotesAll || stringContainsSpecialCharacters) && quotechar != CSVDefaults.NO_QUOTE_CHARACTER) {
				out.append(quotechar);
			}

			if (stringContainsSpecialCharacters) {
				processLine(out, nextElement);
			} else {
				out.append(nextElement);
			}

			if ((applyQuotesAll || stringContainsSpecialCharacters) && quotechar != CSVDefaults.NO_QUOTE_CHARACTER) {
				out.append(quotechar);
			}
		}
		out.append(lineEnd);
	}

	private boolean stringContainsSpecialCharacters(String line) {
		return line.indexOf(quotechar) != -1 || line.indexOf(escapechar) != -1 || line.indexOf(separator) != -1 || line.indexOf("\n") != -1 || line.indexOf("\r") != -1;
	}

	protected void processLine(Appendable sb, String nextElement) throws IOException {
		for (int j = 0; j < nextElement.length(); j++) {
			char nextChar = nextElement.charAt(j);
			if (escapechar != CSVDefaults.NO_ESCAPE_CHARACTER && nextChar == quotechar) {
				sb.append(escapechar).append(nextChar);
			} else if (escapechar != CSVDefaults.NO_ESCAPE_CHARACTER && nextChar == escapechar) {
				sb.append(escapechar).append(nextChar);
			} else {
				sb.append(nextChar);
			}
		}
	}

	/**
	 * Flush underlying stream to writer.
	 * 
	 * @throws IOException
	 *             if bad things happen
	 */
	@Override
	public void flush() throws IOException {
		out.flush();
	}

	/**
	 * Close the underlying stream writer flushing any buffered content.
	 * 
	 * @throws IOException
	 *             if bad things happen
	 */
	@Override
	public void close() throws IOException {
		flush();
		out.close();
	}

	public void flushQuietly() {
		try {
			flush();
		} catch (IOException e) {
			// catch exception and ignore.
		}
	}

}

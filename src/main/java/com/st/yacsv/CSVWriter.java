package com.st.yacsv;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

public class CSVWriter implements Flushable, Closeable {

	private BufferedWriter out;

	private char separator;

	private char quotechar;

	private char escapechar;

	private String lineEnd;

	/**
	 * Constructs CSVWriter using a comma for the separator.
	 * 
	 * @param writer
	 *            the writer to an underlying CSV source.
	 */
	public CSVWriter(Writer writer) {
		this(writer, CSVDefaults.DEFAULT_SEPARATOR);
	}

	/**
	 * Constructs CSVWriter with supplied separator.
	 * 
	 * @param writer
	 *            the writer to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries.
	 */
	public CSVWriter(Writer writer, char separator) {
		this(writer, separator, CSVDefaults.DEFAULT_QUOTE_CHARACTER);
	}

	/**
	 * Constructs CSVWriter with supplied separator and quote char.
	 * 
	 * @param writer
	 *            the writer to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 */
	public CSVWriter(Writer writer, char separator, char quotechar) {
		this(writer, separator, quotechar, CSVDefaults.DEFAULT_ESCAPE_CHARACTER);
	}

	/**
	 * Constructs CSVWriter with supplied separator and quote char.
	 * 
	 * @param writer
	 *            the writer to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escapechar
	 *            the character to use for escaping quotechars or escapechars
	 */
	public CSVWriter(Writer writer, char separator, char quotechar, char escapechar) {
		this(writer, separator, quotechar, escapechar, CSVDefaults.DEFAULT_LINE_END);
	}

	/**
	 * Constructs CSVWriter with supplied separator and quote char.
	 * 
	 * @param writer
	 *            the writer to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param lineEnd
	 *            the line feed terminator to use
	 */
	public CSVWriter(Writer writer, char separator, char quotechar, String lineEnd) {
		this(writer, separator, quotechar, CSVDefaults.DEFAULT_ESCAPE_CHARACTER, lineEnd);
	}

	/**
	 * Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
	 * 
	 * @param writer
	 *            the writer to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escapechar
	 *            the character to use for escaping quotechars or escapechars
	 * @param lineEnd
	 *            the line feed terminator to use
	 */
	public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
		if (writer instanceof BufferedWriter) {
			this.out = (BufferedWriter) writer;
		} else {
			this.out = new BufferedWriter(writer);
		}
		this.separator = separator;
		this.quotechar = quotechar;
		this.escapechar = escapechar;
		this.lineEnd = lineEnd;
	}

	/**
	 * Writes the entire list to a CSV file. The list is assumed to be a String[]
	 * 
	 * @param allLines
	 *            a List of String[], with each String[] representing a line of the file.
	 * @param applyQuotesToAll
	 *            true if all values are to be quoted. false if quotes only to be applied to values which contain the separator, escape, quote or new line characters.
	 */
	public void writeAll(Collection<String[]> allLines, boolean applyQuotesToAll) throws IOException {
		for (String[] line : allLines) {
			writeNext(line, applyQuotesToAll);
		}
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
	 * @param applyQuotesToAll
	 *            true if all values are to be quoted. false applies quotes only to values which contain the separator, escape, quote or new line characters.
	 */
	public void writeNext(String[] nextLine, boolean applyQuotesToAll) throws IOException {

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

			if ((applyQuotesToAll || stringContainsSpecialCharacters) && quotechar != CSVDefaults.NO_QUOTE_CHARACTER) {
				out.append(quotechar);
			}

			if (stringContainsSpecialCharacters) {
				processLine(out, nextElement);
			} else {
				out.append(nextElement);
			}

			if ((applyQuotesToAll || stringContainsSpecialCharacters) && quotechar != CSVDefaults.NO_QUOTE_CHARACTER) {
				out.append(quotechar);
			}
		}
		out.append(lineEnd);
	}

	/**
	 * Writes the next line to the file.
	 * 
	 * @param nextLine
	 *            a string array with each comma-separated element as a separate entry.
	 */
	public void writeNext(String... nextLine) throws IOException {
		writeNext(nextLine, true);
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

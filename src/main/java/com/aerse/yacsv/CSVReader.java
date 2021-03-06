package com.aerse.yacsv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class CSVReader implements Closeable, Iterable<String[]>, Iterator<String[]> {

	private BufferedReader br;

	private boolean hasNext = true;

	private String[] nextLine;

	private CSVParser parser;

	private int skipLines;

	private boolean linesSkiped;

	/**
	 * The default line to start reading.
	 */
	public static final int DEFAULT_SKIP_LINES = 0;

	/**
	 * Constructs CSVReader using a comma for the separator.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 */
	public CSVReader(Reader reader) {
		this(reader, CSVDefaults.SEPARATOR, CSVDefaults.QUOTE_CHARACTER, CSVDefaults.ESCAPE_CHARACTER);
	}

	/**
	 * Constructs CSVReader with supplied separator.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries.
	 */
	public CSVReader(Reader reader, char separator) {
		this(reader, separator, CSVDefaults.QUOTE_CHARACTER, CSVDefaults.ESCAPE_CHARACTER);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 */
	public CSVReader(Reader reader, char separator, char quotechar) {
		this(reader, separator, quotechar, CSVDefaults.ESCAPE_CHARACTER, DEFAULT_SKIP_LINES, CSVDefaults.STRICT_QUOTES);
	}

	/**
	 * Constructs CSVReader with supplied separator, quote char and quote handling behavior.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param strictQuotes
	 *            sets if characters outside the quotes are ignored
	 */
	public CSVReader(Reader reader, char separator, char quotechar, boolean strictQuotes) {
		this(reader, separator, quotechar, CSVDefaults.ESCAPE_CHARACTER, DEFAULT_SKIP_LINES, strictQuotes);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 */

	public CSVReader(Reader reader, char separator, char quotechar, char escape) {
		this(reader, separator, quotechar, escape, DEFAULT_SKIP_LINES, CSVDefaults.STRICT_QUOTES);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param line
	 *            the line number to skip for start reading
	 */
	public CSVReader(Reader reader, char separator, char quotechar, int line) {
		this(reader, separator, quotechar, CSVDefaults.ESCAPE_CHARACTER, line, CSVDefaults.STRICT_QUOTES);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 * @param line
	 *            the line number to skip for start reading
	 */
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line) {
		this(reader, separator, quotechar, escape, line, CSVDefaults.STRICT_QUOTES);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 * @param line
	 *            the line number to skip for start reading
	 * @param strictQuotes
	 *            sets if characters outside the quotes are ignored
	 */
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line, boolean strictQuotes) {
		this(reader, separator, quotechar, escape, line, strictQuotes, CSVDefaults.IGNORE_LEADING_WHITESPACE);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 * @param line
	 *            the line number to skip for start reading
	 * @param strictQuotes
	 *            sets if characters outside the quotes are ignored
	 * @param ignoreLeadingWhiteSpace
	 *            it true, parser should ignore white space before a quote in a field
	 */
	public CSVReader(Reader reader, char separator, char quotechar, char escape, int line, boolean strictQuotes, boolean ignoreLeadingWhiteSpace) {
		this(reader, line, new CSVParser(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace));
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param reader
	 *            the reader to an underlying CSV source.
	 * @param line
	 *            the line number to skip for start reading
	 * @param csvParser
	 *            the parser to use to parse input
	 */
	public CSVReader(Reader reader, int line, CSVParser csvParser) {
		if (reader instanceof BufferedReader) {
			this.br = (BufferedReader) reader;
		} else {
			this.br = new BufferedReader(reader);
		}
		this.skipLines = line;
		this.parser = csvParser;
	}

	private String[] readNext() throws IOException {

		String[] result = null;
		do {
			String nextLine = getNextLine();
			if (!hasNext) {
				return result; // should throw if still pending?
			}
			String[] r = parser.parseLineMulti(nextLine);
			if (r.length > 0) {
				if (result == null) {
					result = r;
				} else {
					String[] t = new String[result.length + r.length];
					System.arraycopy(result, 0, t, 0, result.length);
					System.arraycopy(r, 0, t, result.length, r.length);
					result = t;
				}
			}
		} while (parser.isPending());
		return result;
	}

	/**
	 * Reads the next line from the file.
	 * 
	 * @return the next line from the file without trailing newline
	 * @throws IOException
	 *             if bad things happen during the read
	 */
	private String getNextLine() throws IOException {
		if (!this.linesSkiped) {
			for (int i = 0; i < skipLines; i++) {
				br.readLine();
			}
			this.linesSkiped = true;
		}
		String nextLine = br.readLine();
		if (nextLine == null) {
			hasNext = false;
		}
		return hasNext ? nextLine : null;
	}

	/**
	 * Closes the underlying reader.
	 * 
	 * @throws IOException
	 *             if the close fails
	 */
	@Override
	public void close() throws IOException {
		br.close();
	}

	@Override
	public Iterator<String[]> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		try {
			nextLine = readNext();
		} catch (IOException e) {
			return false;
		}
		return nextLine != null;
	}

	@Override
	public String[] next() {
		return nextLine;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This is a read only iterator.");
	}

}

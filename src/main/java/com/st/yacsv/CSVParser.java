package com.st.yacsv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CSVParser {

	final char separator;

	final char quotechar;

	final char escape;

	final boolean strictQuotes;

	private String pending;
	private boolean inField = false;

	final boolean ignoreLeadingWhiteSpace;

	final boolean ignoreQuotations;

	private static final int INITIAL_READ_SIZE = 128;

	/**
	 * Constructs CSVParser using a comma for the separator.
	 */
	CSVParser() {
		this(CSVDefaults.DEFAULT_SEPARATOR, CSVDefaults.DEFAULT_QUOTE_CHARACTER, CSVDefaults.DEFAULT_ESCAPE_CHARACTER);
	}

	/**
	 * Constructs CSVParser with supplied separator.
	 * 
	 * @param separator
	 *            the delimiter to use for separating entries.
	 */
	CSVParser(char separator) {
		this(separator, CSVDefaults.DEFAULT_QUOTE_CHARACTER, CSVDefaults.DEFAULT_ESCAPE_CHARACTER);
	}

	/**
	 * Constructs CSVParser with supplied separator and quote char.
	 * 
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 */
	CSVParser(char separator, char quotechar) {
		this(separator, quotechar, CSVDefaults.DEFAULT_ESCAPE_CHARACTER);
	}

	/**
	 * Constructs CSVReader with supplied separator and quote char.
	 * 
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 */
	CSVParser(char separator, char quotechar, char escape) {
		this(separator, quotechar, escape, CSVDefaults.DEFAULT_STRICT_QUOTES);
	}

	/**
	 * Constructs CSVParser with supplied separator and quote char. Allows setting the "strict quotes" flag
	 * 
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 * @param strictQuotes
	 *            if true, characters outside the quotes are ignored
	 */
	CSVParser(char separator, char quotechar, char escape, boolean strictQuotes) {
		this(separator, quotechar, escape, strictQuotes, CSVDefaults.DEFAULT_IGNORE_LEADING_WHITESPACE);
	}

	/**
	 * Constructs CSVParser with supplied separator and quote char. Allows setting the "strict quotes" and "ignore leading whitespace" flags
	 * 
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 * @param strictQuotes
	 *            if true, characters outside the quotes are ignored
	 * @param ignoreLeadingWhiteSpace
	 *            if true, white space in front of a quote in a field is ignored
	 */
	CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace) {
		this(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace, CSVDefaults.DEFAULT_IGNORE_QUOTATIONS);
	}

	/**
	 * Constructs CSVParser with supplied separator and quote char. Allows setting the "strict quotes" and "ignore leading whitespace" flags
	 * 
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escape
	 *            the character to use for escaping a separator or quote
	 * @param strictQuotes
	 *            if true, characters outside the quotes are ignored
	 * @param ignoreLeadingWhiteSpace
	 *            if true, white space in front of a quote in a field is ignored
	 */
	CSVParser(char separator, char quotechar, char escape, boolean strictQuotes, boolean ignoreLeadingWhiteSpace, boolean ignoreQuotations) {
		if (separator == CSVDefaults.NULL_CHARACTER) {
			throw new UnsupportedOperationException("The separator character must be defined!");
		}
		if (anyCharactersAreTheSame(separator, quotechar, escape)) {
			throw new UnsupportedOperationException("The separator, quote, and escape characters must be different! separator: " + separator + " quotechar: " + quotechar + " escape: " + escape);
		}
		this.separator = separator;
		this.quotechar = quotechar;
		this.escape = escape;
		this.strictQuotes = strictQuotes;
		this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
		this.ignoreQuotations = ignoreQuotations;
	}

	private static boolean anyCharactersAreTheSame(char separator, char quotechar, char escape) {
		return isSameCharacter(separator, quotechar) || isSameCharacter(separator, escape) || isSameCharacter(quotechar, escape);
	}

	private static boolean isSameCharacter(char c1, char c2) {
		return c1 != CSVDefaults.NULL_CHARACTER && c1 == c2;
	}

	/**
	 * @return true if something was left over from last call(s)
	 */
	boolean isPending() {
		return pending != null;
	}

	String[] parseLineMulti(String nextLine) throws IOException {
		return parseLine(nextLine, true);
	}


	/**
	 * Parses an incoming String and returns an array of elements.
	 * 
	 * @param nextLine
	 *            the string to parse
	 * @param multi
	 * @return the comma-tokenized list of elements, or null if nextLine is null
	 * @throws IOException
	 *             if bad things happen during the read
	 */
	private String[] parseLine(String nextLine, boolean multi) throws IOException {

		if (!multi && pending != null) {
			pending = null;
		}

		if (nextLine == null) {
			if (pending != null) {
				String s = pending;
				pending = null;
				return new String[] { s };
			} else {
				return null;
			}
		}

		List<String> tokensOnThisLine = new ArrayList<String>();
		StringBuilder sb = new StringBuilder(INITIAL_READ_SIZE);
		boolean inQuotes = false;
		if (pending != null) {
			sb.append(pending);
			pending = null;
			inQuotes = !this.ignoreQuotations;// true;
		}
		for (int i = 0; i < nextLine.length(); i++) {

			char c = nextLine.charAt(i);
			if (c == this.escape) {
				if (isNextCharacterEscapable(nextLine, (inQuotes && !ignoreQuotations) || inField, i)) {
					sb.append(nextLine.charAt(i + 1));
					i++;
				}
			} else if (c == quotechar) {
				if (isNextCharacterEscapedQuote(nextLine, (inQuotes && !ignoreQuotations) || inField, i)) {
					sb.append(nextLine.charAt(i + 1));
					i++;
				} else {
					inQuotes = !inQuotes;

					// the tricky case of an embedded quote in the middle: a,bc"d"ef,g
					if (!strictQuotes) {
						if (i > 2 // not on the beginning of the line
								&& nextLine.charAt(i - 1) != this.separator // not at the beginning of an escape sequence
								&& nextLine.length() > (i + 1) && nextLine.charAt(i + 1) != this.separator // not at the end of an escape sequence
						) {

							if (ignoreLeadingWhiteSpace && sb.length() > 0 && isAllWhiteSpace(sb)) {
								sb = new StringBuilder(INITIAL_READ_SIZE); // discard white space leading up to quote
							} else {
								sb.append(c);
							}

						}
					}
				}
				inField = !inField;
			} else if (c == separator && !(inQuotes && !ignoreQuotations)) {
				tokensOnThisLine.add(sb.toString());
				sb = new StringBuilder(INITIAL_READ_SIZE); // start work on next token
				inField = false;
			} else {
				if (!strictQuotes || (inQuotes && !ignoreQuotations)) {
					sb.append(c);
					inField = true;
				}
			}
		}
		// line is done - check status
		if ((inQuotes && !ignoreQuotations)) {
			if (multi) {
				// continuing a quoted section, re-append newline
				sb.append("\n");
				pending = sb.toString();
				sb = null; // this partial content is not to be added to field list yet
			} else {
				throw new IOException("Un-terminated quoted field at end of CSV line");
			}
		}
		if (sb != null) {
			tokensOnThisLine.add(sb.toString());
		}
		return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);

	}

	/**
	 * precondition: the current character is a quote or an escape
	 * 
	 * @param nextLine
	 *            the current line
	 * @param inQuotes
	 *            true if the current context is quoted
	 * @param i
	 *            current index in line
	 * @return true if the following character is a quote
	 */
	private boolean isNextCharacterEscapedQuote(String nextLine, boolean inQuotes, int i) {
		return inQuotes // we are in quotes, therefore there can be escaped quotes in here.
				&& nextLine.length() > (i + 1) // there is indeed another character to check.
				&& nextLine.charAt(i + 1) == quotechar;
	}

	/**
	 * precondition: the current character is an escape
	 * 
	 * @param nextLine
	 *            the current line
	 * @param inQuotes
	 *            true if the current context is quoted
	 * @param i
	 *            current index in line
	 * @return true if the following character is a quote
	 */
	protected boolean isNextCharacterEscapable(String nextLine, boolean inQuotes, int i) {
		return inQuotes // we are in quotes, therefore there can be escaped quotes in here.
				&& nextLine.length() > (i + 1) // there is indeed another character to check.
				&& (nextLine.charAt(i + 1) == quotechar || nextLine.charAt(i + 1) == this.escape);
	}

	/**
	 * precondition: sb.length() > 0
	 * 
	 * @param sb
	 *            A sequence of characters to examine
	 * @return true if every character in the sequence is whitespace
	 */
	protected boolean isAllWhiteSpace(CharSequence sb) {
		boolean result = true;
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);

			if (!Character.isWhitespace(c)) {
				return false;
			}
		}
		return result;
	}
}

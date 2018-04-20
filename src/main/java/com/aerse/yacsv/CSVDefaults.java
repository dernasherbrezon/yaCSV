package com.aerse.yacsv;

public class CSVDefaults {

	public static final boolean APPLY_QUOTES_ALL = true;
	
	/**
	 * The character used for escaping quotes.
	 */
	public static final char ESCAPE_CHARACTER = '\\';

	/**
	 * The default separator to use if none is supplied to the constructor.
	 */
	public static final char SEPARATOR = ',';

	/**
	 * The default quote character to use if none is supplied to the constructor.
	 */
	public static final char QUOTE_CHARACTER = '"';

	/**
	 * The quote constant to use when you wish to suppress all quoting.
	 */
	public static final char NO_QUOTE_CHARACTER = '\u0000';

	/**
	 * The escape constant to use when you wish to suppress all escaping.
	 */
	public static final char NO_ESCAPE_CHARACTER = '\u0000';

	/**
	 * Default line terminator uses platform encoding.
	 */
	public static final String LINE_END = "\n";
	
	/**
	 * The default strict quote behavior to use if none is supplied to the constructor
	 */
	public static final boolean STRICT_QUOTES = false;

	/**
	 * The default leading whitespace behavior to use if none is supplied to the constructor
	 */
	public static final boolean IGNORE_LEADING_WHITESPACE = true;

	/**
	 * I.E. if the quote character is set to null then there is no quote character.
	 */
	public static final boolean IGNORE_QUOTATIONS = false;

	/**
	 * This is the "null" character - if a value is set to this then it is ignored.
	 */
	static final char NULL_CHARACTER = '\0';
}

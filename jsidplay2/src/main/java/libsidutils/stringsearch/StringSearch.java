/* 
 * StringSearch.java
 * 
 * Created on 14.06.2003.
 *
 * eaio: StringSearch - high-performance pattern matching algorithms in Java
 * Copyright (c) 2003, 2004 Johann Burkard (jb@eaio.com) http://eaio.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */
package libsidutils.stringsearch;

/**
 * The base class for String searching implementations. String searching
 * implementations do not maintain state and are thread safe - one instance can
 * be used by as many threads as required.
 * <p>
 * Most pattern-matching algorithms pre-process the pattern to search for in
 * some way. Subclasses of StringSearch allow retrieving the pre-processed
 * pattern to save the time required to build up character tables.
 * <p>
 * Some of the Objects returned from {@link #processBytes(byte[])},
 * {@link #processChars(char[])}, {@link #processString(String)} might implement
 * the {@link java.io.Serializable} interface and enable you to serialize
 * pre-processed Objects to disk, see concrete implementations for details.
 * 
 * @author <a href="mailto:jb@eaio.com">Johann Burkard</a>
 * @version 1.2
 */
abstract class StringSearch {
	/**
	 * Pre-processes a <code>byte</code> array.
	 * 
	 * @param pattern
	 *            the <code>byte</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return an Object
	 */
	public abstract Object processBytes(byte[] pattern);

	/**
	 * Pre-processes a <code>char</code> array
	 * 
	 * @param pattern
	 *            a <code>char</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return an Object
	 */
	public abstract Object processChars(char[] pattern);

	/* Byte searching methods */

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the <code>byte</code> array containing the text, may not be
	 *            <code>null</code>
	 * @param pattern
	 *            the <code>byte</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchBytes(byte[], int, int, byte[], Object)
	 */
	public final int searchBytes(byte[] text, byte[] pattern) {
		return searchBytes(text, 0, text.length, pattern, processBytes(pattern));
	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the <code>byte</code> array containing the text, may not be
	 *            <code>null</code>
	 * @param pattern
	 *            the pattern to search for, may not be <code>null</code>
	 * @param processed
	 *            an Object as returned from {@link #processBytes(byte[])}, may
	 *            not be <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchBytes(byte[], int, int, byte[], Object)
	 */
	public final int searchBytes(byte[] text, byte[] pattern, Object processed) {
		return searchBytes(text, 0, text.length, pattern, processed);
	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the <code>byte</code> array containing the text, may not be
	 *            <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param pattern
	 *            the <code>byte</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return int the position in the text or -1 if the pattern was not found
	 * @see #searchBytes(byte[], int, int, byte[], Object)
	 */
	public final int searchBytes(byte[] text, int textStart, byte[] pattern) {
		return searchBytes(text, textStart, text.length, pattern,
				processBytes(pattern));
	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the <code>byte</code> array containing the text, may not be
	 *            <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param pattern
	 *            the pattern to search for, may not be <code>null</code>
	 * @param processed
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchBytes(byte[], int, int, byte[], Object)
	 */
	public final int searchBytes(byte[] text, int textStart, byte[] pattern,
			Object processed) {

		return searchBytes(text, textStart, text.length, pattern, processed);

	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            text the <code>byte</code> array containing the text, may not
	 *            be <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param textEnd
	 *            at which position in the text comparing should stop
	 * @param pattern
	 *            the <code>byte</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchBytes(byte[], int, int, byte[], Object)
	 */
	public final int searchBytes(byte[] text, int textStart, int textEnd,
			byte[] pattern) {

		return searchBytes(text, textStart, textEnd, pattern,
				processBytes(pattern));

	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            text the <code>byte</code> array containing the text, may not
	 *            be <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param textEnd
	 *            at which position in the text comparing should stop
	 * @param pattern
	 *            the pattern to search for, may not be <code>null</code>
	 * @param processed
	 *            an Object as returned from {@link #processBytes(byte[])}, may
	 *            not be <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #processBytes(byte[])
	 */
	public abstract int searchBytes(byte[] text, int textStart, int textEnd,
			byte[] pattern, Object processed);

	/* Char searching methods */

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the character array containing the text, may not be
	 *            <code>null</code>
	 * @param pattern
	 *            the <code>char</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchChars(char[], int, int, char[], Object)
	 */
	public final int searchChars(char[] text, char[] pattern) {
		return searchChars(text, 0, text.length, pattern, processChars(pattern));
	}

	/**
	 * Returns the index of the pattern in the text using the pre-processed
	 * Object. Returns -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the character array containing the text, may not be
	 *            <code>null</code>
	 * @param pattern
	 *            the <code>char</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @param processed
	 *            an Object as returned from {@link #processChars(char[])} or
	 *            {@link #processString(String)}, may not be <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchChars(char[], int, int, char[], Object)
	 */
	public final int searchChars(char[] text, char[] pattern, Object processed) {
		return searchChars(text, 0, text.length, pattern, processed);
	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the character array containing the text, may not be
	 *            <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param pattern
	 *            the <code>char</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchChars(char[], int, int, char[], Object)
	 */
	public final int searchChars(char[] text, int textStart, char[] pattern) {
		return searchChars(text, textStart, text.length, pattern,
				processChars(pattern));
	}

	/**
	 * Returns the index of the pattern in the text using the pre-processed
	 * Object. Returns -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the String containing the text, may not be <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param pattern
	 *            the <code>char</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @param processed
	 *            an Object as returned from {@link #processChars(char[])} or
	 *            {@link #processString(String)}, may not be <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchChars(char[], int, int, char[], Object)
	 */
	public final int searchChars(char[] text, int textStart, char[] pattern,
			Object processed) {

		return searchChars(text, textStart, text.length, pattern, processed);

	}

	/**
	 * Returns the position in the text at which the pattern was found. Returns
	 * -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the character array containing the text, may not be
	 *            <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param textEnd
	 *            at which position in the text comparing should stop
	 * @param pattern
	 *            the <code>char</code> array containing the pattern, may not be
	 *            <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 * @see #searchChars(char[], int, int, char[], Object)
	 */
	public final int searchChars(char[] text, int textStart, int textEnd,
			char[] pattern) {

		return searchChars(text, textStart, textEnd, pattern,
				processChars(pattern));

	}

	/**
	 * Returns the index of the pattern in the text using the pre-processed
	 * Object. Returns -1 if the pattern was not found.
	 * 
	 * @param text
	 *            the String containing the text, may not be <code>null</code>
	 * @param textStart
	 *            at which position in the text the comparing should start
	 * @param textEnd
	 *            at which position in the text comparing should stop
	 * @param pattern
	 *            the pattern to search for, may not be <code>null</code>
	 * @param processed
	 *            an Object as returned from {@link #processChars(char[])} or
	 *            {@link #processString(String)}, may not be <code>null</code>
	 * @return the position in the text or -1 if the pattern was not found
	 */
	public abstract int searchChars(char[] text, int textStart, int textEnd,
			char[] pattern, Object processed);

	/* String searching methods */

	/**
	 * Returns if the Object's class name matches this Object's class name.
	 * 
	 * @param obj
	 *            the other Object
	 * @return if the Object is equal to this Object
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		return getClass().getName().equals(obj.getClass().getName());
	}

	/**
	 * Returns the hashCode of the Object's Class because all instances of this
	 * Class are equal.
	 * 
	 * @return an int
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		return getClass().getName().hashCode();
	}

	/**
	 * Returns a String representation of this. Simply returns the name of the
	 * Class.
	 * 
	 * @return a String
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return toStringBuffer(null).toString();
	}

	/**
	 * Appends a String representation of this to the given {@link StringBuffer}
	 * or creates a new one if none is given. This method is not
	 * <code>final</code> because subclasses might want a different String
	 * format.
	 * 
	 * @param in
	 *            the StringBuffer to append to, may be <code>null</code>
	 * @return a StringBuffer
	 */
	public StringBuffer toStringBuffer(StringBuffer in) {
		if (in == null) {
			in = new StringBuffer();
		}
		in.append("{ ");
		int idx = getClass().getName().lastIndexOf(".");
		if (idx > -1) {
			in.append(getClass().getName().substring(++idx));
		} else {
			in.append(getClass().getName());
		}
		in.append(" }");
		return in;
	}

	/* Utility methods */

	/**
	 * Returns a {@link CharIntMap} of the extent of the given pattern, using no
	 * default value.
	 * 
	 * @param pattern
	 *            the pattern
	 * @return a CharIntMap
	 * @see CharIntMap#CharIntMap(int, char)
	 */
	protected CharIntMap createCharIntMap(char[] pattern) {
		return createCharIntMap(pattern, 0);
	}

	/**
	 * Returns a {@link CharIntMap} of the extent of the given pattern, using
	 * the specified default value.
	 * 
	 * @param pattern
	 *            the pattern
	 * @param defaultValue
	 *            the default value
	 * @return a CharIntMap
	 * @see CharIntMap#CharIntMap(int, char, int)
	 */
	protected CharIntMap createCharIntMap(char[] pattern, int defaultValue) {
		char min = Character.MAX_VALUE;
		char max = Character.MIN_VALUE;
		for (int i = 0; i < pattern.length; i++) {
			max = max > pattern[i] ? max : pattern[i];
			min = min < pattern[i] ? min : pattern[i];
		}
		return new CharIntMap(max - min + 1, min, defaultValue);
	}

	/**
	 * Converts the given <code>byte</code> to an <code>int</code>.
	 * 
	 * @param idx
	 *            the byte
	 * @return an int
	 */
	protected final int index(byte idx) {
		return (idx < 0) ? 256 + idx : idx;
	}

	/* Utility methods */
}

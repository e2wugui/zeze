using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
	// contributors: lizongbo: proposed special treatment of array parameter values
	// Joern Huxhorn: pointed out double[] omission, suggested deep array copy
	/**
	 * Formats messages according to very simple substitution rules. Substitutions
	 * can be made 1, 2 or more arguments.
	 * 
	 * <p>
	 * For example,
	 * 
	 * <pre>
	 * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;)
	 * </pre>
	 * 
	 * will return the string "Hi there.".
	 * <p>
	 * The {} pair is called the <em>formatting anchor</em>. It serves to designate
	 * the location where arguments need to be substituted within the message
	 * pattern.
	 * <p>
	 * In case your message contains the '{' or the '}' character, you do not have
	 * to do anything special unless the '}' character immediately follows '{'. For
	 * example,
	 * 
	 * <pre>
	 * MessageFormatter.format(&quot;Set {1,2,3} is not equal to {}.&quot;, &quot;1,2&quot;);
	 * </pre>
	 * 
	 * will return the string "Set {1,2,3} is not equal to 1,2.".
	 * 
	 * <p>
	 * If for whatever reason you need to place the string "{}" in the message
	 * without its <em>formatting anchor</em> meaning, then you need to escape the
	 * '{' character with '\', that is the backslash character. Only the '{'
	 * character should be escaped. There is no need to escape the '}' character.
	 * For example,
	 * 
	 * <pre>
	 * MessageFormatter.format(&quot;Set \\{} is not equal to {}.&quot;, &quot;1,2&quot;);
	 * </pre>
	 * 
	 * will return the string "Set {} is not equal to 1,2.".
	 * 
	 * <p>
	 * The escaping behavior just described can be overridden by escaping the escape
	 * character '\'. Calling
	 * 
	 * <pre>
	 * MessageFormatter.format(&quot;File name is C:\\\\{}.&quot;, &quot;file.zip&quot;);
	 * </pre>
	 * 
	 * will return the string "File name is C:\file.zip".
	 * 
	 * <p>
	 * The formatting conventions are different than those of {@link MessageFormat}
	 * which ships with the Java platform. This is justified by the fact that
	 * SLF4J's implementation is 10 times faster than that of {@link MessageFormat}.
	 * This local performance difference is both measurable and significant in the
	 * larger context of the complete logging processing chain.
	 * 
	 * <p>
	 * See also {@link #format(String, Object)},
	 * {@link #format(String, Object, Object)} and
	 * {@link #arrayFormat(String, Object[])} methods for more details.
	 * 
	 * @author Ceki G&uuml;lc&uuml;
	 * @author Joern Huxhorn
	 */
	public class MessageFormatter
	{
		const char DELIM_START = '{';
		char DELIM_STOP = '}';
		const string DELIM_STR = "{}";

		private const char ESCAPE_CHAR = '\\';

		/**
		 * Performs single argument substitution for the 'messagePattern' passed as
		 * parameter.
		 * <p>
		 * For example,
		 * 
		 * <pre>
		 * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
		 * </pre>
		 * 
		 * will return the string "Hi there.".
		 * <p>
		 * 
		 * @param messagePattern The message pattern which will be parsed and formatted
		 * @param argument       The argument to be substituted in place of the
		 *                       formatting anchor
		 * @return The formatted message
		 */
		public static FormattingTuple format(string messagePattern, Object arg)
		{
			return arrayFormat(messagePattern, new Object[] { arg });
		}

		/**
		 * 
		 * Performs a two argument substitution for the 'messagePattern' passed as
		 * parameter.
		 * <p>
		 * For example,
		 * 
		 * <pre>
		 * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
		 * </pre>
		 * 
		 * will return the string "Hi Alice. My name is Bob.".
		 * 
		 * @param messagePattern The message pattern which will be parsed and formatted
		 * @param arg1           The argument to be substituted in place of the first
		 *                       formatting anchor
		 * @param arg2           The argument to be substituted in place of the second
		 *                       formatting anchor
		 * @return The formatted message
		 */
		public static FormattingTuple format(string messagePattern, Object arg1, Object arg2)
		{
			return arrayFormat(messagePattern, new Object[] { arg1, arg2 });
		}

		static Exception getThrowableCandidate(Object[] argArray)
		{
			if (argArray == null || argArray.Length == 0)
			{
				return null;
			}

			Object lastEntry = argArray[^1];
			if (lastEntry is Exception)
			{
				return (Exception)lastEntry;
			}
			return null;
		}

		/**
		 * Same principle as the {@link #format(String, Object)} and
		 * {@link #format(String, Object, Object)} methods except that any number of
		 * arguments can be passed in an array.
		 * 
		 * @param messagePattern The message pattern which will be parsed and formatted
		 * @param argArray       An array of arguments to be substituted in place of
		 *                       formatting anchors
		 * @return The formatted message
		 */
		public static FormattingTuple arrayFormat(string messagePattern, params Object[] argArray)
		{

			Exception throwableCandidate = getThrowableCandidate(argArray);

			if (messagePattern == null)
			{
				return new FormattingTuple(null, argArray, throwableCandidate);
			}

			if (argArray == null)
			{
				return new FormattingTuple(messagePattern);
			}

			int i = 0;
			int j;
			// use string builder for better multicore performance
			StringBuilder sbuf = new StringBuilder(messagePattern.Length + 50);

			int L;
			for (L = 0; L < argArray.Length; L++)
			{

				j = messagePattern.IndexOf(DELIM_STR, i);

				if (j == -1)
				{
					// no more variables
					if (i == 0)
					{ // this is a simple string
						return new FormattingTuple(messagePattern, argArray, throwableCandidate);
					}
					else
					{ // add the tail string which contains no variables and return
					  // the result.
						sbuf.Append(messagePattern.Substring(i));
						return new FormattingTuple(sbuf.ToString(), argArray, throwableCandidate);
					}
				}
				else
				{
					if (isEscapedDelimeter(messagePattern, j))
					{
						if (!isDoubleEscaped(messagePattern, j))
						{
							L--; // DELIM_START was escaped, thus should not be incremented
							sbuf.Append(messagePattern.Substring(i, j - 1 - i));
							sbuf.Append(DELIM_START);
							i = j + 1;
						}
						else
						{
							// The escape character preceding the delimiter start is
							// itself escaped: "abc x:\\{}"
							// we have to consume one backward slash
							sbuf.Append(messagePattern.Substring(i, j - 1 - i));
							deeplyAppendParameter(sbuf, argArray[L], new Dictionary<Object[], Object>());
							i = j + 2;
						}
					}
					else
					{
						// normal case
						sbuf.Append(messagePattern.Substring(i, j - i));
						deeplyAppendParameter(sbuf, argArray[L], new Dictionary<Object[], Object>());
						i = j + 2;
					}
				}
			}
			// append the characters following the last {} pair.
			sbuf.Append(messagePattern.Substring(i));
			if (L < argArray.Length - 1)
			{
				return new FormattingTuple(sbuf.ToString(), argArray, throwableCandidate);
			}
			else
			{
				return new FormattingTuple(sbuf.ToString(), argArray, null);
			}
		}

		static bool isEscapedDelimeter(string messagePattern, int delimeterStartIndex)
		{

			if (delimeterStartIndex == 0)
			{
				return false;
			}
			char potentialEscape = messagePattern[delimeterStartIndex - 1];
			if (potentialEscape == ESCAPE_CHAR)
			{
				return true;
			}
			else
			{
				return false;
			}
		}

		static bool isDoubleEscaped(string messagePattern, int delimeterStartIndex)
		{
			if (delimeterStartIndex >= 2 && messagePattern[delimeterStartIndex - 2] == ESCAPE_CHAR)
			{
				return true;
			}
			else
			{
				return false;
			}
		}

		// special treatment of array values was suggested by 'lizongbo'
		private static void deeplyAppendParameter(StringBuilder sbuf, Object o, Dictionary<Object[], Object> seenMap)
		{
			if (o == null)
			{
				sbuf.Append("null");
				return;
			}
			if (!o.GetType().IsArray)
			{
				safeObjectAppend(sbuf, o);
			}
			else
			{
				// check for primitive array types because they
				// unfortunately cannot be cast to Object[]
				if (o is bool[])
				{
					booleanArrayAppend(sbuf, (bool[])o);
				}
				else if (o is byte[])
				{
					byteArrayAppend(sbuf, (byte[])o);
				}
				else if (o is char[])
				{
					charArrayAppend(sbuf, (char[])o);
				}
				else if (o is short[])
				{
					shortArrayAppend(sbuf, (short[])o);
				}
				else if (o is int[])
				{
					intArrayAppend(sbuf, (int[])o);
				}
				else if (o is long[])
				{
					longArrayAppend(sbuf, (long[])o);
				}
				else if (o is float[])
				{
					floatArrayAppend(sbuf, (float[])o);
				}
				else if (o is double[])
				{
					doubleArrayAppend(sbuf, (double[])o);
				}
				else
				{
					objectArrayAppend(sbuf, (Object[])o, seenMap);
				}
			}
		}

		private static void safeObjectAppend(StringBuilder sbuf, Object o)
		{
			try
			{
				string oAsString = o.ToString();
				sbuf.Append(oAsString);
			}
			catch (Exception t)
			{
				Console.WriteLine("SLF4J: Failed toString() invocation on an object of type [" + o.GetType().Name + "]");
				sbuf.Append("[FAILED toString()]");
			}

		}

		private static void objectArrayAppend(StringBuilder sbuf, Object[] a, Dictionary<Object[], Object> seenMap)
		{
			sbuf.Append('[');
			if (!seenMap.ContainsKey(a))
			{
				seenMap.Add(a, null);
				int len = a.Length;
				for (int i = 0; i < len; i++)
				{
					deeplyAppendParameter(sbuf, a[i], seenMap);
					if (i != len - 1)
						sbuf.Append(", ");
				}
				// allow repeats in siblings
				seenMap.Remove(a);
			}
			else
			{
				sbuf.Append("...");
			}
			sbuf.Append(']');
		}

		private static void booleanArrayAppend(StringBuilder sbuf, bool[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void byteArrayAppend(StringBuilder sbuf, byte[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void charArrayAppend(StringBuilder sbuf, char[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void shortArrayAppend(StringBuilder sbuf, short[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void intArrayAppend(StringBuilder sbuf, int[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void longArrayAppend(StringBuilder sbuf, long[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void floatArrayAppend(StringBuilder sbuf, float[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}

		private static void doubleArrayAppend(StringBuilder sbuf, double[] a)
		{
			sbuf.Append('[');
			int len = a.Length;
			for (int i = 0; i < len; i++)
			{
				sbuf.Append(a[i]);
				if (i != len - 1)
					sbuf.Append(", ");
			}
			sbuf.Append(']');
		}
	}
}
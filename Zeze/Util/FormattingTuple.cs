using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
	/**
	 * Holds the results of formatting done by {@link MessageFormatter}.
	 * 
	 * @author Joern Huxhorn
	 */
	public class FormattingTuple
	{

		static public FormattingTuple NULL = new FormattingTuple(null);

		private string message;
		private Exception throwable;
		private Object[] argArray;

		public FormattingTuple(string message) : this(message, null, null)
		{
		}

		public FormattingTuple(string message, Object[] argArray, Exception throwable)
		{
			this.message = message;
			this.throwable = throwable;
			if (throwable == null)
			{
				this.argArray = argArray;
			}
			else
			{
				this.argArray = trimmedCopy(argArray);
			}
		}

		static Object[] trimmedCopy(Object[] argArray)
		{
			if (argArray == null || argArray.Length == 0)
			{
				throw new ArgumentException("non-sensical empty or null argument array");
			}
			int trimemdLen = argArray.Length - 1;
			Object[] trimmed = new Object[trimemdLen];
			Array.Copy(argArray, 0, trimmed, 0, trimemdLen);
			return trimmed;
		}

		public String getMessage()
		{
			return message;
		}

		public Object[] getArgArray()
		{
			return argArray;
		}

		public Exception getThrowable()
		{
			return throwable;
		}

	}
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public sealed class SimpleAssert
    {
		public static void IsTrue(bool c)
		{
			if (false == c)
				throw new ThrowAgainException();
		}

		public static void IsNull(object o)
		{
			if (null != o)
				throw new ThrowAgainException();
		}

		public static void AreEqual(object except, object current)
		{
			if (except.Equals(current))
				return;
			throw new ThrowAgainException();
		}

	}
}

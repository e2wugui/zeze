using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public class GoBackZezeException : Exception
    {
        public GoBackZezeException(string msg = null, Exception cause = null)
            : base(msg, cause)
        {
        }
    }
}

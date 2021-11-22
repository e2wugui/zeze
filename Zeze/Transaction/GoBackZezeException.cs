using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public class GoBackZezeException : Exception
    {
        public GoBackZezeException(String msg = null, Exception cause = null)
            : base(msg, cause)
        {
        }
    }
}

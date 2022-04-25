using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;

namespace Zeze.Arch.Gen
{
    public interface Result
    {
    }

    public class VoidResult : Result
    {
        public static VoidResult Instance = new VoidResult();
        public static Func<Binary, VoidResult> Decoder = (param) => Instance;
    }
}

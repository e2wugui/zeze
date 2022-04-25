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

    public class EmptyResult : Result
    {
        public static EmptyResult Instance = new EmptyResult();
        public static Func<Binary, EmptyResult> Decoder = (param) => Instance;
    }
}

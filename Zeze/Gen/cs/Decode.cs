using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class Decode
    {
        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public override void Decode(ByteBuffer _os_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }
    }
}

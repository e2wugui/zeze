using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.lua
{
    public class ProtocolFormatter
    {
        public static void Make(string moduleName, Protocol p, System.IO.StreamWriter sw)
        {
            sw.WriteLine($"{moduleName}.{p.Name} = {{");
            sw.WriteLine($"    TypeId = {p.TypeId},");
            sw.WriteLine($"    ResultCode = 0,");
            sw.WriteLine($"    Argument = {{}},");
            sw.WriteLine("}");
            sw.WriteLine($"{moduleName}.{p.Name}.Send = function (p)");
            sw.WriteLine("    ZezeNetServiceSendProtocol(ZezeNetServiceCurrentService, ZezeNetServiceCurrentSessionId, p)");
            sw.WriteLine("end");
        }
    }
}

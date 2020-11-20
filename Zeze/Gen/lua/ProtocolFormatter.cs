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
            sw.WriteLine($"    ModuleId = {p.Space.Id},");
            sw.WriteLine($"    ProtocolId = {p.Id},");
            sw.WriteLine($"    ResultCode = 0,");
            sw.WriteLine($"    Argument = {{}},");
            if (p is Rpc)
                sw.WriteLine($"    Result = {{}},");

            sw.WriteLine("}");
        }
    }
}

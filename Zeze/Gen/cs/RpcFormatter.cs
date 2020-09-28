using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class RpcFormatter
    {
        global::Zeze.Gen.Rpc rpc;
        public RpcFormatter(global::Zeze.Gen.Rpc p)
        {
            this.rpc = p;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + rpc.Space.Path());
            sw.WriteLine("{");

            string argument = rpc.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.ResultType);

            sw.WriteLine("    public sealed class " + rpc.Name + " : Zeze.Net.Rpc<" + argument + ", " + result + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public override int ModuleId => " + rpc.Space.Id + ";");
            sw.WriteLine("        public override int ProtocolId => " + rpc.Id + ";");
            // declare enums
            foreach (Types.Enum e in rpc.Enums)
            {
                sw.WriteLine("        public const int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (rpc.Enums.Count > 0)
            {
                sw.WriteLine("");
            }
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

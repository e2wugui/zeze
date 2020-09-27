using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class ProtocolFormatter
    {
        global::Zeze.Gen.Protocol p;
        public ProtocolFormatter(global::Zeze.Gen.Protocol p)
        {
            this.p = p;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = p.Space.OpenWriter(baseDir, p.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + p.Space.Path());
            sw.WriteLine("{");

            string argument = p.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(p.ArgumentType);
            sw.WriteLine("    public sealed class " + p.Name + " : Zeze.Net.Protocol<" + argument + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public override int ModuleId => " + p.Space.Id + ";");
            sw.WriteLine("        public override int ProtocolId => " + p.Id + ";");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

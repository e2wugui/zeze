using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class ProtocolFormatter
    {
        Zeze.Gen.Protocol protocol;
        public ProtocolFormatter(Zeze.Gen.Protocol p)
        {
            this.protocol = p;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = protocol.Space.OpenWriter(baseDir, protocol.Name + ".cs");

            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + protocol.Space.Path("."));
            sw.WriteLine("{");

            string argument = TypeName.GetName(protocol.ArgumentType);
            sw.WriteLine("    public sealed class " + protocol.Name + " : Zeze.Net.Protocol<" + argument + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public override int ModuleId => " + protocol.Space.Id + ";");
            sw.WriteLine("        public override int ProtocolId => " + protocol.Id + ";");
            sw.WriteLine("");
            sw.WriteLine("        public override void Run()");
            sw.WriteLine("        {");
            sw.WriteLine("            // TODO ");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

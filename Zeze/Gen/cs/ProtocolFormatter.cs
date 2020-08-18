using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class ProtocolFormatter
    {
        Zeze.Gen.Protocol p;
        public ProtocolFormatter(Zeze.Gen.Protocol p)
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

            string argument = TypeName.GetName(p.ArgumentType);
            sw.WriteLine("    public sealed class " + p.Name + " : Zeze.Net.Protocol<" + argument + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public override int ModuleId => " + p.Space.Id + ";");
            sw.WriteLine("        public override int ProtocolId => " + p.Id + ";");
            sw.WriteLine("");
            sw.WriteLine("        public override void Run()");
            sw.WriteLine("        {");
            Module m = (Module)p.Space;
            if ((m.ReferenceManager.HandleFlags & p.HandleFlags) != 0)
            {
                sw.WriteLine("            " + p.Space.Solution.Path(".", "App.Instance.") + m.Path("_", m.Name) + ".On" + p.Name + "(this);");
            }
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

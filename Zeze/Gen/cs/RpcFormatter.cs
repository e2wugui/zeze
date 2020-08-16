using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class RpcFormatter
    {
        Zeze.Gen.Rpc rpc;
        public RpcFormatter(Zeze.Gen.Rpc p)
        {
            this.rpc = p;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".cs");

            sw.WriteLine("");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + rpc.Space.Path("."));
            sw.WriteLine("{");

            string argument = TypeName.GetName(rpc.ArgumentType);
            string result = TypeName.GetName(rpc.ResultType);

            sw.WriteLine("    public sealed class " + rpc.Name + " : Zeze.Net.Rpc<" + argument + ", " + result + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public override int ModuleId => " + rpc.Space.Id + ";");
            sw.WriteLine("        public override int ProtocolId => " + rpc.Id + ";");
            sw.WriteLine("");
            sw.WriteLine("        public override void OnServer()");
            sw.WriteLine("        {");
            Module m = (Module)rpc.Space;
            if ((rpc.HandleFlags & Program.HandleServerFlag) != 0)
            {
                sw.WriteLine("            " + rpc.Space.Solution.Path(".", "App.Instance.") + m.Path("_", m.Name) + ".On" + rpc.Name + "Server(this);");
            }
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public override void OnClient()");
            sw.WriteLine("        {");
            if ((rpc.HandleFlags & Program.HandleClientFlag) != 0)
            {
                sw.WriteLine("            " + rpc.Space.Solution.Path(".", "App.Instance.") + m.Path("_", m.Name) + ".On" + rpc.Name + "Client(this);");
            }
            sw.WriteLine("        }");
            sw.WriteLine("");
            sw.WriteLine("        public override void OnTimeout()");
            sw.WriteLine("        {");
            if ((rpc.HandleFlags & Program.HandleClientFlag) != 0)
            {
                sw.WriteLine("            " + rpc.Space.Solution.Path(".", "App.Instance.") + m.Path("_", m.Name) + ".On" + rpc.Name + "Timeout(this);");
            }
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

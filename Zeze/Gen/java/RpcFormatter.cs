using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.java
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
            using System.IO.StreamWriter sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".java");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("package " + rpc.Space.Path());
            sw.WriteLine("");
            string argument = rpc.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.ResultType);

            sw.WriteLine("public class " + rpc.Name + " : Zeze.Net.Rpc1<" + argument + ", " + result + "> {");
            sw.WriteLine("    public final static int ModuleId_ = " + rpc.Space.Id + ";");
            sw.WriteLine("    public final static int ProtocolId_ = " + rpc.Id + ";");
            sw.WriteLine("    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; ");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public int getModuleId() {");
            sw.WriteLine("        return ModuleId_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public int getProtocolId() {");
            sw.WriteLine("        return ProtocolId_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            // declare enums
            foreach (Types.Enum e in rpc.Enums)
            {
                sw.WriteLine("    public final static int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (rpc.Enums.Count > 0)
            {
                sw.WriteLine("");
            }
            sw.WriteLine($"    public {rpc.Name}() {{");
            sw.WriteLine($"        Argument = new {argument}();");
            sw.WriteLine($"        Result = new {result}();");
            sw.WriteLine("    }");
            sw.WriteLine(); sw.WriteLine("}");
        }
    }
}

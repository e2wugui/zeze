﻿
namespace Zeze.Gen.java
{
    public class RpcFormatter
    {
        readonly Rpc rpc;

        public RpcFormatter(Rpc rpc)
        {
            this.rpc = rpc;
        }

        public void Make(string baseDir)
        {
            using var sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + rpc.Space.Path() + ";");
            sw.WriteLine();
            if (rpc.Comment.Length > 0)
                sw.WriteLine(rpc.Comment);
            string argument = rpc.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(rpc.ResultType);
            string baseclass = string.IsNullOrEmpty(rpc.Base) ? "Zeze.Net.Rpc" : rpc.Base;
            if (rpc.UseData)
            {
                argument += "Data";
                result += "Data";
            }

            sw.WriteLine($"public class {rpc.Name} extends {baseclass}<{argument}, {result}> {{");
            sw.WriteLine("    public static final int ModuleId_ = " + rpc.Space.Id + ";");
            sw.WriteLine("    public static final int ProtocolId_ = " + rpc.Id + ";" + (rpc.Id < 0 ? " // " + (uint)rpc.Id : ""));
            sw.WriteLine("    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // " + Net.Protocol.MakeTypeId(rpc.Space.Id, rpc.Id));
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
            sw.WriteLine("    @Override");
            sw.WriteLine("    public long getTypeId() {");
            sw.WriteLine("        return TypeId_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            // declare enums
            foreach (Types.Enum e in rpc.Enums)
                sw.WriteLine($"    public static final {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (rpc.Enums.Count > 0)
                sw.WriteLine();
            sw.WriteLine($"    public {rpc.Name}() {{");
            if (rpc.ArgumentType != null)
                sw.WriteLine($"        Argument = new {argument}();");
            else
                sw.WriteLine($"        Argument = {argument}.instance;");
            if (rpc.ResultType != null)
                sw.WriteLine($"        Result = new {result}();");
            else
                sw.WriteLine($"        Result = {result}.instance;");
            sw.WriteLine("    }");
            if (rpc.ArgumentType != null)
            {
                sw.WriteLine();
                sw.WriteLine($"    public {rpc.Name}({argument} arg) {{");
                sw.WriteLine($"        Argument = arg;");
                if (rpc.ResultType != null)
                    sw.WriteLine($"        Result = new {result}();");
                else
                    sw.WriteLine($"        Result = {result}.instance;");
                sw.WriteLine("    }");
            }
            sw.WriteLine("}");
        }
    }
}

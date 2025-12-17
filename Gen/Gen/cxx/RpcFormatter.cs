
namespace Zeze.Gen.cxx
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
            using var sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".hpp");
            if (sw == null)
                return;

            sw.WriteLine("#pragma once");
            sw.WriteLine();
            sw.WriteLine("#include \"zeze/cxx/Rpc.h\"");
            if (rpc.ArgumentType != null)
            {
                var argBean = (Types.Bean)rpc.ArgumentType;
                sw.WriteLine($"#include \"Gen/{argBean.Space.Path("/", argBean.Name + ".hpp")}\"");
            }
            if (rpc.ResultType != null && rpc.ResultType != rpc.ArgumentType)
            {
                var argBean = (Types.Bean)rpc.ResultType;
                sw.WriteLine($"#include \"Gen/{argBean.Space.Path("/", argBean.Name + ".hpp")}\"");
            }
            sw.WriteLine();

            var paths = rpc.Space.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            if (rpc.Comment.Length > 0)
                sw.WriteLine(rpc.Comment);
            string argument = rpc.ArgumentType == null ? "Zeze::EmptyBean" : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? "Zeze::EmptyBean" : TypeName.GetName(rpc.ResultType);
            string baseclass = string.IsNullOrEmpty(rpc.Base) ? "Zeze::Net::Rpc" : rpc.Base;

            sw.WriteLine($"class {rpc.Name} : public {baseclass}<{argument}, {result}> {{");
            sw.WriteLine("public:");
            sw.WriteLine("    static const int ModuleId_ = " + rpc.Space.Id + ";");
            sw.WriteLine("    static const int ProtocolId_ = " + rpc.Id + ";" + (rpc.Id < 0 ? " // " + (uint)rpc.Id : ""));
            sw.WriteLine("    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // " + Net.Protocol.MakeTypeId(rpc.Space.Id, rpc.Id));
            sw.WriteLine();
            sw.WriteLine("    virtual int ModuleId() const override {");
            sw.WriteLine("        return ModuleId_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    virtual int ProtocolId() const override {");
            sw.WriteLine("        return ProtocolId_;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    int64_t TypeId() const {");
            sw.WriteLine("        return TypeId_;");
            sw.WriteLine("    }");
            // declare enums
            if (rpc.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in rpc.Enums)
                sw.WriteLine($"    static const {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }
    }
}

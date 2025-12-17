
namespace Zeze.Gen.cxx
{
    public class ProtocolFormatter
    {
        readonly Protocol p;
 
        public ProtocolFormatter(Protocol p)
        {
            this.p = p;
        }

        public void Make(string baseDir)
        {
            using var sw = p.Space.OpenWriter(baseDir, p.Name + ".hpp");
            if (sw == null)
                return;

            sw.WriteLine("#pragma once");
            sw.WriteLine();
            sw.WriteLine("#include \"zeze/cxx/Protocol.h\"");
            if (p.ArgumentType != null)
            {
                var argBean = (Types.Bean)p.ArgumentType;
                sw.WriteLine($"#include \"Gen/{argBean.Space.Path("/", argBean.Name + ".hpp")}\"");
            }
            sw.WriteLine();
            var paths = p.Space.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            if (p.Comment.Length > 0)
                sw.WriteLine(p.Comment);

            string argument = p.ArgumentType == null ? "Zeze::EmptyBean" : TypeName.GetName(p.ArgumentType);
            sw.WriteLine("class " + p.Name + " : public Zeze::Net::ProtocolWithArgument<" + argument + "> {");
            sw.WriteLine("public:");
            sw.WriteLine("    static const int ModuleId_ = " + p.Space.Id + ";");
            sw.WriteLine("    static const int ProtocolId_ = " + p.Id + ";" + (p.Id < 0 ? " // " + (uint)p.Id : ""));
            sw.WriteLine("    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // " + Net.Protocol.MakeTypeId(p.Space.Id, p.Id));
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
            if (p.Enums.Count > 0)
            {
                sw.WriteLine();
            }
            foreach (Types.Enum e in p.Enums)
            {
                sw.WriteLine($"    static const {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }
    }
}

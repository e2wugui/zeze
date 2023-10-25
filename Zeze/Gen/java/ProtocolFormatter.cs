
namespace Zeze.Gen.java
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
            using var sw = p.Space.OpenWriter(baseDir, p.Name + ".java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + p.Space.Path() + ";");
            sw.WriteLine();
            if (p.Comment.Length > 0)
                sw.WriteLine(p.Comment);
            string argument = p.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(p.ArgumentType);
            if (p.UseData)
            {
                if (null == p.ArgumentType || false == p.ArgumentType.IsKeyable)
                    argument += ".Data";
            }

            sw.WriteLine("public class " + p.Name + " extends Zeze.Net.Protocol<" + argument + "> {");
            sw.WriteLine("    public static final int ModuleId_ = " + p.Space.Id + ";");
            sw.WriteLine("    public static final int ProtocolId_ = " + p.Id + ";" + (p.Id < 0 ? " // " + (uint)p.Id : ""));
            sw.WriteLine("    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // " + Net.Protocol.MakeTypeId(p.Space.Id, p.Id));
            sw.WriteLine("    static { register(TypeId_, " + p.Name + ".class); }");
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
            if (p.CriticalLevel != Protocol.eCriticalPlus)
            {
                sw.WriteLine("    @Override");
                sw.WriteLine("    public int getCriticalLevel() {");
                sw.WriteLine($"        return {p.CriticalLevel};");
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            // declare enums
            foreach (Types.Enum e in p.Enums)
            {
                sw.WriteLine($"    public static final {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (p.Enums.Count > 0)
            {
                sw.WriteLine();
            }
            sw.WriteLine("    public " + p.Name + "() {");
            if (p.ArgumentType != null)
                sw.WriteLine($"        Argument = new {argument}();");
            else
                sw.WriteLine($"        Argument = {argument}.instance;");
            sw.WriteLine("    }");
            if (p.ArgumentType != null)
            {
                sw.WriteLine();
                sw.WriteLine($"    public {p.Name}({argument} arg) {{");
                sw.WriteLine($"        Argument = arg;");
                sw.WriteLine("    }");
            }

            /* 现在的bean不是所有的变量都可以赋值，还是先不支持吧。
            if (p.ArgumentType != null)
            {
                Types.Bean argBean = (Types.Bean)p.ArgumentType;
                sw.WriteLine("        public " + p.Name + "(" + ParamName.GetParamList(argBean.Variables) + ")");
                sw.WriteLine("        {");
                foreach (Types.Variable var in argBean.Variables)
                    sw.WriteLine("            this.Argument." + var.NameUpper1 + " = _" + var.Name + "_;");
                sw.WriteLine("        }");
                sw.WriteLine();
            }
            */
            sw.WriteLine("}");
        }
    }
}

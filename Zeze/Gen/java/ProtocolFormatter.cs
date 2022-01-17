
namespace Zeze.Gen.java
{
    public class ProtocolFormatter
    {
        Protocol p;
 
        public ProtocolFormatter(Protocol p)
        {
            this.p = p;
        }

        public void Make(string baseDir)
        {
            using var sw = p.Space.OpenWriter(baseDir, p.Name + ".java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + p.Space.Path() + ";");
            sw.WriteLine();
            string argument = p.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(p.ArgumentType);
            sw.WriteLine("public class " + p.Name + " extends Zeze.Net.Protocol1<" + argument + "> {");
            sw.WriteLine("    public static final int ModuleId_ = " + p.Space.Id + ";");
            sw.WriteLine("    public static final int ProtocolId_ = " + p.Id + ";");
            sw.WriteLine("    public static final long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffffL); ");
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
            foreach (Types.Enum e in p.Enums)
            {
                sw.WriteLine("    public static final int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (p.Enums.Count > 0)
            {
                sw.WriteLine();
            }
            sw.WriteLine("    public " + p.Name + "() {");
            sw.WriteLine($"        Argument = new {argument}();");
            sw.WriteLine("    }");
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

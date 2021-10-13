using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.java
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
            sw.WriteLine("package " + p.Space.Path());
            sw.WriteLine("");
            string argument = p.ArgumentType == null ? "Zeze.Transaction.EmptyBean" : TypeName.GetName(p.ArgumentType);
            sw.WriteLine("public class " + p.Name + " : Zeze.Net.Protocol1<" + argument + "> {");
            sw.WriteLine("    public final static int ModuleId_ = " + p.Space.Id + ";");
            sw.WriteLine("    public final static int ProtocolId_ = " + p.Id + ";");
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
            foreach (Types.Enum e in p.Enums)
            {
                sw.WriteLine("    public final static int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (p.Enums.Count > 0)
            {
                sw.WriteLine("");
            }
            sw.WriteLine("    public " + p.Name + "() {");
            sw.WriteLine($"        Argument = new {argument}();");
            sw.WriteLine("    }");
            sw.WriteLine("");
            /* 现在的bean不是所有的变量都可以赋值，还是先不支持吧。
            if (p.ArgumentType != null)
            {
                Types.Bean argBean = (Types.Bean)p.ArgumentType;
                sw.WriteLine("        public " + p.Name + "(" + ParamName.GetParamList(argBean.Variables) + ")");
                sw.WriteLine("        {");
                foreach (Types.Variable var in argBean.Variables)
                    sw.WriteLine("            this.Argument." + var.NameUpper1 + " = _" + var.Name + "_;");
                sw.WriteLine("        }");
                sw.WriteLine("");
            }
            */
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

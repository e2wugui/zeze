using System.IO;

namespace Zeze.Gen.cs
{
    public class ProtocolFormatter
    {
        readonly Protocol p;
 
        public ProtocolFormatter(Protocol p)
        {
            this.p = p;
        }

        public void Make(string baseDir, bool isconfcs = false)
        {
            using StreamWriter sw = p.Space.OpenWriter(baseDir, p.Name + ".cs");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine();
            if (p.Comment.Length > 0)
                sw.WriteLine(p.Comment);
            sw.WriteLine("// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext");
            sw.WriteLine("// ReSharper disable once CheckNamespace");
            sw.WriteLine("namespace " + p.Space.Path());
            sw.WriteLine("{");

            var emptyBeanName = isconfcs ? "Zeze.Util.ConfEmptyBean" : "Zeze.Transaction.EmptyBean";
            string argument = p.ArgumentType == null ? emptyBeanName : TypeName.GetName(p.ArgumentType);
            sw.WriteLine("    public sealed class " + p.Name + " : Zeze.Net.Protocol<" + argument + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public const int ModuleId_ = " + p.Space.Id + ";");
            sw.WriteLine("        public const int ProtocolId_ = " + p.Id + ";" + (p.Id < 0 ? " // " + (uint)p.Id : ""));
            sw.WriteLine("        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // " + Net.Protocol.MakeTypeId(p.Space.Id, p.Id));
            sw.WriteLine();
            sw.WriteLine("        public override int ModuleId => ModuleId_;");
            sw.WriteLine("        public override int ProtocolId => ProtocolId_;");
            // declare enums
            if (p.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in p.Enums)
                sw.WriteLine($"        public const {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
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
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

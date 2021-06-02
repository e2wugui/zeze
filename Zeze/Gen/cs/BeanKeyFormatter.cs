using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class BeanKeyFormatter
    {
        Types.BeanKey beanKey;

        public BeanKeyFormatter(Types.BeanKey beanKey)
        {
            this.beanKey = beanKey;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = beanKey.Space.OpenWriter(baseDir, beanKey.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("using Zeze.Serialize;");
            sw.WriteLine("using System;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + beanKey.Space.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed class " + beanKey.Name + " : Serializable, System.IComparable");
            sw.WriteLine("    {");
            // declare enums
            foreach (Types.Enum e in beanKey.Enums)
            {
                sw.WriteLine("        public const int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (beanKey.Enums.Count > 0)
            {
                sw.WriteLine("");
            }

            // declare variables
            foreach (Types.Variable v in beanKey.Variables)
            {
                sw.WriteLine("        private " + TypeName.GetName(v.VariableType) + " " + v.NamePrivate + ";" + v.Comment);
            }
            sw.WriteLine("");

            sw.WriteLine("        // for decode only");
            sw.WriteLine("        public " + beanKey.Name + "()");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine("");

            // params construct
            {
                sw.WriteLine("        public " + beanKey.Name + "(" + ParamName.GetParamList(beanKey.Variables) + ")");
                sw.WriteLine("        {");
                foreach (Types.Variable v in beanKey.Variables)
                {
                    sw.WriteLine("            this." + v.NamePrivate + " = " + v.NamePrivate + "_;");
                }
                sw.WriteLine("        }");
                sw.WriteLine("");
            }
            PropertyBeanKey.Make(beanKey, sw, "        ");
            sw.WriteLine("");
            Tostring.Make(beanKey, sw, "        ");
            Encode.Make(beanKey, sw, "        ");
            Decode.Make(beanKey, sw, "        ");
            Equal.Make(beanKey, sw, "        ");
            HashCode.Make(beanKey, sw, "        ");
            Compare.Make(beanKey, sw, "        ");
            NegativeCheck.Make(beanKey, sw, "        ");
            sw.WriteLine("    }");
            sw.WriteLine("}");

        }
    }
}

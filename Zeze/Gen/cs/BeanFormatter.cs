using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Diagnostics.Contracts;
using System.Net.Mime;
using System.Text;
using System.Threading.Tasks.Dataflow;

namespace Zeze.Gen.cs
{
    public class BeanFormatter
    {
        Types.Bean bean;

        public BeanFormatter(Types.Bean bean)
        {
            this.bean = bean;
        }

        public void Make(string baseDir)
        {
            using System.IO.StreamWriter sw = bean.Space.OpenWriter(baseDir, bean.Name + ".cs");

            sw.WriteLine("");
            sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + bean.Space.Path("."));
            sw.WriteLine("{");
            sw.WriteLine("    public sealed class " + bean.Name + " : Zeze.Transaction.Bean");
            sw.WriteLine("    {");
            WriteDefine(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void WriteDefine(System.IO.StreamWriter sw)
        {
            // declare enums
            foreach (Types.Enum e in bean.Enums)
            {
                sw.WriteLine("        public const int " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (bean.Enums.Count > 0)
            {
                sw.WriteLine("");
            }

            // declare variables
            foreach (Types.Variable v in bean.Variables)
            {
                sw.WriteLine("        private " + TypeName.GetName(v.VariableType) + " " + v.NamePrivate + ";" + v.Comment);
            }
            sw.WriteLine("");

            Construct.Make(bean, sw, "        ");
            Encode.Make(bean, sw, "        ");
            Decode.Make(bean, sw, "        ");
            InitChildrenTableKey.Make(bean, sw, "        ");
        }
    }
}

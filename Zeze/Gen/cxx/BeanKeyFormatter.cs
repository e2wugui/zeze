using System.Collections.Generic;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cxx
{
    public class BeanKeyFormatter
    {
        readonly BeanKey beanKey;

        public BeanKeyFormatter(BeanKey beanKey)
        {
            this.beanKey = beanKey;
        }

        public void Make(string baseDir)
        {
            using StreamWriter sw = beanKey.Space.OpenWriter(baseDir, beanKey.Name + ".hpp");
            if (sw == null)
                return;

            sw.WriteLine("#pragma once");
            sw.WriteLine();
            sw.WriteLine("#include \"zeze/cxx/Bean.h\"");
            var includes = new HashSet<Type>();
            beanKey.DependsVariables(includes);
            foreach (var inc in includes)
            {
                if (inc is Bean b)
                {
                    sw.WriteLine($"#include \"Gen/{b.Space.Path("/", b.Name + ".hpp")}\"");
                }
                else if (inc is BeanKey k)
                {
                    sw.WriteLine($"#include \"Gen/{k.Space.Path("/", k.Name + ".hpp")}\"");
                }
            }
            sw.WriteLine();
            var paths = beanKey.Space.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            if (beanKey.Comment.Length > 0)
                sw.WriteLine(beanKey.Comment);
            sw.WriteLine($"class {beanKey.Name} : public Zeze::Serializable {{");

            sw.WriteLine("public:");
            // declare enums
            foreach (Enum e in beanKey.Enums)
                sw.WriteLine($"    static const {TypeName.GetName(Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (beanKey.Enums.Count > 0)
                sw.WriteLine();

            // declare variables
            foreach (Variable v in beanKey.Variables)
                sw.WriteLine("    " + TypeName.GetName(v.VariableType) + " " + v.NameUpper1 + ";" + v.Comment);
            sw.WriteLine();

            Construct.Make(beanKey, sw, "    ");
            Assign.Make(beanKey, sw, "    ");
            // params construct
            if (beanKey.Variables.Count > 0)
            {
                sw.WriteLine("    " + beanKey.Name + "(" + ParamName.GetParamList(beanKey.Variables) + ") {");
                foreach (Variable v in beanKey.Variables)
                {
                    sw.WriteLine("        " + v.NameUpper1 + " = " + v.NameUpper1 + "_;");
                }
                sw.WriteLine("    }");
                sw.WriteLine();
            }
            PropertyBeanKey.Make(beanKey, sw, "    ");
            Encode.Make(beanKey, sw, "    ");
            Decode.Make(beanKey, sw, "    ");
            //Equal.Make(beanKey, sw, "    ");
            //HashCode.Make(beanKey, sw, "    ");
            Compare.Make(beanKey, sw, "    ");
            //NegativeCheck.Make(beanKey, sw, "    ");
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }
    }
}

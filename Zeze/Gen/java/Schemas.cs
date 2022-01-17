using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.java
{
    public class Schemas
    {
        public Project Project { get; }
        public HashSet<Types.Type> Depends { get; } = new HashSet<Types.Type>();
        public string GenDir { get; }

        public Schemas(Project prj, string gendir)
        {
            Project = prj;
            GenDir = gendir;

            foreach (Table table in Project.AllTables.Values)
            {
                if (Project.GenTables.Contains(table.Gen))
                    table.Depends(Depends);
            }
        }

        private string GetFullName(Types.Type type)
        {
            if (type.IsBean)
            {
                if (type.IsKeyable)
                    return (type as Types.BeanKey).FullName;
                return (type as Types.Bean).FullName;
            }
            return type.Name;
        }

        public void Make()
        {
            using System.IO.StreamWriter sw = Project.Solution.OpenWriter(GenDir, "Schemas.java");

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + Project.Solution.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("public class Schemas extends Zeze.Schemas {");
            sw.WriteLine("    public Schemas() {");

            foreach (var table in Project.AllTables.Values)
            {
                sw.WriteLine($"        AddTable(new Zeze.Schemas.Table(\"{table.Space.Path("_", table.Name)}\", \"{GetFullName(table.KeyType)}\", \"{GetFullName(table.ValueType)}\"));");
            }

            foreach (var type in Depends)
            {
                if (!type.IsBean)
                    continue;

                if (type.IsKeyable)
                {
                    var beanKey = type as Types.BeanKey;
                    GenAddBean(sw, beanKey.FullName, true, beanKey.Variables);
                }
                else
                {
                    var bean = type as Types.Bean;
                    GenAddBean(sw, bean.FullName, false, bean.Variables);
                }
            }
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        private void GenAddBean(System.IO.StreamWriter sw, string name, bool isBeanKey, List<Types.Variable> vars)
        {
            sw.WriteLine($"        {{");
            sw.WriteLine($"            var bean = new Zeze.Schemas.Bean(\"{name}\", {isBeanKey.ToString().ToLower()});");
            foreach (var v in vars)
            {
                sw.WriteLine($"        {{");
                sw.WriteLine($"            var var = new Zeze.Schemas.Variable();");
                sw.WriteLine($"            var.Id = {v.Id};");
                sw.WriteLine($"            var.Name = \"{v.Name}\";");
                sw.WriteLine($"            var.TypeName = \"{GetFullName(v.VariableType)}\";");
                if (v.VariableType is Types.TypeCollection collection)
                {
                    sw.WriteLine($"            var.ValueName = \"{GetFullName(collection.ValueType)}\";");
                }
                else if (v.VariableType is Types.TypeMap map)
                {
                    sw.WriteLine($"            var.KeyName = \"{GetFullName(map.KeyType)}\";");
                    sw.WriteLine($"            var.ValueName = \"{GetFullName(map.ValueType)}\";");
                }
                sw.WriteLine($"            bean.AddVariable(var);");
                sw.WriteLine($"        }}");
            }
            sw.WriteLine($"        AddBean(bean);");
            sw.WriteLine($"    }}");
        }
    }
}

using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
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

            foreach (Table table in Project.AllTables)
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
            using System.IO.StreamWriter sw = Project.Solution.OpenWriter(GenDir, "Schemas.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("namespace " + Project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public class Schemas : Zeze.Schemas");
            sw.WriteLine("    {");
            sw.WriteLine("        public Schemas()");
            sw.WriteLine("        {");

            foreach (var table in Project.AllTables)
            {
                sw.WriteLine("            base.AddTable(new Zeze.Schemas.Table()");
                sw.WriteLine("            {");
                sw.WriteLine($"                Name = \"{table.Space.Path("_", table.Name)}\",");
                sw.WriteLine($"                KeyName = \"{GetFullName(table.KeyType)}\",");
                sw.WriteLine($"                ValueName = \"{GetFullName(table.ValueType)}\",");
                sw.WriteLine("            });");
            }

            foreach (var type in Depends)
            {
                if (false == type.IsBean)
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
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        private void GenAddBean(System.IO.StreamWriter sw, string name, bool isBeanKey, List<Types.Variable> vars)
        {
            sw.WriteLine($"            {{");
            sw.WriteLine($"                var bean = new Zeze.Schemas.Bean() {{ Name = \"{name}\", IsBeanKey = {isBeanKey.ToString().ToLower()} }};");
            foreach (var v in vars)
            {
                sw.WriteLine($"                bean.AddVariable(new Zeze.Schemas.Variable()");
                sw.WriteLine($"                {{");
                sw.WriteLine($"                    Id = {v.Id},");
                sw.WriteLine($"                    Name = \"{v.Name}\",");
                sw.WriteLine($"                    TypeName = \"{GetFullName(v.VariableType)}\",");
                if (v.VariableType is Types.TypeCollection collection)
                {
                    sw.WriteLine($"                    ValueName = \"{GetFullName(collection.ValueType)}\",");
                }
                else if (v.VariableType is Types.TypeMap map)
                {
                    sw.WriteLine($"                    KeyName = \"{GetFullName(map.KeyType)}\",");
                    sw.WriteLine($"                    ValueName = \"{GetFullName(map.ValueType)}\",");
                }
                sw.WriteLine($"                }});");
            }
            sw.WriteLine($"                base.AddBean(bean);");
            sw.WriteLine($"            }}");
        }
    }
}

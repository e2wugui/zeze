using System.Collections.Generic;
using System.IO;
using Zeze.Gen.Types;

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
                    table.Depends(Depends, null);
            }
        }

        string GetFullName(Types.Type type)
        {
            if (type.IsBean)
            {
                if (type.IsKeyable)
                    return (type as Types.BeanKey).FullName;
                if (type is TypeDynamic)
                    return "Zeze.Transaction.DynamicBean";
                return (type as Types.Bean).FullName;
            }
            return type.Name;
        }

        public void Make()
        {
            using StreamWriter sw = Project.Solution.OpenWriter(GenDir, "Schemas.java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + Project.Solution.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("public class Schemas extends Zeze.Schemas {");
            sw.WriteLine("    public Schemas() {");

            foreach (var table in Project.AllTables.Values)
            {
                if (!table.NoSchema)
                    sw.WriteLine($"        addTable(new Zeze.Schemas.Table(\"{table.Space.Path("_", table.Name)}\", \"{GetFullName(table.KeyType)}\", \"{GetFullName(table.ValueType)}\"));");
            }

            foreach (var type in Depends)
            {
                if (!type.IsBean)
                    continue;

                if (type.IsKeyable)
                    sw.WriteLine($"        addBean_{((Types.BeanKey)type).FullName.Replace('.', '_')}();");
                else
                    sw.WriteLine($"        addBean_{((Types.Bean)type).FullName.Replace('.', '_')}();");
            }
            sw.WriteLine("    }");
            foreach (var type in Depends)
            {
                if (!type.IsBean)
                    continue;

                if (type.IsKeyable)
                {
                    var beanKey = type as Types.BeanKey;
                    GenAddBean(sw, beanKey.FullName, true, beanKey.Variables);
                }
                else if (type is Bean bean)
                {
                    GenAddBean(sw, bean.FullName, false, bean.Variables);
                }
            }
            sw.WriteLine("}");
        }

        void GenAddBean(StreamWriter sw, string name, bool isBeanKey, List<Types.Variable> vars)
        {
            sw.WriteLine();
            sw.WriteLine($"    private void addBean_{name.Replace('.', '_')}() {{");
            sw.WriteLine($"        var bean = new Zeze.Schemas.Bean(\"{name}\", {isBeanKey.ToString().ToLower()});");
            foreach (var v in vars)
            {
                sw.WriteLine($"        {{");
                sw.WriteLine($"            var var = new Zeze.Schemas.Variable();");
                sw.WriteLine($"            var.id = {v.Id};");
                sw.WriteLine($"            var.name = \"{v.Name}\";");
                sw.WriteLine($"            var.typeName = \"{GetFullName(v.VariableType)}\";");
                if (v.VariableType is Types.TypeCollection collection)
                {
                    sw.WriteLine($"            var.valueName = \"{GetFullName(collection.ValueType)}\";");
                }
                else if (v.VariableType is Types.TypeMap map)
                {
                    sw.WriteLine($"            var.keyName = \"{GetFullName(map.KeyType)}\";");
                    sw.WriteLine($"            var.valueName = \"{GetFullName(map.ValueType)}\";");
                }
                else if (v.VariableType is TypeDynamic dynamic)
                {
                    foreach (var real in dynamic.RealBeans)
                        sw.WriteLine($"            var.dynamicBeans.put({real.Key}L, \"{real.Value.FullName}\");");
                }
                else if (v.VariableType is TypeGTable table)
                {
                    sw.WriteLine($"            var.keyName = \"{GetFullName(table.RowKeyType)},{GetFullName(table.ColKeyType)}\";");
                    sw.WriteLine($"            var.valueName = \"{GetFullName(table.ValueType)}\";");
                }

                sw.WriteLine($"            bean.addVariable(var);");
                sw.WriteLine($"        }}");
            }
            sw.WriteLine($"        addBean(bean);");
            sw.WriteLine($"    }}");
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    /*
    public class FollowApplyTables
    {
        public static void GenFollowerApplyTablesLogFactoryRegister(Project project, string genDir, HashSet<Types.Type> dependsFollowerApplyTables)
        {
            using StreamWriter sw = project.Solution.OpenWriter(genDir, "FollowerApplyTables.java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated");
            sw.WriteLine("package " + project.Solution.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("import Zeze.History.Helper;");
            sw.WriteLine();
            sw.WriteLine("public class FollowerApplyTables {");
            sw.WriteLine("    public static void registerLog() {");

            var tlogs = new HashSet<string>();
            foreach (var dep in dependsFollowerApplyTables)
            {
                if (dep.IsCollection)
                {
                    tlogs.Add(GetCollectionLogTemplateName(dep));
                    continue;
                }
                if (dep.IsKeyable && dep.IsBean) // beankey
                {
                    tlogs.Add($"Helper.registerLogBeanKey({TypeName.GetName(dep)}.class)");
                    continue;
                }
            }
            var sorted = tlogs.ToArray();
            Array.Sort(sorted);
            sw.WriteLine($"        Helper.registerLogs();");
            foreach (var tlog in sorted)
            {
                sw.WriteLine($"        {tlog};");
            }
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        private static string GetCollectionLogTemplateName(Types.Type type)
        {
            if (type is Types.TypeList tlist)
            {
                var is2 = tlist.ValueType.IsNormalBeanOrRocks;
                string value = is2 ? java.TypeName.GetName(tlist.ValueType) : BoxingName.GetBoxingName(tlist.ValueType);
                return "Helper.registerLogList" + (is2 ? "2(" : "1(") + value + ".class)";
            }
            else if (type is Types.TypeSet tset)
            {
                string value = java.BoxingName.GetBoxingName(tset.ValueType);
                return "Helper.registerLogSet1(" + value + ".class)";
            }
            else if (type is Types.TypeMap tmap)
            {
                string key = java.BoxingName.GetBoxingName(tmap.KeyType);
                string value = java.TypeName.GetName(tmap.ValueType);
                var version = tmap.ValueType.IsNormalBeanOrRocks ? "2(" : "1(";
                return $"Helper.registerLogMap{version}{key}.class, {value}.class)";
            }
            throw new System.Exception();
        }
    }
    */
}

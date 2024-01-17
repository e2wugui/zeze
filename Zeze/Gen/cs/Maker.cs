using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Maker
    {
        public Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        public void Make()
        {
            string projectBasedir = Project.GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string genCommonDir = string.IsNullOrEmpty(Project.GenCommonRelativeDir)
                ? genDir : Path.Combine(projectDir, Project.GenCommonRelativeDir, "Gen");

            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;

            Program.AddGenDir(genDir);

            foreach (Types.Bean bean in Project.AllBeans.Values)
                new BeanFormatter(bean).Make(genCommonDir);
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                new BeanKeyFormatter(beanKey).Make(genCommonDir);
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir);
            }
            var MappingClassBeans = new HashSet<Bean>();
            foreach (Module mod in Project.AllOrderDefineModules)
            {
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
                // 收集需要生成类映射的Bean。
                foreach (var bean in mod.MappingClassBeans)
                    MappingClassBeans.Add(bean);
            }
            foreach (Service ma in Project.Services.Values)
                new ServiceFormatter(ma, genDir, srcDir).Make();
            foreach (Table table in Project.AllTables.Values)
            {
                if (Project.GenTables.Contains(table.Gen))
                    new TableFormatter(table, genCommonDir).Make();
            }
            new Schemas(Project, genDir).Make();

            new App(Project, genDir, srcDir).Make();

            if (Project.MappingClass)
            {
                foreach (var bean in MappingClassBeans)
                {
                    new MappingClass(genDir, srcDir, bean).Make();
                }
            }
        }

        public void MakeConfCsNet(HashSet<Types.Type> dependsFollowerApplyTables)
        {
            string projectBasedir = Project.GenDir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, Project.GenRelativeDir, "Gen");
            string genCommonDir = string.IsNullOrEmpty(Project.GenCommonRelativeDir)
                ? genDir : Path.Combine(projectDir, Project.GenCommonRelativeDir, "Gen");

            string srcDir = Project.ScriptDir.Length > 0
                ? Path.Combine(projectDir, Project.ScriptDir) : projectDir;

            Program.AddGenDir(genDir);

            // 不生成table
            var savedGenTables = Project.GenTables;
            Project.GenTables = new();

            foreach (Types.Bean bean in Project.AllBeans.Values)
                new confcs.BeanFormatter(Project, bean, true).Make(genCommonDir);
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                new BeanKeyFormatter(beanKey).Make(genCommonDir);

            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir, true);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir, true);
            }
            foreach (Module mod in Project.AllOrderDefineModules)
                new ModuleFormatter(Project, mod, genDir, srcDir).Make();
            foreach (Service ma in Project.Services.Values)
                new ServiceFormatter(ma, genDir, srcDir).Make();
            new App(Project, genDir, srcDir, true).Make(true);

            Project.GenTables = savedGenTables;
            GenFollowerApplyTablesLogFactoryRegister(genDir, dependsFollowerApplyTables);
        }

        public void GenFollowerApplyTablesLogFactoryRegister(string genDir, HashSet<Types.Type> dependsFollowerApplyTables)
        {
            using StreamWriter sw = Project.Solution.OpenWriter(genDir, "FollowerApplyTables.cs");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("using Zeze.Transaction;");
            sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine();
            sw.WriteLine("namespace " + Project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public class FollowerApplyTables");
            sw.WriteLine("    {");
            sw.WriteLine("        public static void RegisterLog()");
            sw.WriteLine("        {");

            var tlogs = new HashSet<string>();
            foreach (var dep in dependsFollowerApplyTables)
            {
                if (dep.IsCollection)
                {
                    tlogs.Add(GetCollectionLogTemplateName(dep));
                    continue;
                }
                if (dep is TypeDynamic)
                {
                    tlogs.Add($"Zeze.Util.LogConfDynamic");
                    continue;
                }
                if (dep.IsNormalBeanOrRocks)
                    continue;
                tlogs.Add($"Log<{TypeName.GetName(dep)}>");
            }
            var sorted = tlogs.ToArray();
            Array.Sort(sorted);
            foreach (var tlog in sorted)
            {
                sw.WriteLine($"            Log.Register<{tlog}>();");
            }

            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        private string GetCollectionLogTemplateName(Types.Type type)
        {
            if (type is Types.TypeList tlist)
            {
                string value = rrcs.TypeName.GetName(tlist.ValueType);
                return "LogList" + (tlist.ValueType.IsNormalBeanOrRocks ? "2<" : "1<") + value + ">";
            }
            else if (type is Types.TypeSet tset)
            {
                string value = rrcs.TypeName.GetName(tset.ValueType);
                return "LogSet1<" + value + ">";
            }
            else if (type is Types.TypeMap tmap)
            {
                string key = rrcs.TypeName.GetName(tmap.KeyType);
                string value = rrcs.TypeName.GetName(tmap.ValueType);
                var version = tmap.ValueType.IsNormalBeanOrRocks ? "2<" : "1<";
                return $"LogMap{version}{key}, {value}>";
            }
            throw new System.Exception();
        }
    }
}

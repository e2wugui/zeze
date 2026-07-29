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
            var genCommonDir = string.IsNullOrEmpty(Project.CommonDir) ? Project.GenDir : Project.CommonDir;
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            Program.ParallelEach(Project.AllBeans.Values,
                bean => new BeanFormatter(bean).Make(genCommonDir));
            Program.ParallelEach(Project.AllBeanKeys.Values,
                beanKey => new BeanKeyFormatter(beanKey).Make(genCommonDir));
            Program.ParallelEach(Project.AllProtocols.Values, protocol =>
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir);
            });
            Program.ParallelEach(Project.AllOrderDefineModules,
                mod => new ModuleFormatter(Project, mod, genDir, srcDir).Make());
            // 收集需要生成类映射的Bean（mod.MappingClassBeans 已在 Compile 期填好、此处只读，串行收集即可）
            var MappingClassBeans = new HashSet<Bean>();
            foreach (Module mod in Project.AllOrderDefineModules)
            {
                foreach (var bean in mod.MappingClassBeans)
                    MappingClassBeans.Add(bean);
            }
            Program.ParallelEach(Project.Services.Values,
                ma => new ServiceFormatter(ma, genDir, srcDir).Make());
            Program.ParallelEach(Project.AllTables.Values, table =>
            {
                if (Project.GenTables.Contains(table.Gen))
                    new TableFormatter(table, genCommonDir).Make();
            });
            new Schemas(Project, genDir).Make();

            new App(Project, genDir, srcDir).Make();

            if (Project.MappingClass)
            {
                Program.ParallelEach(MappingClassBeans,
                    bean => new MappingClass(genDir, srcDir, bean).Make());
            }
        }

        public void MakeConfCsNet(HashSet<Types.Type> dependsFollowerApplyTables)
        {
            var genCommonDir = string.IsNullOrEmpty(Project.CommonDir) ? Project.GenDir : Project.CommonDir;
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;
            if (!Project.DisableDeleteGen)
                Program.AddGenDir(genDir);

            // 不生成table
            var savedGenTables = Project.GenTables;
            Project.GenTables = new();

            Program.ParallelEach(Project.AllBeans.Values,
                bean => new confcs.BeanFormatter(Project, bean, true).Make(genCommonDir));
            Program.ParallelEach(Project.AllBeanKeys.Values,
                beanKey => new BeanKeyFormatter(beanKey).Make(genCommonDir));

            Program.ParallelEach(Project.AllProtocols.Values, protocol =>
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genCommonDir, true);
                else
                    new ProtocolFormatter(protocol).Make(genCommonDir, true);
            });
            Program.ParallelEach(Project.AllOrderDefineModules,
                mod => new ModuleFormatter(Project, mod, genDir, srcDir).Make());
            Program.ParallelEach(Project.Services.Values,
                ma => new ServiceFormatter(ma, genDir, srcDir).Make());
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
            else if (type is Types.TypeSortedMap tsmap)
            {
                string key = rrcs.TypeName.GetName(tsmap.KeyType);
                string value = rrcs.TypeName.GetName(tsmap.ValueType);
                var version = tsmap.ValueType.IsNormalBeanOrRocks ? "2<" : "1<";
                return $"LogSortedMap{version}{key}, {value}>";
            }
            throw new System.Exception();
        }
    }
}

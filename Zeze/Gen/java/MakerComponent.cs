using System.Collections.Generic;
using System.IO;

namespace Zeze.Gen.java
{
    public class MakerComponent
    {
        public Project Project { get; }

        public MakerComponent(Project project)
        {
            Project = project;
        }

        public void Make()
        {
            string projectBasedir = Project.Gendir;
            string genDir = projectBasedir; // 公共类（Bean，Protocol，Rpc，Table）生成目录。
            var relativeSrcDir = string.IsNullOrEmpty(Project.GenRelativeDir) ? "Zeze/Component" : Project.GenRelativeDir;
            string srcDir = Path.Combine(projectBasedir, relativeSrcDir); // 生成源代码全部放到同一个目录下。
            Directory.CreateDirectory(srcDir);
            Directory.CreateDirectory(genDir);
            foreach (Types.Bean bean in Project.AllBeans.Values)
            {
                if (bean.IsRocks)
                    new rrjava.BeanFormatter(bean).Make(genDir);
                else
                    new BeanFormatter(bean).Make(genDir);
            }
            foreach (Types.BeanKey beanKey in Project.AllBeanKeys.Values)
                new BeanKeyFormatter(beanKey).Make(genDir);
            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new RpcFormatter(rpc).Make(genDir);
                else
                    new ProtocolFormatter(protocol).Make(genDir);
            }
            foreach (Table table in Project.AllTables.Values)
            {
                if (Project.GenTables.Contains(table.Gen))
                {
                    if (false == table.IsRocks)
                    {
                        new TableFormatter(table, genDir).Make();
                    }
                }
            }

            var ns = "";
            foreach (var dir in relativeSrcDir.Split(new char[] { '/', '\\' }))
            {
                if (!string.IsNullOrEmpty(ns))
                    ns += ".";
                ns += dir;
            }

            var mfs = new List<ModuleFormatter>();
            foreach (Module mod in Project.AllOrderDefineModules)
                mfs.Add(new ModuleFormatter(Project, mod, genDir, srcDir));
            var baseFileName = Path.Combine(srcDir, "Abstract" + Project.Name + ".java");
            {
                using StreamWriter sw = Program.OpenStreamWriter(baseFileName);

                sw.WriteLine("// auto-generated @formatter:off");
                sw.WriteLine($"package {ns};");
                sw.WriteLine();

                sw.WriteLine($"public abstract class Abstract{Project.Name} {{");
                foreach (var mf in mfs) mf.GenEnums(sw, mfs.Count > 1 ? mf.module.Name : "");
                foreach (var mf in mfs) mf.DefineZezeTables(sw);

                sw.WriteLine("    public void RegisterProtocols(Zeze.Net.Service service) {");
                foreach (var mf in mfs) mf.RegisterProtocols(sw, "service");
                sw.WriteLine("    }");
                sw.WriteLine();

                sw.WriteLine("    public void UnRegisterProtocols(Zeze.Net.Service service) {");
                foreach (var mf in mfs) mf.UnRegisterProtocols(sw, "service");
                sw.WriteLine("    }");
                sw.WriteLine();

                sw.WriteLine("    public void RegisterZezeTables(Zeze.Application zeze) {");
                foreach (var mf in mfs) mf.RegisterZezeTables(sw, "zeze");
                sw.WriteLine("    }");
                sw.WriteLine();

                sw.WriteLine("    public void UnRegisterZezeTables(Zeze.Application zeze) {");
                foreach (var mf in mfs) mf.UnRegisterZezeTables(sw, "zeze");
                sw.WriteLine("    }");
                sw.WriteLine();

                sw.WriteLine("    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {");
                foreach (var mf in mfs) mf.RegisterRocksTables(sw);
                sw.WriteLine("    }");

                // gen abstract protocol handles
                // 如果模块嵌套，仅传入Module.Name不够。但一般够用了。
                foreach (var mf in mfs) mf.GenAbstractProtocolHandles(sw, mfs.Count > 1 ? mf.module.Name : "", false);

                sw.WriteLine("}");
            }
            var srcFileName = Path.Combine(srcDir, Project.Name + ".java");
            if (!File.Exists(srcFileName))
            {
                using StreamWriter sw = Program.OpenStreamWriter(srcFileName);
                sw.WriteLine($"package {ns};");
                sw.WriteLine();
                sw.WriteLine($"public class {Project.Name} extends Abstract{Project.Name} {{");
                foreach (var mf in mfs) mf.GenEmptyProtocolHandles(sw, mfs.Count > 1 ? mf.module.Name : "", false);
                sw.WriteLine($"}}");
            }
        }
    }
}

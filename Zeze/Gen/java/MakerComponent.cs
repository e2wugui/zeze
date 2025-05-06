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

        public Module GetPresentModule(List<ModuleFormatter> mfs)
        {
            if (string.IsNullOrEmpty(Project.ComponentPresentModuleFullName))
            {
                if (mfs.Count > 1)
                    throw new System.Exception("");
                return mfs[0].module;
            }
            foreach (var mf in mfs)
            {
                if (mf.module.FullName.Equals(Project.ComponentPresentModuleFullName))
                {
                    return mf.module;
                }
            }
            throw new System.Exception($"{Project.ComponentPresentModuleFullName} Not Found In Depends.");
        }

        public void Make()
        {
            var genDir = Project.GenDir;
            var srcDir = Project.SrcDir;
            Program.AddGenDir(Path.Combine(genDir, "Zeze", "Builtin")); // 组件自动删除Bean等，必须特殊处理。

            Directory.CreateDirectory(srcDir);
            Directory.CreateDirectory(genDir);
            foreach (Types.Bean bean in Project.AllBeans.Values)
            {
                if (bean.IsRocks)
                    new rrjava.BeanFormatter(bean).Make(genDir);
                else
                    new BeanFormatter(bean).Make(genDir, Project);
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
            foreach (var dir in Project.PackagePath.Split(new char[] { '/', '\\' }))
            {
                if (!string.IsNullOrEmpty(ns))
                    ns += ".";
                ns += dir;
            }

            var mfs = new List<ModuleFormatter>();
            foreach (Module mod in Project.AllOrderDefineModules)
                mfs.Add(new ModuleFormatter(Project, mod, genDir, srcDir));
            {
                using var sw = Program.OpenStreamWriter(Path.Combine(srcDir, Project.PackagePath, "Abstract" + Project.Name + ".java"));
                sw.WriteLine("// auto-generated @formatter:off");
                sw.WriteLine($"package {ns};");
                sw.WriteLine();
                var presentModule = GetPresentModule(mfs);
                var classBase = (!Project.EnableBase || string.IsNullOrEmpty(presentModule.ClassBase))
                ? "" : $"extends {presentModule.ClassBase} ";
                sw.WriteLine($"public abstract class Abstract{Project.Name} {classBase}implements Zeze.IModule {{");
                sw.WriteLine($"    public static final int ModuleId = {presentModule.Id};");
                sw.WriteLine($"    public static final String ModuleName = \"{Project.Name}\";");
                sw.WriteLine($"    public static final String ModuleFullName = \"{ns + "." + Project.Name}\";");
                sw.WriteLine();
                sw.WriteLine($"    @Override public int getId() {{ return ModuleId; }}");
                sw.WriteLine($"    @Override public String getName() {{ return ModuleName; }}");
                sw.WriteLine($"    @Override public String getFullName() {{ return ModuleFullName; }}");
                sw.WriteLine($"    @Override public boolean isBuiltin() {{ return true; }}");
                sw.WriteLine();
                sw.WriteLine($"    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();");
                sw.WriteLine($"    @Override public void lock() {{ __thisLock.lock(); }}");
                sw.WriteLine($"    @Override public void unlock() {{ __thisLock.unlock(); }}");
                sw.WriteLine($"    @Override public java.util.concurrent.locks.Lock getLock() {{ return __thisLock; }}");

                foreach (var mf in mfs) mf.GenEnums(sw);
                foreach (var mf in mfs) mf.DefineZezeTables(sw);

                sw.WriteLine();
                sw.WriteLine("    public void RegisterProtocols(Zeze.Net.Service service) {");
                for (var i = 0; i < mfs.Count; ++i) mfs[i].RegisterProtocols(sw, i == 0, "service");
                sw.WriteLine("    }");

                sw.WriteLine();
                sw.WriteLine("    public static void UnRegisterProtocols(Zeze.Net.Service service) {");
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
                sw.WriteLine("    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {");
                foreach (var mf in mfs) mf.RegisterRocksTables(sw);
                sw.WriteLine("    }");

                bool writtenHeader = false; // 不需要HttpServlet时不需要依赖Netty
                foreach (var mf in mfs) mf.RegisterHttpServlet(sw, ref writtenHeader);
                if (writtenHeader)
                    sw.WriteLine("    }");

                // gen abstract protocol handles
                // 如果模块嵌套，仅传入Module.Name不够。但一般够用了。
                foreach (var mf in mfs) mf.GenAbstractProtocolHandles(sw);

                foreach (var mf in mfs) mf.GenAbstractHttpHandles(sw);

                sw.WriteLine("}");
            }
            {
                var fileName = Path.Combine(srcDir, Project.PackagePath, Project.Name + ".java");
                if (!File.Exists(fileName))
                {
                    using var sw = Program.OpenStreamWriter(fileName, false);
                    if (sw != null)
                    {
                        sw.WriteLine($"package {ns};");
                        sw.WriteLine();
                        sw.WriteLine($"public class {Project.Name} extends Abstract{Project.Name} {{");
                        foreach (var mf in mfs) mf.GenEmptyProtocolHandles(sw, false);
                        sw.WriteLine($"}}");
                    }
                }
            }
        }
    }
}

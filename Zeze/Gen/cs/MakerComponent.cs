using System.Collections.Generic;
using System.IO;

namespace Zeze.Gen.cs
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
            string projectBasedir = Project._GenDir;
            string genDir = projectBasedir; // 公共类（Bean，Protocol，Rpc，Table）生成目录。

            var relativeSrcDir = string.IsNullOrEmpty(Project.GenRelativeDir) ? "Zeze/Component" : Project.GenRelativeDir;
            string srcDir = Path.Combine(projectBasedir, relativeSrcDir); // 生成源代码全部放到同一个目录下。

            if (Project.IsNewVersionDir())
            {
                genDir = Project.GenDir;
                srcDir = Project.SrcDir;
                if (!Project.DisableDeleteGen)
                    Program.AddGenDir(genDir);
            }
            else if (!Project.DisableDeleteGen)
                Program.AddGenDir(Path.Combine(genDir, "Zeze", "Builtin"));

            Directory.CreateDirectory(srcDir);
            Directory.CreateDirectory(genDir);
            foreach (Types.Bean bean in Project.AllBeans.Values)
            {
                if (bean.IsRocks)
                    new rrcs.BeanFormatter(bean).Make(genDir);
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
            var baseFileName = Path.Combine(srcDir, "Abstract" + Project.Name + ".cs");
            {
                using StreamWriter sw = Program.OpenStreamWriter(baseFileName);
                if (sw != null)
                {
                    sw.WriteLine("// auto generate");
                    sw.WriteLine();
                    sw.WriteLine("// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable");
                    sw.WriteLine("// ReSharper disable once CheckNamespace");
                    sw.WriteLine($"namespace {ns}");
                    sw.WriteLine("{");
                    sw.WriteLine($"    public abstract class Abstract{Project.Name} : Zeze.IModule ");
                    sw.WriteLine("    {");
                    var presentModule = GetPresentModule(mfs);
                    sw.WriteLine($"        public const int ModuleId = {presentModule.Id};");
                    sw.WriteLine($"        public override string FullName => \"{ns + "." + Project.Name}\";");
                    sw.WriteLine($"        public override string Name => \"{Project.Name}\";");
                    sw.WriteLine($"        public override int Id => ModuleId;");
                    sw.WriteLine($"        public override bool IsBuiltin => true;");
                    sw.WriteLine();

                    foreach (var mf in mfs) mf.GenEnums(sw);
                    foreach (var mf in mfs) mf.DefineZezeTables(sw);
                    sw.WriteLine();

                    sw.WriteLine("        public void RegisterProtocols(Zeze.Net.Service service)");
                    sw.WriteLine("        {");
                    for (var i = 0; i < mfs.Count; ++i) mfs[i].RegisterProtocols(sw, i == 0, "service");
                    sw.WriteLine("        }");
                    sw.WriteLine();

                    sw.WriteLine("        public void UnRegisterProtocols(Zeze.Net.Service service)");
                    sw.WriteLine("        {");
                    foreach (var mf in mfs) mf.UnRegisterProtocols(sw, "service");
                    sw.WriteLine("        }");
                    sw.WriteLine();

                    sw.WriteLine("        public void RegisterZezeTables(Zeze.Application zeze)");
                    sw.WriteLine("        {");
                    foreach (var mf in mfs) mf.RegisterZezeTables(sw, "zeze");
                    sw.WriteLine("        }");
                    sw.WriteLine();

                    sw.WriteLine("        public void UnRegisterZezeTables(Zeze.Application zeze)");
                    sw.WriteLine("        {");
                    foreach (var mf in mfs) mf.UnRegisterZezeTables(sw, "zeze");
                    sw.WriteLine("        }");
                    sw.WriteLine();

                    sw.WriteLine("        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)");
                    sw.WriteLine("        {");
                    foreach (var mf in mfs) mf.RegisterRocksTables(sw);
                    sw.WriteLine("        }");
                    sw.WriteLine();

                    // gen abstract protocol handles
                    // 如果模块嵌套，仅传入Module.Name不够。但一般够用了。
                    foreach (var mf in mfs) mf.GenAbstractProtocolHandles(sw);

                    sw.WriteLine("    }");
                    sw.WriteLine("}");
                }
            }
            var srcFileName = Path.Combine(srcDir, Project.Name + ".cs");
            if (!File.Exists(srcFileName))
            {
                using StreamWriter sw = Program.OpenStreamWriter(srcFileName);
                if (sw != null)
                {
                    sw.WriteLine();
                    sw.WriteLine($"namespace {ns}");
                    sw.WriteLine($"{{");
                    sw.WriteLine($"    public class {Project.Name} : Abstract{Project.Name}");
                    sw.WriteLine($"    {{");
                    foreach (var mf in mfs) mf.GenEmptyProtocolHandles(sw, false);
                    sw.WriteLine($"    }}");
                    sw.WriteLine($"}}");
                }
            }
        }
    }
}

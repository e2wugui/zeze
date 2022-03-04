using System.IO;

namespace Zeze.Gen.rrcs
{
    public class App
    {
        readonly Project project;
        readonly string genDir;
        readonly string srcDir;

        public App(Project project, string genDir, string srcDir)
        {
            this.project = project;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakePartialGen();
            MakePartial();
        }

        public void MakePartialGen()
        {
            using StreamWriter sw = project.Solution.OpenWriter(genDir, "App.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public static App Instance { get; } = new App();");
            sw.WriteLine();
            sw.WriteLine("        public Zeze.Raft.RocksRaft.Rocks Rocks { get; set; }");
            sw.WriteLine();

            foreach (var table in project.AllTables.Values)
            {
                string key = TypeName.GetName(table.KeyType);
                string value = TypeName.GetName(table.ValueType);
                sw.WriteLine($"        public Zeze.Raft.RocksRaft.Table<{key}, {value}> {table.Name} {{ get; private set; }}");
            }

            sw.WriteLine();

            sw.WriteLine("        public void Create(string raftName = null, Zeze.Raft.RaftConfig raftConfig = null, Zeze.Config config = null)");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            sw.WriteLine("                if (Rocks != null)");
            sw.WriteLine("                    return;");
            sw.WriteLine();
            sw.WriteLine($"                Rocks = new Zeze.Raft.RocksRaft.Rocks(raftName, raftConfig, config);");
            foreach (var table in project.AllTables.Values)
            {
                string key = TypeName.GetName(table.KeyType);
                string value = TypeName.GetName(table.ValueType);
                sw.WriteLine($"                {table.Name} = Rocks.OpenTable<{key}, {value}>(\"{table.Space.Path("_", table.Name)}\");");
            }
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void Destroy()");
            sw.WriteLine("        {");
            sw.WriteLine("            lock(this)");
            sw.WriteLine("            {");
            sw.WriteLine("                Rocks?.Dispose();");
            sw.WriteLine("                Rocks = null;");
            sw.WriteLine("            }");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void StartService()");
            sw.WriteLine("        {");
            sw.WriteLine("            Rocks.Raft.Server.Start()");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void StopService()");
            sw.WriteLine("        {");
            sw.WriteLine("            Rocks.Raft.Server.Stop()");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void MakePartial()
        {
            using System.IO.StreamWriter sw = project.Solution.OpenWriter(srcDir, "App.cs", false);
            if (sw == null)
                return;

            sw.WriteLine();
            sw.WriteLine("namespace " + project.Solution.Path());
            sw.WriteLine("{");
            sw.WriteLine("    public sealed partial class App");
            sw.WriteLine("    {");
            sw.WriteLine("        public void Start()");
            sw.WriteLine("        {");
            sw.WriteLine("            Create();");
            sw.WriteLine("            StartService(); // 启动网络");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public void Stop()");
            sw.WriteLine("        {");
            sw.WriteLine("            StopService(); // 关闭网络");
            sw.WriteLine("            Destroy();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

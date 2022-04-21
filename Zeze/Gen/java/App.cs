using System;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.java
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
            FileChunkGen fcg = new();
            string fullDir = project.Solution.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, $"App.java");
            if (fcg.LoadFile(fullFileName))
            {
                fcg.SaveFile(fullFileName, GenChunkByName);
                return;
            }
            // new file
            FileSystem.CreateDirectory(fullDir);
            using StreamWriter sw = Program.OpenStreamWriter(fullFileName);

            sw.WriteLine("package " + project.Solution.Path() + ";");
            sw.WriteLine();
            // sw.WriteLine(fcg.ChunkStartTag + " " + ChunkNameImport + " @formatter:off");
            // ImportGen(sw);
            // sw.WriteLine(fcg.ChunkEndTag + " " + ChunkNameImport + " @formatter:on");
            // sw.WriteLine();
            sw.WriteLine("public class App extends Zeze.AppBase {");
            sw.WriteLine("    public static App Instance = new App();");
            sw.WriteLine("    public static App getInstance() {");
            sw.WriteLine("        return Instance;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void Start() throws Throwable {");
            sw.WriteLine("        CreateZeze();");
            sw.WriteLine("        CreateService();");
            sw.WriteLine("        CreateModules();");
            sw.WriteLine("        Zeze.Start(); // 启动数据库");
            sw.WriteLine("        StartModules(); // 启动模块，装载配置什么的。");
            sw.WriteLine("        StartService(); // 启动网络");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void Stop() throws Throwable {");
            sw.WriteLine("        StopService(); // 关闭网络");
            sw.WriteLine("        StopModules(); // 关闭模块，卸载配置什么的。");
            sw.WriteLine("        Zeze.Stop(); // 关闭数据库");
            sw.WriteLine("        DestroyModules();");
            sw.WriteLine("        DestroyServices();");
            sw.WriteLine("        DestroyZeze();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    " + fcg.ChunkStartTag + " " + ChunkNameAppGen + " @formatter:off");
            AppGen(sw);
            sw.WriteLine("    " + fcg.ChunkEndTag + " " + ChunkNameAppGen + " @formatter:on");
            sw.WriteLine("}");
        }

        const string ChunkNameAppGen = "GEN APP";
        const string ChunkNameImport = "IMPORT GEN";

        void GenChunkByName(StreamWriter writer, FileChunkGen.Chunk chunk)
        {
            switch (chunk.Name)
            {
                case ChunkNameAppGen:
                    AppGen(writer);
                    break;
                case ChunkNameImport:
                    ImportGen(writer);
                    break;
                default:
                    throw new Exception("unknown Chunk.Name=" + chunk.Name);
            }
        }

        void ImportGen(StreamWriter sw)
        {
            // sw.WriteLine("import java.util.*;");
        }

        void AppGen(StreamWriter sw)
        {
            sw.WriteLine("    public Zeze.Application Zeze;");
            sw.WriteLine("    public final java.util.HashMap<String, Zeze.IModule> Modules = new java.util.HashMap<>();");
            sw.WriteLine();

            foreach (Service m in project.Services.Values)
                sw.WriteLine("    public " + m.FullName + " " + m.Name + ";");
            if (project.Services.Count > 0)
                sw.WriteLine();

            foreach (Module m in project.AllOrderDefineModules)
            {
                string moduleName = string.Concat(m.Name[..1].ToUpper(), m.Name.AsSpan(1));
                var fullname = m.Path("_");
                sw.WriteLine($"    public {m.Path(".", $"Module{moduleName}")} {fullname};");
            }
            if (project.AllOrderDefineModules.Count > 0)
                sw.WriteLine();

            sw.WriteLine("    @Override");
            sw.WriteLine("    public Zeze.Application getZeze() {");
            sw.WriteLine("        return Zeze;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void CreateZeze() throws Throwable {");
            sw.WriteLine("        CreateZeze(null);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {");
            sw.WriteLine("        if (Zeze != null)");
            sw.WriteLine("            throw new RuntimeException(\"Zeze Has Created!\");");
            sw.WriteLine();
            sw.WriteLine($"        Zeze = new Zeze.Application(\"{project.Solution.Name}\", config);");
            sw.WriteLine("    }");
            sw.WriteLine("");
            sw.WriteLine("    public synchronized void CreateService() throws Throwable {");
            sw.WriteLine("");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + " = new " + m.FullName + "(Zeze);");
            sw.WriteLine("    }");

            sw.WriteLine("    public synchronized void CreateModules() {");
            foreach (Module m in project.AllOrderDefineModules)
            {
                string moduleName = string.Concat(m.Name[..1].ToUpper(), m.Name.AsSpan(1));
                var fullname = m.Path("_");
                sw.WriteLine("        " + fullname + " = new " + m.Path(".", $"Module{moduleName}") + "(this);");
                sw.WriteLine($"        {fullname}.Initialize(this);");
                sw.WriteLine($"        {fullname} = ({m.Path(".", $"Module{moduleName}")})ReplaceModuleInstance({fullname});");
                sw.WriteLine($"        if (Modules.put({fullname}.getFullName(), {fullname}) != null)");
                sw.WriteLine($"            throw new RuntimeException(\"duplicate module name: {fullname}\");");
                sw.WriteLine();
            }
            sw.WriteLine("        Zeze.setSchemas(new " + project.Solution.Path(".", "Schemas") + "());");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void DestroyModules() {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                var fullname = m.Path("_");
                sw.WriteLine("        " + fullname + " = null;");
            }
            sw.WriteLine("        Modules.clear();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void DestroyServices() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + " = null;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void DestroyZeze() {");
            sw.WriteLine("        Zeze = null;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void StartModules() throws Throwable {");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine("        " + m.Path("_") + ".Start(this);");
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        " + m.Path("_") + ".Start(this);");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void StopModules() throws Throwable {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (!project.ModuleStartOrder.Contains(m))
                {
                    var name = m.Path("_");
                    sw.WriteLine("        if (" + name + " != null)");
                    sw.WriteLine("            " + name + ".Stop(this);");
                }
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var name = project.ModuleStartOrder[i].Path("_");
                sw.WriteLine("        if (" + name + " != null)");
                sw.WriteLine("            " + name + ".Stop(this);");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void StartService() throws Throwable {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + ".Start();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void StopService() throws Throwable {");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        if (" + m.Name + " != null)");
                sw.WriteLine("            " + m.Name + ".Stop();");
            }
            sw.WriteLine("    }");
        }
    }
}

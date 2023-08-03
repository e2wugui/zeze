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
            if (sw == null)
                return;

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
            sw.WriteLine("    public void Start() throws Exception {");
            sw.WriteLine("        createZeze();");
            sw.WriteLine("        createService();");
            sw.WriteLine("        createModules();");
            sw.WriteLine("        Zeze.start(); // 启动数据库");
            sw.WriteLine("        startModules(); // 启动模块，装载配置什么的。");
            sw.WriteLine("        startService(); // 启动网络");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void Stop() throws Exception {");
            sw.WriteLine("        stopService(); // 关闭网络");
            sw.WriteLine("        stopModules(); // 关闭模块，卸载配置什么的。");
            sw.WriteLine("        Zeze.stop(); // 关闭数据库");
            sw.WriteLine("        destroyModules();");
            sw.WriteLine("        destroyServices();");
            sw.WriteLine("        destroyZeze();");
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
            if (project.Hot)
            {
                AppGenHot(sw);
                return;
            }

            sw.WriteLine("    public Zeze.Application Zeze;");
            sw.WriteLine("    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();");
            sw.WriteLine();

            foreach (Service m in project.Services.Values)
                sw.WriteLine("    public " + m.FullName + " " + m.Name + ";");
            if (project.Services.Count > 0)
                sw.WriteLine();

            foreach (Module m in project.AllOrderDefineModules)
            {
                string moduleName = Program.Upper1(m.Name);
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
            sw.WriteLine("    public void createZeze() throws Exception {");
            sw.WriteLine("        createZeze(null);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void createZeze(Zeze.Config config) throws Exception {");
            sw.WriteLine("        if (Zeze != null)");
            sw.WriteLine("            throw new IllegalStateException(\"Zeze Has Created!\");");
            sw.WriteLine();
            sw.WriteLine($"        Zeze = new Zeze.Application(\"{project.Name}\", config);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void createService() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + " = new " + m.FullName + "(Zeze);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void createModules() throws Exception {");
            sw.WriteLine("        Zeze.initialize(this);");
            if (project.AllOrderDefineModules.Count > 0)
            {
                sw.WriteLine("        var _modules_ = createRedirectModules(new Class[] {");
                foreach (Module m in project.AllOrderDefineModules)
                    sw.WriteLine("            " + m.Path(".", "Module" + Program.Upper1(m.Name)) + ".class,");
                sw.WriteLine("        });");
                sw.WriteLine("        if (_modules_ == null)");
                sw.WriteLine("            return;");
                sw.WriteLine();
                int index = 0;
                foreach (Module m in project.AllOrderDefineModules)
                {
                    string className = m.Path(".", "Module" + Program.Upper1(m.Name));
                    var fullname = m.Path("_");
                    sw.WriteLine($"        {fullname} = ({className})_modules_[" + index + "];");
                    sw.WriteLine($"        {fullname}.Initialize(this);");
                    sw.WriteLine($"        if (modules.put({fullname}.getFullName(), {fullname}) != null)");
                    sw.WriteLine($"            throw new IllegalStateException(\"duplicate module name: {fullname}\");");
                    sw.WriteLine();
                    index++;
                }
            }
            sw.WriteLine("        Zeze.setSchemas(new " + project.Solution.Path(".", "Schemas") + "());");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void destroyModules() {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                var fullname = m.Path("_");
                sw.WriteLine("        " + fullname + " = null;");
            }
            sw.WriteLine("        modules.clear();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void destroyServices() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + " = null;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void destroyZeze() {");
            sw.WriteLine("        Zeze = null;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void startModules() throws Exception {");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine("        " + m.Path("_") + ".Start(this);");
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        " + m.Path("_") + ".Start(this);");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void stopModules() throws Exception {");
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
            sw.WriteLine("    public synchronized void startService() throws Exception {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + ".start();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void stopService() throws Exception {");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        if (" + m.Name + " != null)");
                sw.WriteLine("            " + m.Name + ".stop();");
            }
            sw.WriteLine("    }");
        }

        void AppGenHot(StreamWriter sw)
        {
            sw.WriteLine("    public Zeze.Application Zeze;");
            sw.WriteLine("    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();");
            sw.WriteLine("    public Zeze.Hot.HotManager HotManager;");
            sw.WriteLine();

            foreach (Service m in project.Services.Values)
                sw.WriteLine("    public " + m.FullName + " " + m.Name + ";");
            if (project.Services.Count > 0)
                sw.WriteLine();

            sw.WriteLine("    @Override");
            sw.WriteLine("    public Zeze.Application getZeze() {");
            sw.WriteLine("        return Zeze;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void createZeze() throws Exception {");
            sw.WriteLine("        createZeze(null);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void createZeze(Zeze.Config config) throws Exception {");
            sw.WriteLine("        if (Zeze != null)");
            sw.WriteLine("            throw new IllegalStateException(\"Zeze Has Created!\");");
            sw.WriteLine();
            sw.WriteLine($"        Zeze = new Zeze.Application(\"{project.Name}\", config);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void createService() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + " = new " + m.FullName + "(Zeze);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void createModules() throws Exception {");
            sw.WriteLine("        Zeze.initialize(this);");
            sw.WriteLine("        HotManager = new Zeze.Hot.HotManager(this, Zeze.getConfig().getHotWorkingDir(), Zeze.getConfig().getHotDistributeDir());");
            sw.WriteLine("        HotManager.initialize(modules);");
            sw.WriteLine("        Zeze.setSchemas(new " + project.Solution.Path(".", "Schemas") + "());");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void destroyModules() {");
            sw.WriteLine("        if (null != HotManager) {");
            sw.WriteLine("            HotManager.destroyModules();");
            sw.WriteLine("            modules.clear();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void destroyServices() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + " = null;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void destroyZeze() {");
            sw.WriteLine("        Zeze = null;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void startModules() throws Exception {");
            sw.WriteLine("        var definedOrder = new java.util.ArrayList<String>();");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine($"        definedOrder.add(\"{m.Path()}\");");
            sw.WriteLine("        HotManager.startModules(definedOrder);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void stopModules() throws Exception {");
            sw.WriteLine("        if (null != HotManager) {");
            sw.WriteLine("            var definedOrderReverse = new java.util.ArrayList<String>();");
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var name = project.ModuleStartOrder[i].Path();
                sw.WriteLine($"            definedOrderReverse.add(\"{name}\");");
            }
            sw.WriteLine("            HotManager.stopModules(definedOrderReverse);");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void startService() throws Exception {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + ".start();");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public synchronized void stopService() throws Exception {");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        if (" + m.Name + " != null)");
                sw.WriteLine("            " + m.Name + ".stop();");
            }
            sw.WriteLine("    }");
        }
    }
}

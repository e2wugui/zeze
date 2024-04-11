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
            sw.WriteLine("    public Zeze.Application Zeze;");
            sw.WriteLine();

            foreach (Service m in project.Services.Values)
                sw.WriteLine("    public " + m.FullName + " " + m.Name + ";");
            if (project.Services.Count > 0)
                sw.WriteLine();

            foreach (Module m in project.AllOrderDefineModules)
            {
                if (false == project.Hot || false == m.Hot)
                {
                    // 非热更模块生成全局唯一变量。
                    string moduleName = Program.Upper1(m.Name);
                    var fullname = m.Path("_");
                    sw.WriteLine($"    public {m.Path(".", $"Module{moduleName}")} {fullname};");
                }
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
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void createZeze(Zeze.Config config) throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            sw.WriteLine("            if (Zeze != null)");
            sw.WriteLine("                throw new IllegalStateException(\"Zeze Has Created!\");");
            sw.WriteLine();
            sw.WriteLine($"            Zeze = new Zeze.Application(\"{project.Name}\", config);");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void createService() {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("            " + m.Name + " = new " + m.FullName + "(Zeze);");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void createModules() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            if (project.Hot)
                sw.WriteLine("            Zeze.setHotManager(new Zeze.Hot.HotManager(this, Zeze.getConfig().getHotWorkingDir(), Zeze.getConfig().getHotDistributeDir()));");
            sw.WriteLine("            Zeze.initialize(this);");
            if (project.Hot)
                sw.WriteLine("            Zeze.getHotManager().initialize(modules);");

            if (project.AllOrderDefineModules.Count > 0)
            {
                sw.WriteLine("            var _modules_ = createRedirectModules(new Class[] {");
                foreach (Module m in project.AllOrderDefineModules)
                {
                    if (false == project.Hot || false == m.Hot)
                        sw.WriteLine("                " + m.Path(".", "Module" + Program.Upper1(m.Name)) + ".class,");
                }
                sw.WriteLine("            });");
                sw.WriteLine("            if (_modules_ == null)");
                sw.WriteLine("                return;");
                sw.WriteLine();
                int index = 0;
                foreach (Module m in project.AllOrderDefineModules)
                {
                    if (false == project.Hot || false == m.Hot)
                    {
                        string className = m.Path(".", "Module" + Program.Upper1(m.Name));
                        var fullname = m.Path("_");
                        sw.WriteLine($"            {fullname} = ({className})_modules_[" + index + "];");
                        sw.WriteLine($"            {fullname}.Initialize(this);");
                        sw.WriteLine($"            if (modules.put({fullname}.getFullName(), {fullname}) != null)");
                        sw.WriteLine($"                throw new IllegalStateException(\"duplicate module name: {fullname}\");");
                        sw.WriteLine();
                        index++;
                    }
                }
            }
            sw.WriteLine("            Zeze.setSchemas(new " + project.Solution.Path(".", "Schemas") + "());");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void destroyModules() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (false == project.Hot || false == m.Hot)
                {
                    var fullname = m.Path("_");
                    sw.WriteLine("            " + fullname + " = null;");
                }
            }
            if (project.Hot)
            {
                sw.WriteLine("            if (null != Zeze.getHotManager()) {");
                sw.WriteLine("                Zeze.getHotManager().destroyModules();");
                sw.WriteLine("                Zeze.setHotManager(null);");
                sw.WriteLine("            }");
            }
            sw.WriteLine("            modules.clear();");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void destroyServices() {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("            " + m.Name + " = null;");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void destroyZeze() {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            sw.WriteLine("            Zeze = null;");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void startModules() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            foreach (var m in project.ModuleStartOrder)
            {
                if (false == project.Hot || false == m.Hot)
                {
                    sw.WriteLine("            " + m.Path("_") + ".Start(this);");
                }
                else
                {
                    // hot module start
                    sw.WriteLine($"            Zeze.getHotManager().startModule(\"{m.Path()}\");");
                }
            }
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m) && (false == project.Hot || false == m.Hot))
                    sw.WriteLine("            " + m.Path("_") + ".Start(this);");
            }
            if (project.Hot)
            {
                sw.WriteLine("            if (null != Zeze.getHotManager()) {");
                sw.WriteLine("                var definedOrder = new java.util.HashSet<String>();");
                foreach (var m in project.ModuleStartOrder)
                {
                    sw.WriteLine($"                definedOrder.add(\"{m.Path()}\");");
                }
                sw.WriteLine("                Zeze.getHotManager().startModulesExcept(definedOrder);");
                sw.WriteLine("            }");
            }
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    @Override");
            sw.WriteLine("    public void startLastModules() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            foreach (var m in project.ModuleStartOrder)
            {
                if (false == project.Hot || false == m.Hot)
                {
                    sw.WriteLine("            " + m.Path("_") + ".StartLast();");
                }
                else
                {
                    // hot module start
                    sw.WriteLine($"            Zeze.getHotManager().startLastModule(\"{m.Path()}\");");
                }
            }
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m) && (false == project.Hot || false == m.Hot))
                    sw.WriteLine("            " + m.Path("_") + ".StartLast();");
            }
            if (project.Hot)
            {
                sw.WriteLine("            if (null != Zeze.getHotManager()) {");
                sw.WriteLine("                var definedOrder = new java.util.HashSet<String>();");
                foreach (var m in project.ModuleStartOrder)
                {
                    sw.WriteLine($"                definedOrder.add(\"{m.Path()}\");");
                }
                sw.WriteLine("                Zeze.getHotManager().startLastModulesExcept(definedOrder);");
                sw.WriteLine("            }");
            }
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void stopModules() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (!project.ModuleStartOrder.Contains(m) && (false == project.Hot || false == m.Hot))
                {
                    var name = m.Path("_");
                    sw.WriteLine("            if (" + name + " != null)");
                    sw.WriteLine("                " + name + ".Stop(this);");
                }
            }
            if (project.Hot)
            {
                sw.WriteLine("            if (null != Zeze.getHotManager()) {");
                sw.WriteLine("                var definedOrder = new java.util.HashSet<String>();");
                foreach (var m in project.ModuleStartOrder)
                {
                    sw.WriteLine($"                definedOrder.add(\"{m.Path()}\");");
                }
                sw.WriteLine("                Zeze.getHotManager().stopModulesExcept(definedOrder);");
                sw.WriteLine("            }");
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var m = project.ModuleStartOrder[i];
                if (false == project.Hot || false == m.Hot)
                {
                    var name = m.Path("_");
                    sw.WriteLine("            if (" + name + " != null)");
                    sw.WriteLine("                " + name + ".Stop(this);");
                }
                else
                {
                    // hot module stop
                    sw.WriteLine($"            Zeze.getHotManager().stopModule(\"{m.Path()}\");");
                }
            }
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void stopBeforeModules() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (!project.ModuleStartOrder.Contains(m) && (false == project.Hot || false == m.Hot))
                {
                    var name = m.Path("_");
                    sw.WriteLine("            if (" + name + " != null)");
                    sw.WriteLine("                " + name + ".StopBefore();");
                }
            }
            if (project.Hot)
            {
                sw.WriteLine("            if (null != Zeze.getHotManager()) {");
                sw.WriteLine("                var definedOrder = new java.util.HashSet<String>();");
                foreach (var m in project.ModuleStartOrder)
                {
                    sw.WriteLine($"                definedOrder.add(\"{m.Path()}\");");
                }
                sw.WriteLine("                Zeze.getHotManager().stopBeforeModulesExcept(definedOrder);");
                sw.WriteLine("            }");
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var m = project.ModuleStartOrder[i];
                if (false == project.Hot || false == m.Hot)
                {
                    var name = m.Path("_");
                    sw.WriteLine("            if (" + name + " != null)");
                    sw.WriteLine("                " + name + ".StopBefore();");
                }
                else
                {
                    // hot module stop
                    sw.WriteLine($"            Zeze.getHotManager().stopBeforeModule(\"{m.Path()}\");");
                }
            }
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void startService() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("            " + m.Name + ".start();");
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    public void stopService() throws Exception {");
            sw.WriteLine("        lock();");
            sw.WriteLine("        try {");
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("            if (" + m.Name + " != null)");
                sw.WriteLine("                " + m.Name + ".stop();");
            }
            sw.WriteLine("        } finally {");
            sw.WriteLine("            unlock();");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            if (project.Hot)
            {
                sw.WriteLine();
                sw.WriteLine("    public static void distributeHot(Zeze.Hot.Distribute distribute) throws Exception {");
                sw.WriteLine("        var hotModules = new java.util.HashSet<String>();");
                foreach (Module m in project.AllOrderDefineModules)
                {
                    if (project.Hot && m.Hot)
                        sw.WriteLine($"        hotModules.add(\"{m.Path()}\");");
                }
                sw.WriteLine($"        distribute.pack(hotModules, \"{project.Name}\", \"{project.Solution.Name}\");");
                sw.WriteLine("    }");
            }
        }
    }
}

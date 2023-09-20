using System;
using System.IO;
using Zeze.Util;

namespace Zeze.Gen.python
{
    public class App
    {
        readonly Project project;
        readonly string srcDir;

        public App(Project project, string srcDir)
        {
            this.project = project;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            FileChunkGen fcg = new();
            string fullDir = project.Solution.GetFullPath(srcDir);
            string fullFileName = Path.Combine(fullDir, "App.py");
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

            // sw.WriteLine(fcg.ChunkStartTag + " " + ChunkNameImport + " @formatter:off");
            // ImportGen(sw);
            // sw.WriteLine(fcg.ChunkEndTag + " " + ChunkNameImport + " @formatter:on");
            // sw.WriteLine();
            sw.WriteLine("from zeze.app import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import {project.ScriptDir}.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine("class App:");
            sw.WriteLine("    instance = None");
            sw.WriteLine();
            sw.WriteLine("    @staticmethod");
            sw.WriteLine("    def get_instance():");
            sw.WriteLine("        return App.instance");
            sw.WriteLine();
            sw.WriteLine("    def start(self):");
            sw.WriteLine("        self.create_zeze()");
            sw.WriteLine("        self.create_service()");
            sw.WriteLine("        self.create_modules()");
            sw.WriteLine("        self.zeze.start()  # 启动数据库");
            sw.WriteLine("        self.start_modules()  # 启动模块，装载配置什么的。");
            sw.WriteLine("        self.start_service()  # 启动网络");
            sw.WriteLine();
            sw.WriteLine("    def stop(self):");
            sw.WriteLine("        self.stop_service()  # 关闭网络");
            sw.WriteLine("        self.stop_modules()  # 关闭模块，卸载配置什么的。");
            sw.WriteLine("        self.zeze.stop()  # 关闭数据库");
            sw.WriteLine("        self.destroy_modules()");
            sw.WriteLine("        self.destroy_services()");
            sw.WriteLine("        self.destroy_zeze()");
            sw.WriteLine();
            sw.WriteLine("    # " + fcg.ChunkStartTag + " " + ChunkNameAppGen + " @formatter:off");
            AppGen(sw);
            sw.WriteLine("    # " + fcg.ChunkEndTag + " " + ChunkNameAppGen + " @formatter:on");
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
        }

        void AppGen(StreamWriter sw)
        {
            sw.WriteLine("    def __init__(self):");
            sw.WriteLine("        self.zeze = None");
            sw.WriteLine("        self.modules = {}");
            sw.WriteLine();

            foreach (Service m in project.Services.Values)
                sw.WriteLine($"        self.{m.Name} = None");
            if (project.Services.Count > 0)
                sw.WriteLine();

            foreach (Module m in project.AllOrderDefineModules)
            {
                // 非热更模块生成全局唯一变量。
                var fullname = m.Path("_");
                sw.WriteLine($"        self.{fullname} = None");
            }
            if (project.AllOrderDefineModules.Count > 0)
                sw.WriteLine();

            sw.WriteLine("    def get_zeze(self):");
            sw.WriteLine("        return self.zeze");
            sw.WriteLine();
            sw.WriteLine("    def create_zeze(self, config=None):");
            sw.WriteLine("        if self.zeze is not None:");
            sw.WriteLine("            raise Exception(\"zeze has created!\")");
            sw.WriteLine();
            sw.WriteLine($"        self.zeze = Application(\"{project.Name}\", config)");
            sw.WriteLine();
            sw.WriteLine("    def create_service(self):");
            foreach (var m in project.Services.Values)
                sw.WriteLine("        self." + m.Name + " = " + m.FullName + "(self.zeze)");
            sw.WriteLine();
            sw.WriteLine("    def create_modules(self):");
            sw.WriteLine("        self.zeze.init(self)");

            foreach (var m in project.AllOrderDefineModules)
            {
                string className = m.Path(".", "Module" + Program.Upper1(m.Name));
                var fullname = m.Path("_");
                sw.WriteLine();
                sw.WriteLine($"        self.{fullname} = {className}(self.zeze)");
                sw.WriteLine($"        self.{fullname}.init()");
                sw.WriteLine($"        if self.{fullname}.get_full_name() in self.modules:");
                sw.WriteLine($"            raise Exception(\"duplicate module name: {fullname}\")");
                sw.WriteLine($"        self.modules[self.{fullname}.get_full_name()] = self.{fullname}");
            }
            sw.WriteLine();
            sw.WriteLine("    def destroy_modules(self):");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                var fullname = m.Path("_");
                sw.WriteLine("        self." + fullname + " = None");
            }
            sw.WriteLine("        self.modules.clear()");
            sw.WriteLine();
            sw.WriteLine("    def destroy_services(self):");
            foreach (var m in project.Services.Values)
                sw.WriteLine("        self." + m.Name + " = None");
            sw.WriteLine();
            sw.WriteLine("    def destroy_zeze(self):");
            sw.WriteLine("        self.zeze = None");
            sw.WriteLine();
            sw.WriteLine("    def start_modules(self):");
            foreach (var m in project.ModuleStartOrder)
            {
                sw.WriteLine("        self." + m.Path("_") + ".start()");
            }
            foreach (var m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        self." + m.Path("_") + ".start()");
            }
            sw.WriteLine();
            sw.WriteLine("    def start_last_modules(self):");
            foreach (var m in project.ModuleStartOrder)
            {
                sw.WriteLine("        self." + m.Path("_") + ".start_last()");
            }
            foreach (var m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        self." + m.Path("_") + ".start_last()");
            }
            sw.WriteLine();
            sw.WriteLine("    def stop_modules(self):");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (!project.ModuleStartOrder.Contains(m))
                {
                    var name = m.Path("_");
                    sw.WriteLine("        if self." + name + " is not None:");
                    sw.WriteLine("            self." + name + ".stop()");
                }
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var m = project.ModuleStartOrder[i];
                var name = m.Path("_");
                sw.WriteLine("        if self." + name + " is not None:");
                sw.WriteLine("            self." + name + ".stop()");
            }
            sw.WriteLine();
            sw.WriteLine("    def stop_before_modules(self):");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (!project.ModuleStartOrder.Contains(m))
                {
                    var name = m.Path("_");
                    sw.WriteLine("        if self." + name + " is not None:");
                    sw.WriteLine("            self." + name + ".stop_before()");
                }
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var m = project.ModuleStartOrder[i];
                var name = m.Path("_");
                sw.WriteLine("        if self." + name + " is not None:");
                sw.WriteLine("            self." + name + ".stop_before()");
            }
            sw.WriteLine();
            sw.WriteLine("    def start_service(self):");
            foreach (var m in project.Services.Values)
                sw.WriteLine("        self." + m.Name + ".start()");
            sw.WriteLine();
            sw.WriteLine("    def stop_service(self):");
            foreach (var m in project.Services.Values)
            {
                sw.WriteLine("        if self." + m.Name + " is not None:");
                sw.WriteLine("            self." + m.Name + ".stop()");
            }
        }
    }
}

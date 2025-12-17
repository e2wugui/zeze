using System;
using System.IO;

namespace Zeze.Gen.cxx
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
            using StreamWriter sw = project.Solution.OpenWriter(genDir, "App.h");
            if (sw == null)
                return;

            sw.WriteLine("#pragma once");
            sw.WriteLine();
            foreach (Module m in project.AllOrderDefineModules)
            {
                sw.WriteLine($"#include \"{m.Path("/", "Module" + Program.Upper1(m.Name) + ".h")}\"");;
            }
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine($"#include \"{project.Solution.Name}/{m.Name}.h\"");
            }
            sw.WriteLine();
            sw.WriteLine($"namespace {project.Solution.Name} {{");
            sw.WriteLine();
            sw.WriteLine("class App {");
            sw.WriteLine("public:");
            sw.WriteLine("    static App& GetInstance()");
            sw.WriteLine("    {");
            sw.WriteLine("        static App staticInstance;");
            sw.WriteLine("        return staticInstance;");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void Start() {");
            sw.WriteLine("        CreateService();");
            sw.WriteLine("        CreateModules();");
            sw.WriteLine("        StartModules(); // 启动模块，装载配置什么的。");
            sw.WriteLine("        StartService(); // 启动网络");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void Stop() {");
            sw.WriteLine("        StopService(); // 关闭网络");
            sw.WriteLine("        StopModules(); // 关闭模块，卸载配置什么的。");
            sw.WriteLine("        DestroyModules();");
            sw.WriteLine("        DestroyServices();");
            sw.WriteLine("    }");
            sw.WriteLine();
            AppGen(sw);
            sw.WriteLine("};");
            sw.WriteLine("}");
        }

        void AppGen(StreamWriter sw)
        {
            //sw.WriteLine("    std::unordered_map<std::string, Zeze::IModule> modules;");
            //sw.WriteLine();

            foreach (Service m in project.Services.Values)
                sw.WriteLine($"    std::unique_ptr<{project.Solution.Name}::{m.Name}> " + m.Name + ";");
            if (project.Services.Count > 0)
                sw.WriteLine();

            foreach (Module m in project.AllOrderDefineModules)
            {
                string moduleName = Program.Upper1(m.Name);
                var fullname = m.Path("_");
                sw.WriteLine($"    std::unique_ptr<{m.Path("::", $"Module{moduleName}")}> {fullname};");
            }
            if (project.AllOrderDefineModules.Count > 0)
                sw.WriteLine();

            sw.WriteLine("    void CreateService() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine($"        {m.Name}.reset(new {project.Solution.Name}::{m.Name}());");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void CreateModules() {");
            if (project.AllOrderDefineModules.Count > 0)
            {
                foreach (Module m in project.AllOrderDefineModules)
                {
                    string className = m.Path("::", "Module" + Program.Upper1(m.Name));
                    var fullname = m.Path("_");
                    sw.WriteLine($"        {fullname}.reset(new {className}(this));");
                }
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void DestroyModules() {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                var fullname = m.Path("_");
                sw.WriteLine("        " + fullname + ".reset(nullptr);");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void DestroyServices() {");
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + ".reset(nullptr);");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void StartModules() {");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine("        " + m.Path("_") + "->Start();");
            foreach (Module m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        " + m.Path("_") + "->Start();");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void StopModules() {");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
            {
                var m = project.AllOrderDefineModules[i];
                if (!project.ModuleStartOrder.Contains(m))
                {
                    var name = m.Path("_");
                    sw.WriteLine("        if (" + name + " != nullptr)");
                    sw.WriteLine("            " + name + "->Stop();");
                }
            }
            for (int i = project.ModuleStartOrder.Count - 1; i >= 0; --i)
            {
                var name = project.ModuleStartOrder[i].Path("_");
                sw.WriteLine("        if (" + name + " != nullptr)");
                sw.WriteLine("            " + name + "->Stop();");
            }
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void StartService() {");
            /*
            foreach (Service m in project.Services.Values)
                sw.WriteLine("        " + m.Name + ".start();");
            */
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    void StopService() {");
            /*
            foreach (Service m in project.Services.Values)
            {
                sw.WriteLine("        if (" + m.Name + ".get() != nullptr)");
                sw.WriteLine("            " + m.Name + "->stop();");
            }
            */
            sw.WriteLine("    }");
        }
    }
}

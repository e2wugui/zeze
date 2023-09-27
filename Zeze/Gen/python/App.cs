using System.IO;
using Zeze.Util;

namespace Zeze.Gen.python
{
    public class App
    {
        readonly Project project;
        readonly string genDir;

        public App(Project project, string genDir)
        {
            this.project = project;
            this.genDir = genDir;
        }

        public void Make()
        {
            string fullDir = project.Solution.GetFullPath(genDir);
            string appClassName = $"{Program.Upper1(project.Name)}App";
            string fullFileName = Path.Combine(fullDir, $"{appClassName}.py");
            FileSystem.CreateDirectory(fullDir);
            using StreamWriter sw = Program.OpenStreamWriter(fullFileName);
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import {project.ScriptDir}.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine($"class {appClassName}:");
            sw.WriteLine("    instance = None");
            sw.WriteLine();
            sw.WriteLine("    @staticmethod");
            sw.WriteLine("    def get_instance():");
            sw.WriteLine($"        if {appClassName}.instance is None:");
            sw.WriteLine($"            {appClassName}.instance = {appClassName}()");
            sw.WriteLine($"        return {appClassName}.instance");
            sw.WriteLine();
            sw.WriteLine("    def __init__(self):");
            sw.WriteLine("        self.modules = {}");
            sw.WriteLine();

            foreach (var m in project.Services.Values)
                sw.WriteLine($"        self.{m.Name} = None");
            if (project.Services.Count > 0)
                sw.WriteLine();

            foreach (var m in project.AllOrderDefineModules)
                sw.WriteLine($"        self.{m.Path("_")} = None");
            if (project.AllOrderDefineModules.Count > 0)
                sw.WriteLine();

            sw.WriteLine("    def start(self):");
            sw.WriteLine("        self.create_service()");
            sw.WriteLine("        self.create_modules()");
            sw.WriteLine("        self.start_modules()");
            sw.WriteLine("        self.start_service()");
            sw.WriteLine("        self.start_last_modules()");
            sw.WriteLine();
            sw.WriteLine("    def stop(self):");
            sw.WriteLine("        self.stop_before_modules()");
            sw.WriteLine("        self.stop_service()");
            sw.WriteLine("        self.stop_modules()");
            sw.WriteLine("        self.destroy_modules()");
            sw.WriteLine("        self.destroy_services()");
            sw.WriteLine();

            sw.WriteLine("    def create_service(self):");
            foreach (var m in project.Services.Values)
                sw.WriteLine("        self." + m.Name + " = " + m.FullName + "(self)");
            if (project.Services.Count == 0)
                sw.WriteLine("        pass");
            sw.WriteLine();

            sw.WriteLine("    def start_service(self):");
            foreach (var m in project.Services.Values)
                sw.WriteLine("        self." + m.Name + ".start()");
            if (project.Services.Count == 0)
                sw.WriteLine("        pass");
            sw.WriteLine();

            sw.WriteLine("    def stop_service(self):");
            foreach (var m in project.Services.Values)
            {
                sw.WriteLine("        if self." + m.Name + " is not None:");
                sw.WriteLine("            self." + m.Name + ".stop()");
            }
            if (project.Services.Count == 0)
                sw.WriteLine("        pass");
            sw.WriteLine();

            sw.WriteLine("    def destroy_services(self):");
            foreach (var m in project.Services.Values)
                sw.WriteLine($"        self.{m.Name} = None");
            if (project.Services.Count == 0)
                sw.WriteLine("        pass");
            sw.WriteLine();

            sw.WriteLine("    def create_modules(self):");
            foreach (var m in project.AllOrderDefineModules)
            {
                string className = m.Path(".", "Module" + Program.Upper1(m.Name));
                var fullname = m.Path("_");
                sw.WriteLine($"        self.{fullname} = {className}(self)");
                sw.WriteLine($"        self.{fullname}.init()");
                sw.WriteLine($"        if self.{fullname}.get_full_name() in self.modules:");
                sw.WriteLine($"            raise Exception(\"duplicate module name: {fullname}\")");
                sw.WriteLine($"        self.modules[self.{fullname}.get_full_name()] = self.{fullname}");
                sw.WriteLine();
            }
            if (project.AllOrderDefineModules.Count == 0)
            {
                sw.WriteLine("        pass");
                sw.WriteLine();
            }

            sw.WriteLine("    def start_modules(self):");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine("        self." + m.Path("_") + ".start()");
            foreach (var m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        self." + m.Path("_") + ".start()");
            }
            if (project.AllOrderDefineModules.Count == 0 && project.ModuleStartOrder.Count == 0)
                sw.WriteLine("        pass");
            sw.WriteLine();

            sw.WriteLine("    def start_last_modules(self):");
            foreach (var m in project.ModuleStartOrder)
                sw.WriteLine("        self." + m.Path("_") + ".start_last()");
            foreach (var m in project.AllOrderDefineModules)
            {
                if (!project.ModuleStartOrder.Contains(m))
                    sw.WriteLine("        self." + m.Path("_") + ".start_last()");
            }
            if (project.AllOrderDefineModules.Count == 0 && project.ModuleStartOrder.Count == 0)
                sw.WriteLine("        pass");
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
            if (project.AllOrderDefineModules.Count == 0 && project.ModuleStartOrder.Count == 0)
                sw.WriteLine("        pass");
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
            if (project.AllOrderDefineModules.Count == 0 && project.ModuleStartOrder.Count == 0)
                sw.WriteLine("        pass");
            sw.WriteLine();

            sw.WriteLine("    def destroy_modules(self):");
            for (int i = project.AllOrderDefineModules.Count - 1; i >= 0; --i)
                sw.WriteLine($"        self.{project.AllOrderDefineModules[i].Path("_")} = None");
            sw.WriteLine("        self.modules.clear()");
            if (project.AllOrderDefineModules.Count == 0)
                sw.WriteLine("        pass");
        }
    }
}

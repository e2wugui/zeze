using System.IO;

namespace Zeze.Gen.cxx
{
    public class ServiceFormatter
    {
        readonly Service service;
        readonly string genDir;
        readonly string srcDir;

        public ServiceFormatter(Service service, string genDir, string srcDir)
        {
            this.service = service;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakePartialInGen();
            MakePartialInSrc();
        }

        public string BaseClass()
        {
            return service.Base.Length > 0 ? service.Base : "Zeze::Net::Service";
        }

        public void MakePartialInGen()
        {
            using StreamWriter sw = service.Project.Solution.OpenWriter(genDir, service.Name + "Base.hpp");
            if (sw == null)
                return;

            sw.WriteLine("#include \"zeze/cxx/Net.h\"");
            sw.WriteLine();
            var paths = service.Project.Solution.Paths();
            foreach ( var path in paths )
            {
                sw.WriteLine($"namespace {path} {{");
            }
            sw.WriteLine("class " + service.Name + "Base : public " + BaseClass() + " {");
            sw.WriteLine("public:");
            sw.WriteLine("    " + service.Name + "Base() {");
            sw.WriteLine("    }");

            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }

        public void MakePartialInSrc()
        {
            using StreamWriter sw = service.Project.Solution.OpenWriter(srcDir, service.Name + ".h", false);
            if (sw == null)
                return;

            sw.WriteLine($"#include \"Gen/{service.Project.Solution.Path("/")}/{service.Name}Base.hpp\"");
            sw.WriteLine();
            var paths = service.Project.Solution.Paths();
            foreach (var path in paths)
            {
                sw.WriteLine($"namespace {path} {{");
            }
            sw.WriteLine($"class {service.Name} : public {service.Name}Base {{");
            sw.WriteLine("public:");
            sw.WriteLine("    " + service.Name + "() {");
            sw.WriteLine("    }");
            sw.WriteLine();
            sw.WriteLine("    // 重载需要的方法。");
            sw.WriteLine("};");
            foreach (var path in paths)
            {
                sw.WriteLine("}");
            }
        }
    }
}

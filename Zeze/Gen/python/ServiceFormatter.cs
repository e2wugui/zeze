using System.IO;

namespace Zeze.Gen.python
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
            MakePartialInSrc();
        }

        public string BaseClass()
        {
            return service.Base.Length > 0 ? service.Base : "Zeze.Net.Service";
        }

        public void MakePartialInSrc()
        {
            using StreamWriter sw = service.Project.Solution.OpenWriter(srcDir, service.Name + ".py", false);
            if (sw == null)
                return;

            sw.WriteLine("package " + service.Project.Solution.Path() + ";");
            sw.WriteLine();
            sw.WriteLine($"public class {service.Name} extends {BaseClass()} {{");
            sw.WriteLine("    public " + service.Name + "(Zeze.Application zeze) {");
            sw.WriteLine("        super(zeze);");
            sw.WriteLine("    }");
            sw.WriteLine("    // 重载需要的方法。");
            sw.WriteLine("}");
        }
    }
}

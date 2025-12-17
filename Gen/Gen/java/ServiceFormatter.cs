using System.IO;

namespace Zeze.Gen.java
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
            return service.Base.Length > 0 ? service.Base : "Zeze.Net.Service";
        }

        public void MakePartialInGen()
        {
            using StreamWriter sw = service.Project.Solution.OpenWriter(genDir, service.Name + "Base.java");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated @formatter:off");
            sw.WriteLine("package " + service.Project.Solution.Path() + ";");
            sw.WriteLine();
            sw.WriteLine("public class " + service.Name + "Base extends " + BaseClass() + " {");
            sw.WriteLine("    public " + service.Name + "Base(Zeze.Application zeze) {");
            sw.WriteLine("        super(\"" + service.Name + "\", zeze);");
            sw.WriteLine("    }");
            /*
            sw.WriteLine();
            if (service.IsProvider)
            {
                sw.WriteLine("        // 用来同步等待Provider的静态绑定完成。");
                sw.WriteLine("        public System.Threading.ManualResetEvent ProviderStaticBindCompleted = new System.Threading.ManualResetEvent(false);");
                sw.WriteLine();
                sw.WriteLine("        public void ProviderStaticBind(Zeze.Net.AsyncSocket socket)");
                sw.WriteLine("        {");
                sw.WriteLine("            var rpc = new Zezex.Provider.Bind();");
                foreach (var module in service.Modules)
                {
                    var fullName = module.Path();
                    if (service.DynamicModules.Contains(fullName))
                        continue;
                    sw.WriteLine($"            rpc.Argument.ModuleIds.Add({module.Id}); // {fullName}");
                }
                sw.WriteLine("            rpc.Send(socket, (protocol) => { ProviderStaticBindCompleted.Set(); return 0; });");
                sw.WriteLine("        }");
                sw.WriteLine();
            }
            */
            sw.WriteLine("}");
        }

        public void MakePartialInSrc()
        {
            using StreamWriter sw = service.Project.Solution.OpenWriter(srcDir, service.Name + ".java", false);
            if (sw == null)
                return;

            sw.WriteLine("package " + service.Project.Solution.Path() + ";");
            sw.WriteLine();
            sw.WriteLine($"public class {service.Name} extends {service.Name}Base {{");
            sw.WriteLine("    public " + service.Name + "(Zeze.Application zeze) {");
            sw.WriteLine("        super(zeze);");
            sw.WriteLine("    }");
            sw.WriteLine("    // 重载需要的方法。");
            sw.WriteLine("}");
        }
    }
}

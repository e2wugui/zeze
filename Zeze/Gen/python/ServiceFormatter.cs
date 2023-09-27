namespace Zeze.Gen.python
{
    public class ServiceFormatter
    {
        readonly Service service;
        readonly string srcDir;

        public ServiceFormatter(Service service, string srcDir)
        {
            this.service = service;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            MakePartialInSrc();
        }

        public string BaseClass()
        {
            return service.Base.Length > 0 ? service.Base : "Service";
        }

        public void MakePartialInSrc()
        {
            using var sw = service.Project.Solution.OpenWriter(srcDir, service.Name + ".py", false);
            if (sw == null)
                return;

            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine("from zeze.net import *");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine($"class {service.Name}({BaseClass()}):");
            sw.WriteLine("    def __init__(self, app):");
            sw.WriteLine("        super().__init__()");
            sw.WriteLine("        self.app = app");
            sw.WriteLine();
            sw.WriteLine("    def start(self):");
            sw.WriteLine("        pass  # TODO");
            sw.WriteLine();
            sw.WriteLine("    def stop(self):");
            sw.WriteLine("        pass  # TODO");
        }
    }
}

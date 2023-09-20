namespace Zeze.Gen.python
{
    public class ProtocolFormatter
    {
        readonly Protocol p;

        public ProtocolFormatter(Protocol p)
        {
            this.p = p;
        }

        public void Make(string baseDir, Project project)
        {
            using var sw = p.Space.OpenWriter(baseDir, p.Name + ".py");
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine("from zeze.bean import *");
            sw.WriteLine("from zeze.net import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import gen.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine($"class {p.Name}(Protocol):");
            if (p.Comment.Length > 0)
                sw.WriteLine(Maker.toPythonComment(p.Comment, "    "));
            sw.WriteLine($"    ModuleId = {p.Space.Id}");
            sw.WriteLine($"    ProtocolId = {p.Id}" + (p.Id < 0 ? "  # " + (uint)p.Id : ""));
            sw.WriteLine($"    TypeId = Service.make_type_id(ModuleId, ProtocolId)  # {Net.Protocol.MakeTypeId(p.Space.Id, p.Id)}");
            sw.WriteLine();
            sw.WriteLine($"    def get_module_id(self):");
            sw.WriteLine($"        return {p.Name}.ModuleId");
            sw.WriteLine();
            sw.WriteLine($"    def get_protocol_id(self):");
            sw.WriteLine($"        return {p.Name}.ProtocolId");
            sw.WriteLine();
            sw.WriteLine($"    def type_id(self):");
            sw.WriteLine($"        return {p.Name}.TypeId");
            sw.WriteLine();
            foreach (var e in p.Enums)
            {
                sw.WriteLine(string.IsNullOrEmpty(e.Comment)
                    ? $"    {e.Name} = {e.Value}  {Maker.toPythonComment(e.Comment)}"
                    : $"    {e.Name} = {e.Value}");
            }
            if (p.Enums.Count > 0)
                sw.WriteLine();
            string argument = p.ArgumentType == null ? "EmptyBean" : TypeName.GetName(p.ArgumentType);
            sw.WriteLine($"    def __init__(self, arg=None):");
            sw.WriteLine($"        super().__init__()");
            sw.WriteLine($"        self.arg = {argument}() if arg is None else arg");
        }
    }
}

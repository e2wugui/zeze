namespace Zeze.Gen.python
{
    public class RpcFormatter
    {
        readonly Rpc rpc;

        public RpcFormatter(Rpc rpc)
        {
            this.rpc = rpc;
        }

        public void Make(string baseDir, Project project)
        {
            using var sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".py");
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
            sw.WriteLine($"class {rpc.Name}(Rpc):");
            if (rpc.Comment.Length > 0)
                sw.WriteLine(Maker.toPythonComment(rpc.Comment, "    "));
            sw.WriteLine($"    ModuleId = {rpc.Space.Id}");
            sw.WriteLine($"    ProtocolId = {rpc.Id}" + (rpc.Id < 0 ? "  # " + (uint)rpc.Id : ""));
            sw.WriteLine($"    TypeId = Service.make_type_id(ModuleId, ProtocolId)  # {Net.Protocol.MakeTypeId(rpc.Space.Id, rpc.Id)}");
            sw.WriteLine();
            sw.WriteLine($"    def get_module_id(self):");
            sw.WriteLine($"        return {rpc.Name}.ModuleId");
            sw.WriteLine();
            sw.WriteLine($"    def get_protocol_id(self):");
            sw.WriteLine($"        return {rpc.Name}.ProtocolId");
            sw.WriteLine();
            sw.WriteLine($"    def type_id(self):");
            sw.WriteLine($"        return {rpc.Name}.TypeId");
            sw.WriteLine();
            foreach (var e in rpc.Enums)
            {
                sw.WriteLine(string.IsNullOrEmpty(e.Comment)
                    ? $"    {e.Name} = {e.Value}  {Maker.toPythonComment(e.Comment)}"
                    : $"    {e.Name} = {e.Value}");
            }
            if (rpc.Enums.Count > 0)
                sw.WriteLine();
            string argument = rpc.ArgumentType == null ? "EmptyBean" : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? "EmptyBean" : TypeName.GetName(rpc.ResultType);
            sw.WriteLine($"    def __init__(self, arg=None):");
            sw.WriteLine($"        super().__init__()");
            sw.WriteLine($"        self.arg = {argument}() if arg is None else arg");
            sw.WriteLine($"        self.arg = {result}()");
        }
    }
}

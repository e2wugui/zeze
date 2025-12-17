namespace Zeze.Gen.ts
{
    public class RpcFormatter
    {
        Rpc rpc;
        public RpcFormatter(Rpc p)
        {
            rpc = p;
        }

        public void Make(System.IO.StreamWriter sw)
        {
            string argument = rpc.ArgumentType == null ? "Zeze.EmptyBean" : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? "Zeze.EmptyBean" : TypeName.GetName(rpc.ResultType);

            sw.WriteLine("export class " + rpc.Space.Path("_", rpc.Name) + " extends Zeze.Rpc<" + argument + ", " + result + "> {");
            sw.WriteLine("    public ModuleId(): number { return " + rpc.Space.Id + "; }");
            sw.WriteLine("    public ProtocolId(): number { return " + rpc.Id + "; }");
            // declare enums
            foreach (Types.Enum e in rpc.Enums)
            {
                sw.WriteLine("    public static readonly " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (rpc.Enums.Count > 0)
            {
                sw.WriteLine();
            }
            sw.WriteLine("    public constructor() {");
            sw.WriteLine("        super(new " + argument + "(), new " + result + "());");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

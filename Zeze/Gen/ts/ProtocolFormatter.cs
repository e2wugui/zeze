namespace Zeze.Gen.ts
{
    public class ProtocolFormatter
    {
        Protocol p;

        public ProtocolFormatter(Protocol p)
        {
            this.p = p;
        }

        public void Make(System.IO.StreamWriter sw)
        {
            string argument = p.ArgumentType == null ? "Zeze.EmptyBean" : TypeName.GetName(p.ArgumentType);
            sw.WriteLine("export class " + p.Space.Path("_", p.Name) + " extends Zeze.ProtocolWithArgument<" + argument + "> {");
            sw.WriteLine("    public ModuleId(): number { return " + p.Space.Id + "; }");
            sw.WriteLine("    public ProtocolId(): number { return " + p.Id + "; }");
            sw.WriteLine();
            // declare enums
            foreach (Types.Enum e in p.Enums)
            {
                sw.WriteLine("    public static readonly " + e.Name + " = " + e.Value + ";" + e.Comment);
            }
            if (p.Enums.Count > 0)
            {
                sw.WriteLine();
            }
            sw.WriteLine("    public constructor() {");
            sw.WriteLine("        super(new " + argument + "());");
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

using System.IO;

namespace Zeze.Gen.cs
{
    public class RpcFormatter
    {
        readonly Rpc rpc;
 
        public RpcFormatter(Rpc rpc)
        {
            this.rpc = rpc;
        }

        public void Make(string baseDir, bool isconfcs = false)
        {
            using StreamWriter sw = rpc.Space.OpenWriter(baseDir, rpc.Name + ".cs");
            if (sw == null)
                return;

            sw.WriteLine("// auto-generated");
            //sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine();
            if (rpc.Comment.Length > 0)
                sw.WriteLine(rpc.Comment);
            sw.WriteLine("// ReSharper disable RedundantCast RedundantNameQualifier RedundantOverflowCheckingContext");
            sw.WriteLine("// ReSharper disable once CheckNamespace");
            sw.WriteLine("namespace " + rpc.Space.Path());
            sw.WriteLine("{");

            var emptyBeanName = isconfcs ? "Zeze.Util.ConfEmptyBean" : "Zeze.Transaction.EmptyBean";
            string argument = rpc.ArgumentType == null ? emptyBeanName : TypeName.GetName(rpc.ArgumentType);
            string result = rpc.ResultType == null ? emptyBeanName : TypeName.GetName(rpc.ResultType);
            string baseclass = string.IsNullOrEmpty(rpc.Base) ? "Zeze.Net.Rpc" : rpc.Base;

            sw.WriteLine($"    public sealed class {rpc.Name} : {baseclass}<{argument}, {result}>");
            sw.WriteLine("    {");
            sw.WriteLine("        public const int ModuleId_ = " + rpc.Space.Id + ";");
            sw.WriteLine("        public const int ProtocolId_ = " + rpc.Id + ";" + (rpc.Id < 0 ? " // " + (uint)rpc.Id : ""));
            sw.WriteLine("        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // " + Net.Protocol.MakeTypeId(rpc.Space.Id, rpc.Id));
            sw.WriteLine();
            sw.WriteLine("        public override int ModuleId => ModuleId_;");
            sw.WriteLine("        public override int ProtocolId => ProtocolId_;");
            // declare enums
            foreach (Types.Enum e in rpc.Enums)
                sw.WriteLine($"        public const {TypeName.GetName(Types.Type.Compile(e.Type))} " + e.Name + " = " + e.Value + ";" + e.Comment);
            if (rpc.Enums.Count > 0)
                sw.WriteLine();
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }
    }
}

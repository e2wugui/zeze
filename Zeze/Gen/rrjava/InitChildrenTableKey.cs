using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class InitChildrenTableKey
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {");
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitRootInfo(root, this);");
                else if (v.VariableType is TypeDynamic)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitRootInfo(root, this);");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }
    }
}

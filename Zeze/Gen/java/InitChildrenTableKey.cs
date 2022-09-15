using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class InitChildrenTableKey
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {");
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".initRootInfo(root, this);");
                else if (v.VariableType is TypeDynamic)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".initRootInfo(root, this);");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public static void MakeReset(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "protected void resetChildrenRootInfo() {");
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".resetRootInfo();");
                else if (v.VariableType is TypeDynamic)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".resetRootInfo();");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }
    }
}

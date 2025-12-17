using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class InitChildrenTableKey
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            var genBegin = false;
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection || v.VariableType is TypeDynamic)
                {
                    if (!genBegin)
                    {
                        genBegin = true;
                        sw.WriteLine(prefix + "protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)");
                        sw.WriteLine(prefix + "{");
                    }
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitRootInfo(root, this);");
                }
            }
            if (genBegin)
            {
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }

            genBegin = false;
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection || v.VariableType is TypeDynamic)
                {
                    if (!genBegin)
                    {
                        genBegin = true;
                        sw.WriteLine(prefix + "protected override void InitChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root)");
                        sw.WriteLine(prefix + "{");
                    }
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitRootInfoWithRedo(root, this);");
                }
            }
            if (genBegin)
            {
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }

        public static void MakeReset(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "protected override void ResetChildrenRootInfo()");
            sw.WriteLine(prefix + "{");
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection || v.VariableType is TypeDynamic)
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".ResetRootInfo();");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }
    }
}

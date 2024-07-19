using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public static class InitChildrenTableKey
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
                        sw.WriteLine(prefix + "@Override");
                        sw.WriteLine(prefix + "protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {");
                    }
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".initRootInfo(_r_, this);");
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
                        sw.WriteLine(prefix + "@Override");
                        sw.WriteLine(prefix + "protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {");
                    }
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".initRootInfoWithRedo(_r_, this);");
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
            var genBegin = false;
            foreach (Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection || v.VariableType is TypeDynamic)
                {
                    if (!genBegin)
                    {
                        genBegin = true;
                        sw.WriteLine(prefix + "@Override");
                        sw.WriteLine(prefix + "protected void resetChildrenRootInfo() {");
                    }
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".resetRootInfo();");
                }
            }
            if (genBegin)
            {
                sw.WriteLine(prefix + "}");
                sw.WriteLine();
            }
        }
    }
}

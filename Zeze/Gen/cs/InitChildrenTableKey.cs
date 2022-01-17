using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class InitChildrenTableKey
    {
        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "protected override void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root)");
            sw.WriteLine(prefix + "{");
            foreach (Types.Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection)
                {
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitRootInfo(root, this);");
                }
                else if (v.VariableType is TypeDynamic)
                {
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitRootInfo(root, this);");
                }
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }
    }
}

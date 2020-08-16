using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class InitChildrenTableKey
    {
        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "protected override void InitChildrenTableKey(Zeze.Transaction.TableKey root)");
            sw.WriteLine(prefix + "{");
            foreach (Types.Variable v in bean.Variables)
            {
                if (v.VariableType.IsNormalBean || v.VariableType.IsCollection)
                {
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".InitTableKey(root);");
                }
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine("");
        }
    }
}

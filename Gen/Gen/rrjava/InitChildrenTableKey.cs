using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.rrjava
{
    public class InitChildrenTableKey
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "protected void initChildrenRootInfo(Zeze.Raft.RocksRaft.Record.@Nullable RootInfo root) {");
            foreach (Variable v in bean.Variables)
            {
                if (!v.Transient && (v.VariableType is Bean or TypeDynamic || v.VariableType.IsCollection))
                    sw.WriteLine(prefix + "    " + v.NamePrivate + ".initRootInfo(root, this);");
            }
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }
    }
}

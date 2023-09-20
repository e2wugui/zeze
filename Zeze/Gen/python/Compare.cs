using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class Compare
    {
        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine();
            sw.WriteLine($"{prefix}def __lt__(self, _o_):");
            foreach (Variable var in bean.VariablesIdOrder)
            {
                sw.WriteLine($"{prefix}    if self.{var.Name} < _o_.{var.Name}:");
                sw.WriteLine($"{prefix}        return True");
                sw.WriteLine($"{prefix}    if self.{var.Name} > _o_.{var.Name}:");
                sw.WriteLine($"{prefix}        return False");
            }
            sw.WriteLine(prefix + "    return False");
        }
    }
}

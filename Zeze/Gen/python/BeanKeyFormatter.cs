using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.python
{
    public class BeanKeyFormatter
    {
        readonly BeanKey beanKey;

        public BeanKeyFormatter(BeanKey beanKey)
        {
            this.beanKey = beanKey;
        }

        public void Make(string baseDir)
        {
            using StreamWriter sw = beanKey.Space.OpenWriter(baseDir, beanKey.Name + ".py");
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("from zeze.buffer import ByteBuffer");
            sw.WriteLine("from zeze.util import *");
            sw.WriteLine();
            if (beanKey.Comment.Length > 0)
                sw.WriteLine(Maker.toPythonComment(beanKey.Comment));
            sw.WriteLine($"class {beanKey.Name}(BeanKey):");

            // declare enums
            foreach (var e in beanKey.Enums)
                sw.WriteLine($"    {e.Name} = {e.Value}{Maker.toPythonComment(e.Comment, true)}");
            if (beanKey.Enums.Count > 0)
                sw.WriteLine();

            Construct.Make(beanKey, sw, "    ");
            Encode.Make(beanKey, sw, "    ");
            Decode.Make(beanKey, sw, "    ");
            HashCode.Make(beanKey, sw, "    ");
            Equal.Make(beanKey, sw, "    ");
            Compare.Make(beanKey, sw, "    ");
            Tostring.Make(beanKey, sw, "    ");
        }
    }
}

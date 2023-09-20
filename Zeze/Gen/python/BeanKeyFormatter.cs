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

        public void Make(string baseDir, Project project)
        {
            using StreamWriter sw = beanKey.Space.OpenWriter(baseDir, beanKey.Name + ".py");
            if (sw == null)
                return;

            sw.WriteLine("# auto-generated @formatter:off");
            sw.WriteLine("from zeze.bean import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine("from zeze.buffer import ByteBuffer");
            sw.WriteLine("from zeze.util import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine("from zeze.vector import *");
            sw.WriteLine("# noinspection PyUnresolvedReferences");
            sw.WriteLine($"import gen.{project.Solution.Name} as {project.Solution.Name}");
            sw.WriteLine();
            sw.WriteLine();
            sw.WriteLine("# noinspection GrazieInspection,PyPep8Naming,PyShadowingBuiltins");
            sw.WriteLine($"class {beanKey.Name}(BeanKey):");
            if (beanKey.Comment.Length > 0)
                sw.WriteLine($"{Maker.toPythonComment(beanKey.Comment, "    ")}");

            // declare enums
            if (beanKey.Enums.Count > 0)
                sw.WriteLine();
            foreach (var e in beanKey.Enums)
                sw.WriteLine($"    {e.Name} = {e.Value}  {Maker.toPythonComment(e.Comment)}");

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

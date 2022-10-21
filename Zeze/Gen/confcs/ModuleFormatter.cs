using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Gen.confcs
{
    // conf+cs 的ModuleFormatter仅生成enum。
    public class ModuleFormatter
    {
        readonly Project project;
        internal readonly Module module;
        readonly string genDir;
        readonly string srcDir;

        public ModuleFormatter(Project project, Module module, string genDir, string srcDir)
        {
            this.project = project;
            this.module = module;
            this.genDir = genDir;
            this.srcDir = srcDir;
        }

        public void Make()
        {
            using StreamWriter sw = module.OpenWriter(genDir, $"Module{module.Name}.cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine();
            if (module.Comment.Length > 0)
                sw.WriteLine(module.Comment);
            sw.WriteLine("namespace " + module.Path());
            sw.WriteLine("{");
            sw.WriteLine($"    public class Module{module.Name}");
            sw.WriteLine("    {");
            sw.WriteLine($"        public string FullName => \"{module.Path()}\";");
            sw.WriteLine($"        public string Name => \"{module.Name}\";");
            sw.WriteLine($"        public int Id => {module.Id};");
            GenEnums(sw);
            sw.WriteLine("    }");
            sw.WriteLine("}");
        }

        public void GenEnums(StreamWriter sw)
        {
            // declare enums
            if (module.Enums.Count > 0)
                sw.WriteLine();
            foreach (Types.Enum e in module.Enums)
                sw.WriteLine($"        public const {TypeName.GetName(Types.Type.Compile(e.Type))} {e.Name} = {e.Value};{e.Comment}");
        }
    }
}

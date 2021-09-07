using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace Zeze.Util
{
    public class Zezex
    {
        private string modules = "login";
        private bool linkd = true;
        private string SolutionName = null;
        private string ServerProjectName = null;
        private string ClientLang = null;
        private string ExportDirectory = "../../";
        private string ZezexDirectory = "./";

        private Zezex(string [] args)
        {
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-SolutionName": SolutionName = args[++i]; break;
                    case "-ExportDirectory": ExportDirectory = args[++i]; break;
                    case "-ZezexDirectory": ZezexDirectory = args[++i]; break;
                    case "-ServerProjectName": ServerProjectName = args[++i]; break;
                    case "-ClientLang": ClientLang = args[++i]; break;
                    case "-nolinkd": linkd = false; break;
                    case "-modules": modules = args[++i]; break;
                }
            }
        }

        private static void Usage()
        {
            Console.WriteLine("args:");
            Console.WriteLine("    [-c zezex] Must Present To Run Zezex Export");
            Console.WriteLine("    [-SolutionName Game] Must Present");
            Console.WriteLine("    [-ExportDirectory Path] default='../../'");
            Console.WriteLine("    [-ZezexDirectory Path] default='./'");
            Console.WriteLine("    [-ServerProjectName server] no change if not present");
            Console.WriteLine("    [-ClientLang cs|ts|lua] no change if not present");

            Console.WriteLine("    [-nolinkd] do not export linkd");
            Console.WriteLine("    [-modules ModuleNameA,ModuleNameB] default='login'");
            Console.WriteLine("    [-modules all] export all modules");
            Console.WriteLine("    [-modules none] export none module");
        }

        public static void Main(string [] args)
        {
            var x = new Zezex(args);

            if (false == x.VerifyParams())
            {
                Usage();
                return;
            }

            x.Export();
        }

        private bool VerifyParams()
        {
            if (string.IsNullOrEmpty(SolutionName))
                return false;
            if (false == Directory.Exists(ExportDirectory))
            {
                Console.WriteLine($"ExportDirectory Not Exist: Path={ExportDirectory}");
                return false;
            }
            ExportDirectory = Path.Combine(ExportDirectory, SolutionName);
            if (Directory.Exists(ExportDirectory))
            {
                Console.WriteLine($"ExportDirectory For Solution={SolutionName} Has Exist: Path={ExportDirectory}");
                return false;
            }
            if (false == Directory.Exists(ZezexDirectory))
            {
                Console.WriteLine($"ZezexDirectory Not Exist: Path={ZezexDirectory}");
                return false;
            }
            return true;
        }

        public void Export()
        {
            Directory.CreateDirectory(ExportDirectory);

            if (linkd)
                ExportLinkd();

            var moduleset = new HashSet<string>();
            foreach (var m in modules.Split(","))
                moduleset.Add(m);

            ExportModules();
        }

        private void ExportModules()
        {

        }

        private void ExportLinkd()
        {
            var linkdDir = Path.Combine(ExportDirectory, "linkd");
            Directory.CreateDirectory(linkdDir);

            CopyTo("linkd/Zezex", linkdDir);

            CopyTo("linkd/linkd.csproj", linkdDir);
            CopyTo("linkd/Program.cs", linkdDir);
            CopyTo("linkd/zeze.xml", linkdDir);
        }

        private void CopyTo(string relativePath, string destDirName)
        {
            var src = Path.Combine(ZezexDirectory, relativePath);
            FileSystem.CopyFileOrDirectory(src, destDirName, false);
        }
    }
}

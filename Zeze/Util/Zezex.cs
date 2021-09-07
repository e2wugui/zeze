using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;
using System.Xml;

namespace Zeze.Util
{
    public class Zezex
    {
        private string modules = "Login";
        private bool linkd = true;

        private string SolutionName = null;
        private string ServerProjectName = null;
        private string ClientProjectName = null;
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
                    case "-ClientProjectName": ClientProjectName = args[++i]; break;
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
            {
                Console.WriteLine($"SolutionName Need.");
                return false;
            }

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

            ExportModules();
        }

        private string ModuleExportType = "";

        private string GetClientHandleByClientLang()
        {
            if (string.IsNullOrEmpty(ClientLang))
                return null;

            switch (ClientLang)
            {
                case "cs":
                    return "client";
                default:
                    return "clientscript";
            }
        }

        private void UpdateProtocolClientHandle(XmlElement self, string clientHandle)
        {
            foreach (XmlNode n in self.ChildNodes)
            {
                if (XmlNodeType.Element != n.NodeType)
                    continue;

                XmlElement e = (XmlElement)n;
                switch (e.Name)
                {
                    case "protocol":
                    case "rpc":
                        var newHandle = e.GetAttribute("handle").Replace("client", clientHandle);
                        e.SetAttribute("handle", newHandle);
                        break;
                }
            }
        }

        private void UpdateClientServiceHandleName(XmlElement self, string clientHandle)
        {
            foreach (XmlNode n in self.ChildNodes)
            {
                if (XmlNodeType.Element != n.NodeType)
                    continue;

                XmlElement e = (XmlElement)n;
                switch (e.Name)
                {
                    case "service":
                        if (e.GetAttribute("name").Equals("Client"))
                        {
                            e.SetAttribute("handle", clientHandle);
                        }
                        break;
                }
            }
        }

        private void UpdateProject(XmlElement e, string clientHandle)
        {
            switch (e.GetAttribute("name"))
            {
                case "server":
                    if (false == string.IsNullOrEmpty(ServerProjectName))
                        e.SetAttribute("name", ServerProjectName);
                    break;

                case "client":
                    // 需要更多参数，client采用脚本的话，目录组织可能很不一样。
                    // 以后实际使用的时候再支持。
                    if (false == string.IsNullOrEmpty(ClientProjectName))
                        e.SetAttribute("name", ClientProjectName);
                    if (false == string.IsNullOrEmpty(clientHandle))
                        UpdateClientServiceHandleName(e, clientHandle);
                    break;

                default:
                    e.ParentNode.RemoveChild(e);
                    break;
            }
        }

        private void ExportModules()
        {
            var exportModules = new HashSet<string>();

            foreach (var m in modules.Split(","))
            {
                if (m.Equals("all") || m.Equals("none"))
                {
                    if (ModuleExportType != null)
                        throw new Exception($"ModuleExportType has setup with '{ModuleExportType}'");
                    ModuleExportType = m;
                }
                else if (false == string.IsNullOrEmpty(m))
                {
                    exportModules.Add(m);
                }
            }

            if (ModuleExportType.Equals("none"))
                return;

            var solutionXmlFile = "solution.xml";
            XmlDocument doc = new XmlDocument();
            doc.PreserveWhitespace = true;
            doc.Load(Path.Combine(ZezexDirectory, solutionXmlFile));

            XmlElement self = doc.DocumentElement;
            self.SetAttribute("name", SolutionName);

            if (ModuleExportType.Equals("all"))
            {
                if (exportModules.Count > 0)
                    throw new Exception("-modules all must present along.");
            }

            // update document.
            var clientHandle = GetClientHandleByClientLang();
            for (int i = self.ChildNodes.Count - 1; i >= 0; --i)
            {
                XmlNode child = self.ChildNodes[i];

                if (XmlNodeType.Element != child.NodeType)
                    continue;

                XmlElement e = (XmlElement)child;
                switch (e.Name)
                {
                    case "module":
                        if (false == ModuleExportType.Equals("all")
                            && false == exportModules.Contains(e.GetAttribute("name")))
                        {
                            self.RemoveChild(child);
                        }
                        else if (false == string.IsNullOrEmpty(clientHandle))
                        {
                            UpdateProtocolClientHandle(e, clientHandle);
                        }
                        break;

                    case "project":
                        UpdateProject(e, clientHandle);
                        break;
                }
            }

            using (TextWriter sw = new StreamWriter(
                Path.Combine(ExportDirectory, solutionXmlFile),
                false, Encoding.UTF8))
            {
                doc.Save(sw);
            }
        }

        private void ExportLinkd()
        {
            CopyTo("solution.linkd.xml", ExportDirectory);

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

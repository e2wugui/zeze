using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;
using System.Xml;
using System.Diagnostics;

namespace Zeze.Util
{
    public class Zezex
    {
        private string modules = "Login";
        private bool linkd = true;

        private string SolutionName = null;
        private string ServerProjectName = "server";
        private string ClientProjectName = "client";
        private string ClientPlatform = null;
        private string ExportDirectory = "../../";
        private readonly string ZezexDirectory = "./";

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
                    case "-ClientPlatform": ClientPlatform = args[++i]; break;
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
            Console.WriteLine("    [-ServerProjectName server] default='server'");
            Console.WriteLine("    [-ClientPlatform cs|...] no change if not present");

            Console.WriteLine("    [-nolinkd] do not export linkd");
            Console.WriteLine("    [-modules ma,mb] default='login'");
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
                FirstExportVersion = File.ReadAllLines(Path.Combine(ExportDirectory, "FirstExport.Version"))[0];
            }
            if (false == Directory.Exists(ZezexDirectory))
            {
                Console.WriteLine($"ZezexDirectory Not Exist: Path={ZezexDirectory}");
                return false;
            }
            return true;
        }

        private string FirstExportVersion = null;

        private void GitCheckout(string tag)
        {
            Process proc = new Process();
            proc.StartInfo.FileName = Path.Combine(ZezexDirectory, "gitcheckout.bat");
            proc.StartInfo.Arguments = $"\"{ZezexDirectory}\" \"{tag}\"";
            proc.StartInfo.UseShellExecute = false;
            proc.StartInfo.CreateNoWindow = true;
            proc.Start();
            proc.WaitForExit();
        }

        public void Export()
        {
            // prepare
            Directory.CreateDirectory(ExportDirectory);

            GitCheckout(""); // NewestRelease
            ParseModulesAndTryExportSolutionXml();

            // phase 1
            IsNewest = true;
            ExportLinkd();
            CopyModulesSource();
            CopyClientSource(); // 最后输出。

            GitCheckout(FirstExportVersion);

            // phase 2
            IsNewest = false;
            ExportLinkd();
            CopyModulesSource();            
            CopyClientSource(); // 最后输出。

            // end
            SaveFilesNow();
            GitCheckout("master");
        }

        private void CopyClientSource()
        {
            switch (ClientPlatform)
            {
                default:
                    Console.WriteLine("ClientPlatform TODO prepare all");
                    break;
            }
        }

        private string ModuleExportType = "";

        private string GetClientHandleByClientLang()
        {
            if (string.IsNullOrEmpty(ClientPlatform))
                return null;

            // see Zeze.Gen.Project.cs
            switch (ClientPlatform)
            {
                case "cs":
                    return "client";

                case "lua":
                case "cs+lua":
                case "cxx+lua":
                case "ts":
                case "cs+ts":
                case "cxx+ts":
                    return "clientscript";

                default:
                    throw new Exception($"unknown ClientPlatform={ClientPlatform}");
            }
        }

        private void UpdateProtocolClientHandle(XmlElement self)
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
                        var newHandle = e.GetAttribute("handle").Replace("client", ClientHandle);
                        e.SetAttribute("handle", newHandle);
                        break;
                }
            }
        }

        private void UpdateClientServiceHandleName(XmlElement self)
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
                            e.SetAttribute("handle", ClientHandle);
                            // TODO more params
                            e.SetAttribute("platform", ClientPlatform);
                        }
                        break;
                }
            }
        }

        private void RemoveServiceRef(XmlElement project, String skipRef)
        {
            for (int i = project.ChildNodes.Count - 1; i >= 0; --i)
            {
                XmlNode node = project.ChildNodes[i];
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                switch (e.Name)
                {
                    case "service":
                        for (int j = e.ChildNodes.Count - 1; j >= 0; --j)
                        {
                            XmlNode noderef = e.ChildNodes[j];
                            if (XmlNodeType.Element != noderef.NodeType)
                                continue;

                            XmlElement eref = (XmlElement)noderef;
                            switch (eref.Name)
                            {
                                case "module":
                                    var refName = eref.GetAttribute("ref");
                                    if (false == ModulesExported.Contains(refName)
                                        && false == refName.Equals(skipRef))
                                    {
                                        e.RemoveChild(eref);
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
        }

        private void UpdateProject(XmlElement e)
        {
            switch (e.GetAttribute("name"))
            {
                case "server":
                    e.SetAttribute("name", ServerProjectName);
                    RemoveServiceRef(e, "Zezex.Provider");
                    break;

                case "client":
                    e.SetAttribute("name", ClientProjectName);
                    if (false == string.IsNullOrEmpty(ClientHandle))
                        UpdateClientServiceHandleName(e);
                    RemoveServiceRef(e, "Zezex.Linkd");
                    e.ParentNode.RemoveChild(e); // TODO 实现Client时，去掉这一行。
                    break;

                default:
                    e.ParentNode.RemoveChild(e);
                    break;
            }
        }

        private HashSet<string> ModulesExported = new HashSet<string>();
        private string ClientHandle = null;

        private void ParseModulesAndTryExportSolutionXml()
        {
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
                    ModulesExported.Add(m);
                }
            }

            if (ModulesExported.Count > 0)
            {
                if (ModuleExportType.Equals("none") || ModuleExportType.Equals("all"))
                    throw new Exception("-modules none|all|ma,mb,mc");
            }

            var solutionXmlFile = "solution.xml";
            XmlDocument doc = new XmlDocument();
            doc.PreserveWhitespace = true;
            doc.Load(Path.Combine(ZezexDirectory, solutionXmlFile));

            XmlElement self = doc.DocumentElement;
            self.SetAttribute("name", SolutionName);

            // update document.
            ClientHandle = GetClientHandleByClientLang();
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
                            && false == ModulesExported.Contains(e.GetAttribute("name")))
                        {
                            self.RemoveChild(child);
                        }
                        else if (false == string.IsNullOrEmpty(ClientHandle))
                        {
                            UpdateProtocolClientHandle(e);
                        }
                        break;

                    case "project":
                        UpdateProject(e);
                        break;
                }
            }

            var targetXmlFile = Path.Combine(ExportDirectory, solutionXmlFile);
            if (File.Exists(targetXmlFile))
            {
                Console.WriteLine($"{solutionXmlFile} Has Exist In Export Directory. Skip!");
            }
            else
            {
                using (TextWriter sw = new StreamWriter(
                    targetXmlFile,
                    false, Encoding.UTF8))
                {
                    doc.Save(sw);
                }
            }
        }

        private string GetServerProjectName()
        {
            return string.IsNullOrEmpty(ServerProjectName) ? "server" : ServerProjectName;
        }

        private void CopyModulesSource()
        {
            ReplaceAndCopyTo("gen.bat", ExportDirectory);

            var serverName = GetServerProjectName();
            var serverDir = Path.Combine(ExportDirectory, serverName);
            Directory.CreateDirectory(serverDir);

            ReplaceAndCopyTo("server/Program.cs", serverDir);
            ReplaceAndCopyTo("server/server.csproj", Path.Combine(serverDir, $"{serverName}.csproj"));
            CopyTo("server/zeze.xml", serverDir);

            ReplaceAndCopyTo("server/Zezex", serverDir);

            var moduleBasedir = Path.Combine(serverDir, SolutionName);
            Directory.CreateDirectory(moduleBasedir);

            ReplaceAndCopyTo($"server/Game/App.cs", moduleBasedir);
            ReplaceAndCopyTo($"server/Game/Config.cs", moduleBasedir);
            ReplaceAndCopyTo($"server/Game/Load.cs", moduleBasedir);
            ReplaceAndCopyTo($"server/Game/Server.cs", moduleBasedir);

            foreach (var m in ModulesExported)
            {
                ReplaceAndCopyTo($"server/Game/{m}", moduleBasedir);
            }
        }

        private void ReplaceAndCopyTo(string relativePath, string destDir)
        {
            var src = Path.Combine(ZezexDirectory, relativePath);
            FileSystem.CopyFileOrDirectory(src, destDir, 
                (srcFile, dstFileName) =>
                {
                    var source = File.ReadAllText(srcFile.FullName, Encoding.UTF8);

                    source = source.Replace("namespace server", $"namespace {GetServerProjectName()}");
                    source = source.Replace("namespace Game", $"namespace {SolutionName}");
                    source = source.Replace("Game.", $"{SolutionName}.");
                    source = source.Replace("Game_", $"{SolutionName}_");

                    source = source.Replace("Include=\"..\\..\\Zeze\\Zeze.csproj\"", "Include=\"..\\..\\zeze\\Zeze\\Zeze.csproj\"");
                    source = source.Replace("..\\Gen\\bin\\", "..\\zeze\\Gen\\bin\\");

                    AddOrUpdateFileCopings(relativePath, source, destDir);
                });
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

        /// <summary>
        /// NewestRelease FirstExport TargetCurrent
        /// null
        /// </summary>
        /// 输出文件
        class FileCoping
        {
            // 最新发行版本内容
            public string NewestRelease { get; set; }
            // 第一次导出时的版本内容
            public string FirstExport { get; set; }
            // 目标文件当前的内容
            public string TargetCurrent { get; set; }
        }

        private void SaveFilesNow()
        {
            // 0=null 1=exist
            // _ N F Result
            // 0 0 0 Error
            // 1 0 1 文件已经不再需要导出。
            // 2 1 0 新增的导出文件。
            // 3 1 1 可能需要更新的导出文件。
            foreach (var e in FileCopings)
            {
                var N = e.Value.NewestRelease == null ? 0 : 1;
                var F = e.Value.FirstExport == null ? 0 : 1;
                switch ((N << 1) | F)
                {
                    case 0:
                        throw new Exception("Impossible!");

                    case 1:
                        TryDelete(e.Key, e.Value);
                        break;

                    case 2:
                        TryNew(e.Key, e.Value);
                        break;

                    case 3:
                        TryUpdate(e.Key, e.Value);
                        break;
                }
            }
        }

        private void TryUpdate(string relativePath, FileCoping file)
        {
            if (null == file.TargetCurrent)
            {
                var newpath = relativePath + ".TryUpdateButDeleted";
                var dstFileName = Path.Combine(ExportDirectory, newpath);
                File.WriteAllText(dstFileName, file.NewestRelease, Encoding.UTF8);
                Console.WriteLine($"TryUpdate '{relativePath}'. Not Exist. SaveAs={newpath}");
            }
            else if (file.FirstExport.Equals(file.TargetCurrent))
            {
                var dstFileName = Path.Combine(ExportDirectory, relativePath);
                File.WriteAllText(dstFileName, file.NewestRelease, Encoding.UTF8);
                Console.WriteLine($"TryUpdate '{relativePath}'. Ok.");
            }
            else
            {
                var newpath = relativePath + ".TryUpdateButChanged";
                var dstFileName = Path.Combine(ExportDirectory, newpath);
                File.WriteAllText(dstFileName, file.NewestRelease, Encoding.UTF8);
                Console.WriteLine($"TryUpdate '{relativePath}'. Has Changed. SaveAs={newpath}");
            }
        }

        private void TryNew(string relativePath, FileCoping file)
        {
            if (null == file.TargetCurrent)
            {
                var dstFileName = Path.Combine(ExportDirectory, relativePath);
                File.WriteAllText(dstFileName, file.NewestRelease, Encoding.UTF8);
                Console.WriteLine($"TryNew '{relativePath}'. Ok.");
            }
            else
            {
                var newpath = relativePath + ".TryNew";
                var dstFileName = Path.Combine(ExportDirectory, newpath);
                File.WriteAllText(dstFileName, file.NewestRelease, Encoding.UTF8);
                Console.WriteLine($"TryNew '{relativePath}'. Has Exist. SaveAs={newpath}");
            }
        }

        private void TryDelete(string relativePath, FileCoping file)
        {
            if (null == file.TargetCurrent)
            {
                Console.WriteLine($"TryDelete '{relativePath}'. Has Deleted.");
            }
            else if (file.FirstExport.Equals(file.TargetCurrent))
            {
                // delete
                File.Delete(Path.Combine(ExportDirectory, relativePath));
                Console.WriteLine($"TryDelete '{relativePath}'. Ok.");
            }
            else
            {
                Console.WriteLine($"TryDelete '{relativePath}'. But Is Has Changed Since First Export.");
            }
        }

        bool IsNewest;

        Dictionary<string, FileCoping> FileCopings = new Dictionary<string, FileCoping>();

        private FileCoping AddToFileCopings(bool isNewest, string relativePath, string srcText, string destDirName)
        {
            var targetFile = Path.Combine(destDirName, relativePath);
            var targetText = File.Exists(targetFile)
                ? File.ReadAllText(targetFile, Encoding.UTF8)
                : null;

            var fc = isNewest
                ? new FileCoping() { NewestRelease = srcText, TargetCurrent = targetText, }
                : new FileCoping() { FirstExport = srcText, TargetCurrent = targetText, };

            FileCopings.Add(relativePath, fc);
            return fc;
        }

        private void AddOrUpdateFileCopings(string relativePath, string srcText, string destDirName)
        {
            // 下面的分支按处理步骤走，不要首先根据FileCopings.TryGetValue的结果。
            if (IsNewest)
            {
                AddToFileCopings(true, relativePath, srcText, destDirName);
            }
            else
            {
                if (FileCopings.TryGetValue(relativePath, out var exist))
                {
                    exist.FirstExport = srcText;
                }
                else
                {
                    // 第一次输出时存在这个文件，但是最新版本里面没有这个文件。
                    AddToFileCopings(false, relativePath, srcText, destDirName);
                }
            }
        }

        private void CopyTo(string relativePath, string destDirName)
        {
            var src = Path.Combine(ZezexDirectory, relativePath);
            FileSystem.CopyFileOrDirectory(src, destDirName,
                (srcFile, dstFileName) =>
                {
                    var srcText = File.ReadAllText(src, Encoding.UTF8);
                    AddOrUpdateFileCopings(relativePath, srcText, destDirName);
                });
        }
    }
}

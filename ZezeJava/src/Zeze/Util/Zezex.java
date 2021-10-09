package Zeze.Util;

import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Zezex {
	private String modules = "Login";
	private Encoding utf8NoBom = new UTF8Encoding(false);

	private String SolutionName = null;
	private String ServerProjectName = "server";
	private String ClientProjectName = "client";
	private String ClientPlatform = null;
	private String ExportDirectory = "../../";
	private final String ZezexDirectory = "./";

	private Zezex(String[] args) {
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
				case "-SolutionName":
					SolutionName = args[++i];
					break;
				case "-ExportDirectory":
					ExportDirectory = args[++i];
					break;
				case "-ZezexDirectory":
					ZezexDirectory = args[++i];
					break;
				case "-ServerProjectName":
					ServerProjectName = args[++i];
					break;
				case "-ClientProjectName":
					ClientProjectName = args[++i];
					break;
				case "-ClientPlatform":
					ClientPlatform = args[++i];
					break;
				case "-modules":
					modules = args[++i];
					break;
			}
		}
	}

	private static void Usage() {
		System.out.println("args:");
		System.out.println("    [-c zezex] Must Present To Run Zezex Export");
		System.out.println("    [-SolutionName Game] Must Present");
		System.out.println("    [-ExportDirectory Path] default='../../'");
		System.out.println("    [-ZezexDirectory Path] default='./'");
		System.out.println("    [-ServerProjectName server] default='server'");
		System.out.println("    [-ClientPlatform cs|...] no change if not present");

		System.out.println("    [-modules ma,mb] default='login'");
		System.out.println("    [-modules all] export all modules");
		System.out.println("    [-modules none] export none module");
	}

	public static void main(String[] args) {
		var x = new Zezex(args);

		if (false == x.VerifyParams()) {
			Usage();
			return;
		}

		x.Export();
	}

	private boolean VerifyParams() {
		if (tangible.StringHelper.isNullOrEmpty(SolutionName)) {
			System.out.println(String.format("SolutionName Need."));
			return false;
		}

		if (false == (new File(ExportDirectory)).isDirectory()) {
			System.out.println(String.format("ExportDirectory Not Exist: Path=%1$s", ExportDirectory));
			return false;
		}
		ExportDirectory = Paths.get(ExportDirectory).resolve(SolutionName).toString();
		if ((new File(ExportDirectory)).isDirectory()) {
			FirstExportVersion = File.ReadAllLines(Paths.get(ExportDirectory).resolve("FirstExport.Version").toString())[0];
		}
		if (false == (new File(ZezexDirectory)).isDirectory()) {
			System.out.println(String.format("ZezexDirectory Not Exist: Path=%1$s", ZezexDirectory));
			return false;
		}
		return true;
	}

	private String FirstExportVersion = null;

	private void GitCheckout(String tag) {
		Process proc = new Process();
		proc.StartInfo.FileName = Paths.get(ZezexDirectory).resolve("gitcheckout.20210913.tmp.bat").toString();
		proc.StartInfo.Arguments = String.format("\"%1$s\" \"%2$s\"", ZezexDirectory, tag);
		proc.StartInfo.UseShellExecute = false;
		proc.StartInfo.CreateNoWindow = true;
		proc.Start();
		proc.WaitForExit();
		if (proc.ExitCode != 0) {
			throw new RuntimeException("gitcheckout.bat ExitCode != 0");
		}
	}

	public final void Export() {
		// prepare
		try {
			(new File(ExportDirectory)).mkdirs();

			// phase 0
			Files.copy(Paths.get("gitcheckout.bat"), Paths.get("gitcheckout.20210913.tmp.bat"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
			ParseModulesAndTryExportSolutionXml();

			// phase 1
			GitCheckout(""); // NewestRelease
			IsNewest = true;
			ExportLinkd();
			CopyModulesSource();
			CopyClientSource(); // 最后输出。

			// phase 2
			if (!tangible.StringHelper.isNullOrEmpty(FirstExportVersion)) {
				GitCheckout(FirstExportVersion);
				IsNewest = false;
				ExportLinkd();
				CopyModulesSource();
				CopyClientSource(); // 最后输出。
			}

			// phase end
			SaveFilesNow();
		}
		finally {
			// restore git
			GitCheckout("master");
			(new File("gitcheckout.20210913.tmp.bat")).delete();
		}
	}

	private void CopyClientSource() {
		switch (ClientPlatform) {
			default:
				System.out.println("ClientPlatform TODO prepare all");
				break;
		}
	}

	private String ModuleExportType = "";

	private String GetClientHandleByClientLang() {
		if (tangible.StringHelper.isNullOrEmpty(ClientPlatform)) {
			return null;
		}

		// see Zeze.Gen.Project.cs
		switch (ClientPlatform) {
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
				throw new RuntimeException(String.format("unknown ClientPlatform=%1$s", ClientPlatform));
		}
	}

	private void UpdateProtocolClientHandle(XmlElement self) {
		for (XmlNode n : self.ChildNodes) {
			if (XmlNodeType.Element != n.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)n;
			switch (e.Name) {
				case "protocol":
				case "rpc":
					var newHandle = e.GetAttribute("handle").replace("client", ClientHandle);
					e.SetAttribute("handle", newHandle);
					break;
			}
		}
	}

	private void UpdateClientServiceHandleName(XmlElement self) {
		for (XmlNode n : self.ChildNodes) {
			if (XmlNodeType.Element != n.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)n;
			switch (e.Name) {
				case "service":
					if (e.GetAttribute("name").equals("Client")) {
						e.SetAttribute("handle", ClientHandle);
						// TODO more params
						e.SetAttribute("platform", ClientPlatform);
					}
					break;
			}
		}
	}

	private void RemoveServiceRef(XmlElement project, String skipRef) {
		for (int i = project.ChildNodes.Count - 1; i >= 0; --i) {
			XmlNode node = project.ChildNodes[i];
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "service":
					for (int j = e.ChildNodes.Count - 1; j >= 0; --j) {
						XmlNode noderef = e.ChildNodes[j];
						if (XmlNodeType.Element != noderef.NodeType) {
							continue;
						}

						XmlElement eref = (XmlElement)noderef;
						switch (eref.Name) {
							case "module":
								var refName = eref.GetAttribute("ref");
								if (false == ModulesExported.contains(refName) && false == refName.equals(skipRef)) {
									e.RemoveChild(eref);
								}
								break;
						}
					}
					break;
			}
		}
	}

	private void UpdateProject(XmlElement e) {
		switch (e.GetAttribute("name")) {
			case "server":
				e.SetAttribute("name", ServerProjectName);
				RemoveServiceRef(e, "Zezex.Provider");
				break;

			case "client":
				e.SetAttribute("name", ClientProjectName);
				if (false == tangible.StringHelper.isNullOrEmpty(ClientHandle)) {
					UpdateClientServiceHandleName(e);
				}
				RemoveServiceRef(e, "Zezex.Linkd");
				e.ParentNode.RemoveChild(e); // TODO 实现Client时，去掉这一行。
				break;

			default:
				e.ParentNode.RemoveChild(e);
				break;
		}
	}

	private HashSet<String> ModulesExported = new HashSet<String>();
	private String ClientHandle = null;

	private void ParseModulesAndTryExportSolutionXml() {
		for (var m : modules.split(java.util.regex.Pattern.quote(","), -1)) {
			if (m.equals("all") || m.equals("none")) {
				if (ModuleExportType != null) {
					throw new RuntimeException(String.format("ModuleExportType has setup with '%1$s'", ModuleExportType));
				}
				ModuleExportType = m;
			}
			else if (false == tangible.StringHelper.isNullOrEmpty(m)) {
				ModulesExported.add(m);
			}
		}

		if (!ModulesExported.isEmpty()) {
			if (ModuleExportType.equals("none") || ModuleExportType.equals("all")) {
				throw new RuntimeException("-modules none|all|ma,mb,mc");
			}
		}

		var solutionXmlFile = "solution.xml";
		XmlDocument doc = new XmlDocument();
		doc.PreserveWhitespace = true;
		doc.Load(Paths.get(ZezexDirectory).resolve(solutionXmlFile).toString());

		XmlElement self = doc.DocumentElement;
		self.SetAttribute("name", SolutionName);

		// update document.
		ClientHandle = GetClientHandleByClientLang();
		for (int i = self.ChildNodes.Count - 1; i >= 0; --i) {
			XmlNode child = self.ChildNodes[i];

			if (XmlNodeType.Element != child.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)child;
			switch (e.Name) {
				case "module":
					if (false == ModuleExportType.equals("all") && false == ModulesExported.contains(e.GetAttribute("name"))) {
						self.RemoveChild(child);
					}
					else if (false == tangible.StringHelper.isNullOrEmpty(ClientHandle)) {
						UpdateProtocolClientHandle(e);
					}
					break;

				case "project":
					UpdateProject(e);
					break;
			}
		}

		var targetXmlFile = Paths.get(ExportDirectory).resolve(solutionXmlFile).toString();
		if ((new File(targetXmlFile)).isFile()) {
			System.out.println(String.format("%1$s Has Exist In Export Directory. Skip!", solutionXmlFile));
		}
		else {
			try (TextWriter sw = new OutputStreamWriter(targetXmlFile)) {
				doc.Save(sw);
			}
		}
	}

	private String GetServerProjectName() {
		return tangible.StringHelper.isNullOrEmpty(ServerProjectName) ? "server" : ServerProjectName;
	}

	private void CopyModulesSource() {
		ReplaceAndCopyTo("gen.bat", ExportDirectory);

		var serverName = GetServerProjectName();
		var serverDir = Paths.get(ExportDirectory).resolve(serverName).toString();
		(new File(serverDir)).mkdirs();

		ReplaceAndCopyTo("server/Program.cs", serverDir);
		ReplaceAndCopyTo("server/server.csproj", Paths.get(serverDir).resolve(String.format("%1$s.csproj", serverName)).toString());
		ReplaceAndCopyTo("server/zeze.xml", serverDir);

		ReplaceAndCopyTo("server/Zezex", serverDir);

		var moduleBasedir = Paths.get(serverDir).resolve(SolutionName).toString();
		(new File(moduleBasedir)).mkdirs();

		ReplaceAndCopyTo(String.format("server/Game/App.cs"), moduleBasedir);
		ReplaceAndCopyTo(String.format("server/Game/Config.cs"), moduleBasedir);
		ReplaceAndCopyTo(String.format("server/Game/Load.cs"), moduleBasedir);
		ReplaceAndCopyTo(String.format("server/Game/Server.cs"), moduleBasedir);

		for (var m : ModulesExported) {
			ReplaceAndCopyTo(String.format("server/Game/%1$s", m), moduleBasedir);
		}
	}

	private void ReplaceAndCopyTo(String relativePath, String destDir) {
		var src = Paths.get(ZezexDirectory).resolve(relativePath).toString();
		FileSystem.CopyFileOrDirectory(src, destDir, (srcFile, dstFileName) -> {
					var source = Files.readString(srcFile.FullName, java.nio.charset.StandardCharsets.UTF_8);

					source = source.replace("namespace server", String.format("namespace %1$s", GetServerProjectName()));
					source = source.replace("namespace Game", String.format("namespace %1$s", SolutionName));
					source = source.replace("Game.", String.format("%1$s.", SolutionName));
					source = source.replace("Game_", String.format("%1$s_", SolutionName));

					source = source.replace("Include=\"..\\..\\Zeze\\Zeze.csproj\"", "Include=\"..\\..\\zeze\\Zeze\\Zeze.csproj\"");
					source = source.replace("..\\Gen\\bin\\", "..\\zeze\\Gen\\bin\\");

					AddOrUpdateFileCopings(source, srcFile, dstFileName);
		});
	}

	private void ExportLinkd() {
		ReplaceAndCopyTo("solution.linkd.xml", ExportDirectory);

		var linkdDir = Paths.get(ExportDirectory).resolve("linkd").toString();
		(new File(linkdDir)).mkdirs();
		ReplaceAndCopyTo("linkd/Zezex", linkdDir);

		ReplaceAndCopyTo("linkd/linkd.csproj", linkdDir);
		ReplaceAndCopyTo("linkd/Program.cs", linkdDir);
		ReplaceAndCopyTo("linkd/zeze.xml", linkdDir);
	}

	/** 
	 NewestRelease FirstExport TargetCurrent
	 null
	 
	 输出文件
	*/
	private static class FileCoping {
		// 最新发行版本内容
		private String NewestRelease;
		public final String getNewestRelease() {
			return NewestRelease;
		}
		public final void setNewestRelease(String value) {
			NewestRelease = value;
		}
		// 第一次导出时的版本内容
		private String FirstExport;
		public final String getFirstExport() {
			return FirstExport;
		}
		public final void setFirstExport(String value) {
			FirstExport = value;
		}
		// 目标文件相对ExportDirectory的名字。
		private String RelativeDstFile;
		public final String getRelativeDstFile() {
			return RelativeDstFile;
		}
		public final void setRelativeDstFile(String value) {
			RelativeDstFile = value;
		}
	}

	private void SaveFilesNow() {
		// 0=null 1=exist
		// _ N F Result
		// 0 0 0 Error
		// 1 0 1 文件已经不再需要导出。
		// 2 1 0 新增的导出文件。
		// 3 1 1 可能需要更新的导出文件。
		for (var e : FileCopings.entrySet()) {
			var N = e.getValue().NewestRelease == null ? 0 : 1;
			var F = e.getValue().FirstExport == null ? 0 : 1;
			switch ((N << 1) | F) {
				case 0:
					throw new RuntimeException("Impossible!");

				case 1:
					TryDelete(e.getValue());
					break;

				case 2:
					TryNew(e.getValue());
					break;

				case 3:
					TryUpdate(e.getValue());
					break;
			}
		}
	}

	private void TryUpdate(FileCoping file) {
		var dstFileName = Paths.get(ExportDirectory).resolve(file.getRelativeDstFile()).toString();
		if (false == (new File(dstFileName)).isFile()) {
			var msg = file.getFirstExport().equals(file.getNewestRelease()) ? "NewExport Or Restore" : "Update Need But Deleted";
			Files.writeString(dstFileName, file.getNewestRelease(),);
			System.out.println(String.format("TryUpdate [Ok] '%1$s'. %2$s", file.getRelativeDstFile(), msg));
			return;
		}

		var dstText = Files.readString(dstFileName, java.nio.charset.StandardCharsets.UTF_8);
		if (file.getFirstExport().equals(dstText)) {
			Files.writeString(dstFileName, file.getNewestRelease(),);
			System.out.println(String.format("TryUpdate [Ok] '%1$s'.", file.getRelativeDstFile()));
			return;
		}

		if (file.getFirstExport().equals(file.getNewestRelease())) {
			System.out.println(String.format("TryUpdate [Ok] '%1$s'. Changed Since FirstExport But Export Do Not Change.", file.getRelativeDstFile()));
			return;
		}

		if (dstText.equals(file.getNewestRelease())) {
			System.out.println(String.format("TryUpdate [Ok] '%1$s'. DstFile.Equals(NewestRelease).", file.getRelativeDstFile()));
			return;
		}

		var newpath = dstFileName + ".TryUpdateButChanged";
		Files.writeString(newpath, file.getNewestRelease(),);
		System.out.println(String.format("TryUpdate [Changed] '%1$s'. SaveAs=%2$s", file.getRelativeDstFile(), newpath));
	}

	private void TryNew(FileCoping file) {
		var dstFileName = Paths.get(ExportDirectory).resolve(file.getRelativeDstFile()).toString();
		if (false == (new File(dstFileName)).isFile()) {
			Files.writeString(dstFileName, file.getNewestRelease(),);
			System.out.println(String.format("TryNew [Ok] '%1$s'.", file.getRelativeDstFile()));
			return;
		}

		var newpath = dstFileName + ".TryNew";
		Files.writeString(newpath, file.getNewestRelease(),);
		System.out.println(String.format("TryNew [Exist] '%1$s'. SaveAs=%2$s", file.getRelativeDstFile(), newpath));
	}

	private void TryDelete(FileCoping file) {
		var dstFileName = Paths.get(ExportDirectory).resolve(file.getRelativeDstFile()).toString();
		if (false == (new File(dstFileName)).isFile()) {
			System.out.println(String.format("TryDelete [Has Deleted] '%1$s'.", file.getRelativeDstFile()));
			return;
		}

		var dstText = Files.readString(dstFileName, java.nio.charset.StandardCharsets.UTF_8);
		if (file.getFirstExport().equals(dstText)) {
			// delete
			(new File(dstText)).delete();
			System.out.println(String.format("TryDelete [Ok] '%1$s'.", file.getRelativeDstFile()));
			return;
		}

		System.out.println(String.format("TryDelete [Changed] '%1$s'. Changed Since FirstExport.", file.getRelativeDstFile()));
	}

	private boolean IsNewest;

	private HashMap<String, FileCoping> FileCopings = new HashMap<String, FileCoping>();

	private FileCoping AddToFileCopings(boolean isNewest, String srcText, String relativeSrcFile, String relativeDstFileName) {
		var fc = new FileCoping();
		if (isNewest) {
			fc.setNewestRelease(srcText);
		}
		else {
			fc.setFirstExport(srcText);
		}

		if (fc.getRelativeDstFile().equals(null)) { // 新旧版本的目标文件应该是一样的。这样写只是保险。
			fc.setRelativeDstFile(relativeDstFileName);
		}

		FileCopings.put(relativeSrcFile, fc);
		return fc;
	}

	private void AddOrUpdateFileCopings(String srcText, File srcFile, String dstFileName) {
		var relativeSrcFile = Path.GetRelativePath(ZezexDirectory, srcFile.getPath());
		var relativeDstFile = Path.GetRelativePath(ExportDirectory, dstFileName);

		// 下面的分支按处理步骤走，不要首先根据FileCopings.TryGetValue的结果。
		if (IsNewest) {
			AddToFileCopings(true, srcText, relativeSrcFile, relativeDstFile);
		}
		else {
			TValue exist;
			if (FileCopings.containsKey(relativeSrcFile) && (exist = FileCopings.get(relativeSrcFile)) == exist) {
				exist.FirstExport = srcText;
			}
			else {
				// 第一次输出时存在这个文件，但是最新版本里面没有这个文件。
				AddToFileCopings(false, srcText, relativeSrcFile, relativeDstFile);
			}
		}
	}

	/*
	private void CopyTo(string relativePath, string destDirName)
	{
	    var src = Path.Combine(ZezexDirectory, relativePath);
	    FileSystem.CopyFileOrDirectory(src, destDirName,
	        (srcFile, dstFileName) =>
	        {
	            var srcText = File.ReadAllText(srcFile.FullName, Encoding.UTF8);
	            AddOrUpdateFileCopings(srcText, srcFile, dstFileName);
	        });
	}
	*/
}
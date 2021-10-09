package Zeze.Gen.ts;

import NLog.Layouts.*;
import Zeze.*;
import Zeze.Gen.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class ModuleFormatter {
	private Project project;
	private Module module;
	private String genDir;

	public ModuleFormatter(Project project, Module module, String genDir) {
		this.project = project;
		this.module = module;
		this.genDir = genDir;
	}

	private static final String ChunkNameRegisterProtocol = "REGISTER PROTOCOL";
	private static final String ChunkNameImport = "IMPORT GEN";
	private static final String ChunkNameModuleEnums = "MODULE ENUMS";

	private void GenChunkByName(OutputStreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk) {
		switch (chunk.getName()) {
			case ChunkNameRegisterProtocol:
				RegisterProtocol(writer);
				break;
			case ChunkNameImport:
				Import(writer);
				break;
			case ChunkNameModuleEnums:
				PrintModuleEnums(writer);
				break;
			default:
				throw new RuntimeException("unknown Chunk.Name=" + chunk.getName());
		}
	}

	private void Import(OutputStreamWriter sw) {
		sw.write("import { Zeze } from \"zeze\"" + System.lineSeparator());
		var needp = NeedRegisterProtocold();
		if (!needp.isEmpty()) {
			StringBuilder importp = new StringBuilder();
			for (var p : needp) {
				importp.append(p.getSpace().Path("_", p.getName())).append(", ");
			}
			sw.write("import { " + importp.toString() + "} from \"gen\"" + System.lineSeparator());
		}
		sw.write("import { demo_App } from \"demo/App\"" + System.lineSeparator());
	}

	private ArrayList<Protocol> NeedRegisterProtocold() {
		ArrayList<Protocol> need = new ArrayList<Protocol>();
		Service serv = module.getReferenceService();
		if (serv == null) {
			return need;
		}

		int serviceHandleFlags = module.getReferenceService().getHandleFlags();
		for (Protocol p : module.getProtocols().values()) {
			boolean tempVar = p instanceof Rpc;
			Rpc rpc = tempVar ? (Rpc)p : null;
			if (tempVar) {
				// rpc 总是需要注册.
				need.add(p);
				continue;
			}
			if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags)) {
				need.add(p);
			}
		}
		return need;
	}

	private void RegisterProtocol(OutputStreamWriter sw) {
		Service serv = module.getReferenceService();
		if (serv == null) {
			return;
		}

		int serviceHandleFlags = module.getReferenceService().getHandleFlags();
		for (Protocol p : module.getProtocols().values()) {
			String fullName = p.getSpace().Path("_", p.getName());
			String factory = "() => { return new " + fullName + "(); }";
			boolean tempVar = p instanceof Rpc;
			Rpc rpc = tempVar ? (Rpc)p : null;
			if (tempVar) {
				String handle = ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags) != 0) ? "this.Process" + rpc.getName() + "Request.bind(this)" : "null";
				sw.write(String.format("        app.%1$s.FactoryHandleMap.set(%2$s, new Zeze.ProtocolFactoryHandle(%3$s, %4$s));", serv.getName(), rpc.getTypeId(), factory, handle) + System.lineSeparator());
				continue;
			}
			if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags)) {
				String handle = "this.Process" + p.getName() + ".bind(this)";
				sw.write(String.format("        app.%1$s.FactoryHandleMap.set(%2$s, new Zeze.ProtocolFactoryHandle(%3$s, %4$s));", serv.getName(), p.getTypeId(), factory, handle) + System.lineSeparator());
			}
		}
	}

	private void PrintModuleEnums(OutputStreamWriter sw) {
		for (Types.Enum e : module.getEnums()) {
			sw.write(String.valueOf(String.format("    static readonly %1$s = %2$s; %3$s", e.getName(), e.getValue(), e.getComment())) + System.lineSeparator());
		}
		if (!module.getEnums().isEmpty()) {
			sw.WriteLine();
		}
	}

	public final void Make() {
		Zeze.Util.FileChunkGen fcg = new Util.FileChunkGen();
		String fullDir = module.GetFullPath(genDir, null);
		String fullFileName = Paths.get(fullDir).resolve(String.format("Module%1$s.ts", module.getName())).toString();
		if (fcg.LoadFile(fullFileName)) {
			fcg.SaveFile(fullFileName, ::GenChunkByName);
		}
		else {
			// new file
			(new File(fullDir)).mkdirs();
			try (OutputStreamWriter sw = new OutputStreamWriter(fullFileName, java.nio.charset.StandardCharsets.UTF_8)) {
				sw.WriteLine();
				sw.write(fcg.getChunkStartTag() + " " + ChunkNameImport + System.lineSeparator());
				Import(sw);
				sw.write(fcg.getChunkEndTag() + " " + ChunkNameImport + System.lineSeparator());
				sw.WriteLine();
				sw.write("export class " + module.Path("_", null) + " {" + System.lineSeparator());
				sw.write("        " + fcg.getChunkStartTag() + " " + ChunkNameModuleEnums + System.lineSeparator());
				PrintModuleEnums(sw);
				sw.write("        " + fcg.getChunkEndTag() + " " + ChunkNameModuleEnums + System.lineSeparator());
				sw.write("    public constructor(app: " + module.getSolution().getName() + "_App) {" + System.lineSeparator());
				sw.write("        " + fcg.getChunkStartTag() + " " + ChunkNameRegisterProtocol + System.lineSeparator());
				RegisterProtocol(sw);
				sw.write("        " + fcg.getChunkEndTag() + " " + ChunkNameRegisterProtocol + System.lineSeparator());
				sw.write("    }" + System.lineSeparator());
				sw.write("" + System.lineSeparator());
				sw.write("    public Start(app: " + module.getSolution().getName() + "_App): void {" + System.lineSeparator());
				sw.write("    }" + System.lineSeparator());
				sw.write("" + System.lineSeparator());
				sw.write("    public Stop(app: " + module.getSolution().getName() + "_App): void {" + System.lineSeparator());
				sw.write("    }" + System.lineSeparator());
				sw.write("" + System.lineSeparator());
				if (module.getReferenceService() != null) {
					int serviceHandleFlags = module.getReferenceService().getHandleFlags();
					for (Protocol p : module.getProtocols().values()) {
						String fullName = p.getSpace().Path("_", p.getName());
						boolean tempVar = p instanceof Rpc;
						Rpc rpc = tempVar ? (Rpc)p : null;
						if (tempVar) {
							if ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags) != 0) {
								sw.write("    public Process" + rpc.getName() + "Request(rpc: " + fullName + "): number {" + System.lineSeparator());
								sw.write("        return 0;" + System.lineSeparator());
								sw.write("    }" + System.lineSeparator());
								sw.write("" + System.lineSeparator());
							}
							continue;
						}
						if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags)) {
							sw.write("    public Process" + p.getName() + "(protocol: " + fullName + "): number {" + System.lineSeparator());
							sw.write("        return 0;" + System.lineSeparator());
							sw.write("    }" + System.lineSeparator());
							sw.write("" + System.lineSeparator());
						}
					}
				}
				sw.write("}" + System.lineSeparator());
			}
		}
	}
}
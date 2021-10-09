package Zeze.Gen.lua;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;
import java.nio.file.*;

public class ModuleFormatter {
	private Project project;
	private ModuleSpace module;
	private String genDir;
	private String srcDir;

	public ModuleFormatter(Project project, ModuleSpace module, String genDir, String srcDir) {
		this.project = project;
		this.module = module;
		this.genDir = genDir;
		this.srcDir = srcDir;
	}

	public final void Make() {
		MakeGen();
		MakeSrc();
	}

	public final void MakeGen() {
		try (OutputStreamWriter sw = (null == module.getParent()) ? new OutputStreamWriter(Paths.get(genDir).resolve(module.getName() + ".lua").toString(), java.nio.charset.StandardCharsets.UTF_8) : module.getParent().OpenWriter(genDir, module.getName() + ".lua", true)) {
			MakeGen(sw);
			sw.WriteLine();
			sw.write("return " + module.getName() + "" + System.lineSeparator());
		}
	}

	public final void MakeGen(OutputStreamWriter sw) {
		sw.write("-- auto-generated" + System.lineSeparator());
		sw.WriteLine();
		sw.write("local " + module.getName() + " = {}" + System.lineSeparator());
		//sw.WriteLine("" + module.Name + ".ModuleId = " + module.Id);
		sw.WriteLine();
		// declare enums
		sw.write(String.valueOf(String.format("%1$s.ResultCode = {", module.getName())) + System.lineSeparator());
		for (Types.Enum e : module.getEnums()) {
			sw.write(String.valueOf(String.format("    %1$s = %2$s, --%3$s", e.getName(), e.getValue(), e.getComment())) + System.lineSeparator());
		}
		sw.write(String.format("}}") + System.lineSeparator());
		for (var b : module.getBeanKeys().values()) {
			BeanFormatter.Make(module.getName(), b.Name, b.TypeId, b.Variables, b.Enums, sw);
		}
		sw.WriteLine();
		for (var b : module.getBeans().values()) {
			BeanFormatter.Make(module.getName(), b.Name, b.TypeId, b.Variables, b.Enums, sw);
		}
		sw.WriteLine();
		for (var p : module.getProtocols().values()) {
			ProtocolFormatter.Make(module.getName(), p, sw);
		}
	}

	private static final String ChunkNameRegisterProtocol = "REGISTER PROTOCOL";

	private void GenChunkByName(OutputStreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk) {
		switch (chunk.getName()) {
			case ChunkNameRegisterProtocol:
				RegisterProtocol(writer);
				break;
			default:
				throw new RuntimeException("unknown Chunk.Name=" + chunk.getName());
		}
	}

	private void RegisterProtocol(OutputStreamWriter sw) {
		Module realmod = (Module)module;
		Service serv = realmod.getReferenceService();
		if (serv != null) {
			int serviceHandleFlags = realmod.getReferenceService().getHandleFlags();
			for (Protocol p : realmod.getProtocols().values()) {
				boolean tempVar = p instanceof Rpc;
				Rpc rpc = tempVar ? (Rpc)p : null;
				if (tempVar) {
					if ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags) != 0) {
						sw.write(String.format("    Zeze.ProtocolHandles[%1$s] = %2$sImpl.Process%3$sRequest", p.getTypeId(), module.getName(), p.getName()) + System.lineSeparator());
					}
					continue;
				}

				if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags)) {
					sw.write(String.format("    Zeze.ProtocolHandles[%1$s] = %2$sImpl.Process%3$s", p.getTypeId(), module.getName(), p.getName()) + System.lineSeparator());
				}
			}
		}
	}

	public final void MakeSrc() {
		if (null == module.getParent()) {
			return; // must be solution
		}

		Zeze.Util.FileChunkGen fcg = new Util.FileChunkGen("-- ZEZE_FILE_CHUNK {{{", "-- ZEZE_FILE_CHUNK }}}");
		String fullDir = module.getParent().GetFullPath(srcDir, null);
		String fullFileName = Paths.get(fullDir).resolve(module.getName() + "Impl.lua").toString();
		if (fcg.LoadFile(fullFileName)) {
			fcg.SaveFile(fullFileName, ::GenChunkByName);
		}
		else {
			(new File(fullDir)).mkdirs();
			try (OutputStreamWriter sw = new OutputStreamWriter(fullFileName, java.nio.charset.StandardCharsets.UTF_8)) {
    
				sw.write(String.valueOf(String.format("local %1$sImpl = {}", module.getName())) + System.lineSeparator());
				sw.WriteLine();
				sw.write("local Zeze = require 'Zeze'" + System.lineSeparator());
				sw.WriteLine();
				sw.write(String.valueOf(String.format("function %1$sImpl:Init()", module.getName())) + System.lineSeparator());
				sw.write("    " + fcg.getChunkStartTag() + " " + ChunkNameRegisterProtocol + System.lineSeparator());
				RegisterProtocol(sw);
				sw.write("    " + fcg.getChunkEndTag() + " " + ChunkNameRegisterProtocol + System.lineSeparator());
				sw.write(String.format("end") + System.lineSeparator());
				sw.WriteLine();
				Module realmod = (Module)module;
				Service serv = realmod.getReferenceService();
				if (serv != null) {
					int serviceHandleFlags = realmod.getReferenceService().getHandleFlags();
					for (Protocol p : realmod.getProtocols().values()) {
						boolean tempVar = p instanceof Rpc;
						Rpc rpc = tempVar ? (Rpc)p : null;
						if (tempVar) {
							if ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags) != 0) {
								sw.write(String.valueOf(String.format("function %1$sImpl.Process%2$sRequest(rpc)", module.getName(), p.getName())) + System.lineSeparator());
								sw.write(String.format("    -- write rpc request handle here") + System.lineSeparator());
								sw.write(String.format("end") + System.lineSeparator());
								sw.write(String.format("") + System.lineSeparator());
							}
							continue;
						}
						if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleScriptFlags)) {
							sw.write(String.valueOf(String.format("function %1$sImpl.Process%2$s(p)", module.getName(), p.getName())) + System.lineSeparator());
							sw.write(String.format("    -- write handle here") + System.lineSeparator());
							sw.write(String.format("end") + System.lineSeparator());
							sw.write(String.format("") + System.lineSeparator());
						}
					}
				}
				sw.WriteLine();
				sw.write(String.valueOf(String.format("return %1$sImpl", module.getName())) + System.lineSeparator());
			}
		}
	}
}
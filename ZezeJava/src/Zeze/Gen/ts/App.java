package Zeze.Gen.ts;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;
import java.nio.file.*;

public class App {
	private Project project;
	private String genDir;

	public App(Project project, String genDir) {
		this.project = project;
		this.genDir = genDir;
	}

	private static final String ChunkNamePropertyGen = "PROPERTY GEN";
	private static final String ChunkNamePropertyInitGen = "PROPERTY INIT GEN";
	private static final String ChunkNameImportGen = "IMPORT GEN";
	private static final String ChunkNameStartGen = "START MODULE GEN";
	private static final String ChunkNameStopGen = "STOP MODULE GEN";
	private void GenChunkByName(OutputStreamWriter writer, Zeze.Util.FileChunkGen.Chunk chunk) {
		switch (chunk.getName()) {
			case ChunkNamePropertyGen:
				PropertyGen(writer);
				break;
			case ChunkNameImportGen:
				ImportGen(writer);
				break;
			case ChunkNameStartGen:
				StartGen(writer);
				break;
			case ChunkNameStopGen:
				StopGen(writer);
				break;
			case ChunkNamePropertyInitGen:
				PropertyInitInConstructorGen(writer);
				break;
		}
	}

	private void PropertyGen(OutputStreamWriter sw) {
		for (Module m : project.getAllModules().values()) {
			sw.write("    public " + m.Path("_", null) + ": " + m.Path("_", null) + ";" + System.lineSeparator());
		}
		for (Service m : project.getServices().values()) {
			sw.write("    public " + m.getName() + ": Zeze.Service;" + System.lineSeparator());
		}
		sw.WriteLine();
	}

	private void PropertyInitInConstructorGen(OutputStreamWriter sw) {
		for (Service m : project.getServices().values()) {
			sw.write("        this." + m.getName() + " = new Zeze.Service(\"" + m.getName() + "\");" + System.lineSeparator());
		}
		for (Module m : project.getAllModules().values()) {
			sw.write("        this." + m.Path("_", null) + " = new " + m.Path("_", null) + "(this);" + System.lineSeparator());
		}
	}

	private void ImportGen(OutputStreamWriter sw) {
		sw.write("import { Zeze } from \"zeze\"" + System.lineSeparator());
		for (Module m : project.getAllModules().values()) {
			sw.write("import { " + m.Path("_", null) + " } from \"" + m.Path("/", String.format("Module%1$s", m.getName())) + "\"" + System.lineSeparator());
		}
	}

	private void StartGen(OutputStreamWriter sw) {
		for (var m : project.getModuleStartOrder()) {
			sw.write("        this." + m.Path("_", null) + ".Start(this);" + System.lineSeparator());
		}
		for (Module m : project.getAllModules().values()) {
			if (project.getModuleStartOrder().contains(m)) {
				continue;
			}
			sw.write("        this." + m.Path("_", null) + ".Start(this);" + System.lineSeparator());
		}
	}

	private void StopGen(OutputStreamWriter sw) {
		for (Module m : project.getAllModules().values()) {
			sw.write("        this." + m.Path("_", null) + ".Stop(this);" + System.lineSeparator());
		}
	}

	public final void Make() {
		Zeze.Util.FileChunkGen fcg = new Util.FileChunkGen();
		String fullDir = project.getSolution().GetFullPath(genDir, null);
		String fullFileName = Paths.get(fullDir).resolve("App.ts").toString();
		if (fcg.LoadFile(fullFileName)) {
			fcg.SaveFile(fullFileName, ::GenChunkByName);
			return;
		}
		// new file
		(new File(fullDir)).mkdirs();
		try (OutputStreamWriter sw = new OutputStreamWriter(fullFileName, java.nio.charset.StandardCharsets.UTF_8)) {
			sw.WriteLine();
			sw.write(fcg.getChunkStartTag() + " " + ChunkNameImportGen + System.lineSeparator());
			ImportGen(sw);
			sw.write(fcg.getChunkEndTag() + " " + ChunkNameImportGen + System.lineSeparator());
			sw.WriteLine();
			sw.write("export class " + project.getSolution().getName() + "_App {" + System.lineSeparator());
			sw.write("    " + fcg.getChunkStartTag() + " " + ChunkNamePropertyGen + System.lineSeparator());
			PropertyGen(sw);
			sw.write("    " + fcg.getChunkEndTag() + " " + ChunkNamePropertyGen + System.lineSeparator());
			sw.write("    public constructor() {" + System.lineSeparator());
			sw.write("        " + fcg.getChunkStartTag() + " " + ChunkNamePropertyInitGen + System.lineSeparator());
			PropertyInitInConstructorGen(sw);
			sw.write("        " + fcg.getChunkEndTag() + " " + ChunkNamePropertyInitGen + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.WriteLine();
			sw.write("    public Start(): void {" + System.lineSeparator());
			sw.write("        " + fcg.getChunkStartTag() + " " + ChunkNameStartGen + System.lineSeparator());
			StartGen(sw);
			sw.write("        " + fcg.getChunkEndTag() + " " + ChunkNameStartGen + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.WriteLine();
			sw.write("    public Stop(): void {" + System.lineSeparator());
			sw.write("        " + fcg.getChunkStartTag() + " " + ChunkNameStopGen + System.lineSeparator());
			StopGen(sw);
			sw.write("        " + fcg.getChunkEndTag() + " " + ChunkNameStopGen + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}
}
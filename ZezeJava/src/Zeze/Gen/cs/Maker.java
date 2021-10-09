package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;
import java.io.*;
import java.nio.file.*;

public class Maker {
	private Project Project;
	public final Project getProject() {
		return Project;
	}

	public Maker(Project project) {
		Project = project;
	}

	public final void Make() {
		String projectBasedir = getProject().getGendir();
		String projectDir = Paths.get(projectBasedir).resolve(getProject().getName()).toString();
		String genDir = Paths.get(projectDir).resolve("Gen").toString();
		String srcDir = projectDir;

		if ((new File(genDir)).isDirectory()) {
			Directory.Delete(genDir, true);
		}

		for (Types.Bean bean : getProject().getAllBeans().values()) {
			(new BeanFormatter(bean)).Make(genDir);
		}
		for (Types.BeanKey beanKey : getProject().getAllBeanKeys().values()) {
			(new BeanKeyFormatter(beanKey)).Make(genDir);
		}
		for (Protocol protocol : getProject().getAllProtocols().values()) {
			boolean tempVar = protocol instanceof Rpc;
			Rpc rpc = tempVar ? (Rpc)protocol : null;
			if (tempVar) {
				(new RpcFormatter(rpc)).Make(genDir);
			}
			else {
				(new ProtocolFormatter(protocol)).Make(genDir);
			}
		}
		for (Module mod : getProject().getAllModules().values()) {
			(new ModuleFormatter(getProject(), mod, genDir, srcDir)).Make();
		}
		for (Service ma : getProject().getServices().values()) {
			(new ServiceFormatter(ma, genDir, srcDir)).Make();
		}
		for (Table table : getProject().getAllTables().values()) {
			if (getProject().getGenTables().contains(table.getGen())) {
				(new TableFormatter(table, genDir)).Make();
			}
		}
		(new Schemas(getProject(), genDir)).Make();

		(new App(getProject(), genDir, srcDir)).Make();
	}

}
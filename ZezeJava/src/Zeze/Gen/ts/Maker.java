package Zeze.Gen.ts;

import Zeze.*;
import Zeze.Gen.*;
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
		String genDir = getProject().getScriptDir().length() > 0 ? Paths.get(projectDir).resolve(getProject().getScriptDir()).toString() : projectDir;

		try (OutputStreamWriter sw = Program.OpenWriterNoPath(genDir, "gen.ts", true)) {
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("import { Zeze } from \"zeze\"" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			for (Types.Bean bean : getProject().getAllBeans().values()) {
				(new BeanFormatter(bean)).Make(sw);
			}
			for (Types.BeanKey beanKey : getProject().getAllBeanKeys().values()) {
				(new BeanKeyFormatter(beanKey)).Make(sw);
			}
			for (Protocol protocol : getProject().getAllProtocols().values()) {
				boolean tempVar = protocol instanceof Rpc;
				Rpc rpc = tempVar ? (Rpc)protocol : null;
				if (tempVar) {
				   (new RpcFormatter(rpc)).Make(sw);
				}
				else {
					(new ProtocolFormatter(protocol)).Make(sw);
				}
			}
			for (Module mod : getProject().getAllModules().values()) {
				(new ModuleFormatter(getProject(), mod, genDir)).Make();
			}
			(new App(getProject(), genDir)).Make();
			/*
			foreach (Service ma in Project.Services.Values)
			{
			    new ServiceFormatter(ma, genDir, srcDir).Make();
			}
			*/
		}
	}

}
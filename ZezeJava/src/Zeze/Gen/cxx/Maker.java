package Zeze.Gen.cxx;

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

	private String BaseClass(Service s) {
		return s.getBase().length() > 0 ? s.getBase() : "Zeze::Net::ToLuaService";
	}
	public final void Make() {
		String projectBasedir = getProject().getGendir();
		String projectDir = Paths.get(projectBasedir).resolve(getProject().getName()).toString();
		String genDir = projectDir;

		{
			try (OutputStreamWriter sw = getProject().getSolution().OpenWriter(genDir, "App.h", true)) {
				sw.write("// auto-generated" + System.lineSeparator());
				sw.WriteLine();
				for (var m : getProject().getServices().values()) {
					sw.write(String.valueOf(String.format("#include \"%1$s.h\"", m.Name)) + System.lineSeparator());
				}
				sw.WriteLine();
				sw.write(String.valueOf(String.format("namespace %1$s", getProject().getSolution().getName())) + System.lineSeparator());
				sw.write(String.format("{{") + System.lineSeparator());
				sw.write(String.format("    class App") + System.lineSeparator());
				sw.write(String.format("    {{") + System.lineSeparator());
				sw.write(String.format("    public:") + System.lineSeparator());
				sw.write(String.format("        static App & Instance() {{ static App instance; return instance; }}") + System.lineSeparator());
				sw.WriteLine();
				for (var m : getProject().getServices().values()) {
					sw.write(String.valueOf(String.format("        %1$s %2$s;", getProject().getSolution().Path("::", m.Name), m.Name)) + System.lineSeparator());
				}
				sw.write(String.format("    }};") + System.lineSeparator());
				sw.write(String.format("}}") + System.lineSeparator());
			}
		}

		for (var m : getProject().getServices().values()) {
			try (OutputStreamWriter sw = getProject().getSolution().OpenWriter(genDir, String.format("%1$s.h", m.Name), false)) {
				if (null == sw) {
					continue;
				}
				//sw.WriteLine("// auto-generated");
				sw.WriteLine();
				sw.write(String.format("#include \"ToLuaService.h\"") + System.lineSeparator());
				sw.WriteLine();
				sw.write(String.valueOf(String.format("namespace %1$s", getProject().getSolution().getName())) + System.lineSeparator());
				sw.write(String.format("{{") + System.lineSeparator());
				sw.write(String.valueOf(String.format("    class %1$s : public %2$s", m.Name, BaseClass(m))) + System.lineSeparator());
				sw.write(String.format("    {{") + System.lineSeparator());
				sw.write(String.format("    public:") + System.lineSeparator());
				sw.write(String.valueOf(String.format("        %1$s() : %2$s(\"%3$s\") { }", m.Name, BaseClass(m), m.Name)) + System.lineSeparator());
				sw.write(String.format("    }};") + System.lineSeparator());
				sw.write(String.format("}}") + System.lineSeparator());
			}
		}
	}
}
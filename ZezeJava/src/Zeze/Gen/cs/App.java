package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;

public class App {
	private Project project;
	private String genDir;
	private String srcDir;

	public App(Project project, String genDir, String srcDir) {
		this.project = project;
		this.genDir = genDir;
		this.srcDir = srcDir;
	}

	public final void Make() {
		MakePartialGen();
		MakePartial();
	}

	public final void MakePartialGen() {
		try (OutputStreamWriter sw = project.getSolution().OpenWriter(genDir, "App.cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("using System.Collections.Generic;" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + project.getSolution().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public sealed partial class App" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public static App Instance { get; } = new App();" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public Zeze.Application Zeze { get; set; }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public Dictionary<string, Zeze.IModule> Modules { get; } = new Dictionary<string, Zeze.IModule>();" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
    
			for (Module m : project.getAllModules().values()) {
				var fullname = m.Path("_", null);
				sw.write(String.valueOf(String.format("        public %1$s %2$s { get; set; }", m.Path(".", String.format("Module%1$s", m.getName())), fullname)) + System.lineSeparator());
				sw.write("" + System.lineSeparator());
			}
    
			for (Service m : project.getServices().values()) {
				sw.write("        public " + m.getFullName() + " " + m.getName() + " { get; set; }" + System.lineSeparator());
				sw.write("" + System.lineSeparator());
			}
    
			sw.write("        public void Create(Zeze.Config config = null)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            lock(this)" + System.lineSeparator());
			sw.write("            {" + System.lineSeparator());
			sw.write("                if (null != Zeze)" + System.lineSeparator());
			sw.write("                    return;" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write(String.valueOf(String.format("                Zeze = new Zeze.Application(\"%1$s\", config);", project.getSolution().getName())) + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			for (Service m : project.getServices().values()) {
				sw.write("                " + m.getName() + " = new " + m.getFullName() + "(Zeze);" + System.lineSeparator());
			}
			sw.write("" + System.lineSeparator());
			for (Module m : project.getAllModules().values()) {
				var fullname = m.Path("_", null);
				sw.write("                " + fullname + " = new " + m.Path(".", String.format("Module%1$s", m.getName())) + "(this);" + System.lineSeparator());
				sw.write(String.valueOf(String.format("                %1$s = (%2$s)ReplaceModuleInstance(%3$s);", fullname, m.Path(".", String.format("Module%1$s", m.getName())), fullname)) + System.lineSeparator());
				sw.write(String.valueOf(String.format("                Modules.Add(%1$s.Name, %2$s);", fullname, fullname)) + System.lineSeparator());
			}
			sw.write("" + System.lineSeparator());
			sw.write("                Zeze.Schemas = new " + project.getSolution().Path(".", "Schemas") + "();" + System.lineSeparator());
			sw.write("            }" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void Destroy()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            lock(this)" + System.lineSeparator());
			sw.write("            {" + System.lineSeparator());
			for (Module m : project.getAllModules().values()) {
				var fullname = m.Path("_", null);
				sw.write("                " + fullname + " = null;" + System.lineSeparator());
			}
			sw.write("                Modules.Clear();" + System.lineSeparator());
			for (Service m : project.getServices().values()) {
				sw.write("                " + m.getName() + " = null;" + System.lineSeparator());
			}
			sw.write("                Zeze = null;" + System.lineSeparator());
			sw.write("            }" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void StartModules()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            lock(this)" + System.lineSeparator());
			sw.write("            {" + System.lineSeparator());
			for (var m : project.getModuleStartOrder()) {
				sw.write("                " + m.Path("_", null) + ".Start(this);" + System.lineSeparator());
			}
			for (Module m : project.getAllModules().values()) {
				if (project.getModuleStartOrder().contains(m)) {
					continue;
				}
				sw.write("                " + m.Path("_", null) + ".Start(this);" + System.lineSeparator());
			}
			sw.write("" + System.lineSeparator());
			sw.write("            }" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void StopModules()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            lock(this)" + System.lineSeparator());
			sw.write("            {" + System.lineSeparator());
			for (Module m : project.getAllModules().values()) {
				sw.write("                " + m.Path("_", null) + ".Stop(this);" + System.lineSeparator());
			}
			sw.write("            }" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void StartService()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            lock(this)" + System.lineSeparator());
			sw.write("            {" + System.lineSeparator());
			for (Service m : project.getServices().values()) {
				sw.write("                " + m.getName() + ".Start();" + System.lineSeparator());
			}
			sw.write("            }" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void StopService()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            lock(this)" + System.lineSeparator());
			sw.write("            {" + System.lineSeparator());
			for (Service m : project.getServices().values()) {
				sw.write("                " + m.getName() + ".Stop();" + System.lineSeparator());
			}
			sw.write("            }" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}

	public final void MakePartial() {
		try (OutputStreamWriter sw = project.getSolution().OpenWriter(srcDir, "App.cs", false)) {
			if (sw == null) {
				return;
			}
    
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + project.getSolution().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public sealed partial class App" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            return module;" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void Start()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            Create();" + System.lineSeparator());
			sw.write("            Zeze.Start(); // 启动数据库" + System.lineSeparator());
			sw.write("            StartModules(); // 启动模块，装载配置什么的。" + System.lineSeparator());
			sw.write("            StartService(); // 启动网络" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void Stop()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            StopService(); // 关闭网络" + System.lineSeparator());
			sw.write("            StopModules(); // 关闭模块,，卸载配置什么的。" + System.lineSeparator());
			sw.write("            Zeze.Stop(); // 关闭数据库" + System.lineSeparator());
			sw.write("            Destroy();" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}
}
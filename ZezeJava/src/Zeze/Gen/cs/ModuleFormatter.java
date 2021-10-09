package Zeze.Gen.cs;

import NLog.Layouts.*;
import Zeze.*;
import Zeze.Gen.*;

public class ModuleFormatter {
	private Project project;
	private Module module;
	private String genDir;
	private String srcDir;

	public ModuleFormatter(Project project, Module module, String genDir, String srcDir) {
		this.project = project;
		this.module = module;
		this.genDir = genDir;
		this.srcDir = srcDir;
	}

	public final void Make() {
		MakeInterface();
		MakePartialImplement();
		MakePartialImplementInGen();
	}

	public final void MakePartialImplementInGen() {
		try (OutputStreamWriter sw = module.OpenWriter(genDir, String.format("Module%1$sGen.cs", module.getName()), true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + module.Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write(String.valueOf(String.format("    public partial class Module%1$s : AbstractModule", module.getName())) + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write(String.format("        public const int ModuleId = %1$s;", module.getId()) + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			for (Table table : module.getTables().values()) {
				if (project.getGenTables().contains(table.getGen())) {
					sw.write("        private " + table.getName() + " _" + table.getName() + " = new " + table.getName() + "();" + System.lineSeparator());
				}
			}
			sw.write("" + System.lineSeparator());
			sw.write(String.valueOf(String.format("        public %1$s.App App { get; }", project.getSolution().getName())) + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write(String.valueOf(String.format("        public Module%1$s(%2$s.App app)", module.getName(), project.getSolution().getName())) + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("            App = app;" + System.lineSeparator());
			sw.write("            // register protocol factory and handles" + System.lineSeparator());
			Service serv = module.getReferenceService();
			if (serv != null) {
				int serviceHandleFlags = module.getReferenceService().getHandleFlags();
				for (Protocol p : module.getProtocols().values()) {
					boolean tempVar = p instanceof Rpc;
					Rpc rpc = tempVar ? (Rpc)p : null;
					if (tempVar) {
						// rpc 可能作为客户端发送也需要factory，所以总是注册factory。
						sw.write(String.valueOf(String.format("            App.%1$s.AddFactoryHandle(%2$s, new Zeze.Net.Service.ProtocolFactoryHandle()", serv.getName(), rpc.getTypeId())) + System.lineSeparator());
						sw.write("            {" + System.lineSeparator());
						sw.write(String.valueOf(String.format("                Factory = () => new %1$s(),", rpc.getSpace().Path(".", rpc.getName()))) + System.lineSeparator());
						if ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags) != 0) {
							sw.write(String.valueOf(String.format("                Handle = Zeze.Net.Service.MakeHandle<%1$s>(this, GetType().GetMethod(nameof(Process%2$sRequest))),", rpc.ShortNameIf(module), rpc.getName())) + System.lineSeparator());
						}
						if (p.getNoProcedure()) {
							sw.write(String.format("                NoProcedure = true,") + System.lineSeparator());
						}
						sw.write("            });" + System.lineSeparator());
						continue;
					}
					if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags)) {
						sw.write(String.valueOf(String.format("            App.%1$s.AddFactoryHandle(%2$s, new Zeze.Net.Service.ProtocolFactoryHandle()", serv.getName(), p.getTypeId())) + System.lineSeparator());
						sw.write("            {" + System.lineSeparator());
						sw.write(String.valueOf(String.format("                Factory = () => new %1$s(),", p.getSpace().Path(".", p.getName()))) + System.lineSeparator());
						sw.write(String.valueOf(String.format("                Handle = Zeze.Net.Service.MakeHandle<%1$s>(this, GetType().GetMethod(nameof(Process%2$s))),", p.ShortNameIf(module), p.getName())) + System.lineSeparator());
						if (p.getNoProcedure()) {
							sw.write(String.format("                NoProcedure = true,") + System.lineSeparator());
						}
						sw.write("            });" + System.lineSeparator());
					}
				}
			}
			sw.write("            // register table" + System.lineSeparator());
			for (Table table : module.getTables().values()) {
				if (project.getGenTables().contains(table.getGen())) {
					sw.write(String.valueOf(String.format("            App.Zeze.AddTable(App.Zeze.Config.GetTableConf(_%1$s.Name).DatabaseName, _%2$s);", table.getName(), table.getName())) + System.lineSeparator());
				}
			}
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public override void UnRegister()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			if (serv != null) {
				int serviceHandleFlags = module.getReferenceService().getHandleFlags();
				for (Protocol p : module.getProtocols().values()) {
					boolean tempVar2 = p instanceof Rpc;
					Rpc rpc = tempVar2 ? (Rpc)p : null;
					if (tempVar2) {
						// rpc 可能作为客户端发送也需要factory，所以总是注册factory。
						sw.write(String.valueOf(String.format("            App.%1$s.Factorys.TryRemove(%2$s, out var _);", serv.getName(), rpc.getTypeId())) + System.lineSeparator());
						continue;
					}
					if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags)) {
						sw.write(String.valueOf(String.format("            App.%1$s.Factorys.TryRemove(%2$s, out var _);", serv.getName(), p.getTypeId())) + System.lineSeparator());
					}
				}
			}
			for (Table table : module.getTables().values()) {
				if (project.getGenTables().contains(table.getGen())) {
					sw.write(String.valueOf(String.format("            App.Zeze.RemoveTable(App.Zeze.Config.GetTableConf(_%1$s.Name).DatabaseName, _%2$s);", table.getName(), table.getName())) + System.lineSeparator());
				}
			}
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}

	public final void MakePartialImplement() {
		try (OutputStreamWriter sw = module.OpenWriter(srcDir, String.format("Module%1$s.cs", module.getName()), false)) {
    
			if (null == sw) {
				return;
			}
    
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + module.Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write(String.valueOf(String.format("    public partial class Module%1$s : AbstractModule", module.getName())) + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public void Start(" + project.getSolution().getName() + ".App app)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("        public void Stop(" + project.getSolution().getName() + ".App app)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			if (module.getReferenceService() != null) {
				int serviceHandleFlags = module.getReferenceService().getHandleFlags();
				for (Protocol p : module.getProtocols().values()) {
					boolean tempVar = p instanceof Rpc;
					Rpc rpc = tempVar ? (Rpc)p : null;
					if (tempVar) {
						if ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags) != 0) {
							sw.write("        public override int Process" + rpc.getName() + "Request(" + rpc.ShortNameIf(module) + " rpc)" + System.lineSeparator());
							sw.write("        {" + System.lineSeparator());
							sw.write("            return Zeze.Transaction.Procedure.NotImplement;" + System.lineSeparator());
							sw.write("        }" + System.lineSeparator());
							sw.write("" + System.lineSeparator());
						}
						continue;
					}
					if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags)) {
						sw.write("        public override int Process" + p.getName() + "(" + p.ShortNameIf(module) + " protocol)" + System.lineSeparator());
						sw.write("        {" + System.lineSeparator());
						sw.write("            return Zeze.Transaction.Procedure.NotImplement;" + System.lineSeparator());
						sw.write("        }" + System.lineSeparator());
						sw.write("" + System.lineSeparator());
					}
				}
			}
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}

	public final void MakeInterface() {
		try (OutputStreamWriter sw = module.OpenWriter(genDir, "AbstractModule.cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + module.Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public abstract class AbstractModule : Zeze.IModule" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write(String.valueOf(String.format("        public override string FullName => \"%1$s\";", module.Path(".", null))) + System.lineSeparator());
			sw.write(String.valueOf(String.format("        public override string Name => \"%1$s\";", module.getName())) + System.lineSeparator());
			sw.write(String.format("        public override int Id => %1$s;", module.getId()) + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			// declare enums
			for (Types.Enum e : module.getEnums()) {
				sw.write("        public const int " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
			}
			if (!module.getEnums().isEmpty()) {
				sw.write("" + System.lineSeparator());
			}
    
			if (module.getReferenceService() != null) {
				int serviceHandleFlags = module.getReferenceService().getHandleFlags();
				for (Protocol p : module.getProtocols().values()) {
					boolean tempVar = p instanceof Rpc;
					Rpc rpc = tempVar ? (Rpc)p : null;
					if (tempVar) {
						if ((rpc.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags) != 0) {
							sw.write("        public abstract int Process" + rpc.getName() + "Request(" + rpc.ShortNameIf(module) + " rpc);" + System.lineSeparator());
							sw.write("" + System.lineSeparator());
						}
						continue;
					}
					if (0 != (p.getHandleFlags() & serviceHandleFlags & Program.HandleCSharpFlags)) {
						sw.write("        public abstract int Process" + p.getName() + "(" + p.ShortNameIf(module) + " protocol);" + System.lineSeparator());
						sw.write("" + System.lineSeparator());
					}
				}
			}
    
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}
}
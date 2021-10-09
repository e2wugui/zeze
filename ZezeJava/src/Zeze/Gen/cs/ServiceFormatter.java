package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;

public class ServiceFormatter {
	private Service service;
	private String genDir;
	private String srcDir;

	public ServiceFormatter(Service service, String genDir, String srcDir) {
		this.service = service;
		this.genDir = genDir;
		this.srcDir = srcDir;
	}

	public final void Make() {
		MakePartialInGen();
		MakePartialInSrc();
	}

	public final String BaseClass() {
		return service.getBase().length() > 0 ? service.getBase() : "Zeze.Net.Service";
	}

	public final void MakePartialInGen() {
		try (OutputStreamWriter sw = service.getProject().getSolution().OpenWriter(genDir, service.getName() + ".cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Serialize;");
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + service.getProject().getSolution().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public sealed partial class " + service.getName() + " : " + BaseClass() + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public " + service.getName() + "(Zeze.Application zeze) : base(\"" + service.getName() + "\", zeze)" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			/*
			if (service.IsProvider)
			{
			    sw.WriteLine("        // 用来同步等待Provider的静态绑定完成。");
			    sw.WriteLine("        public System.Threading.ManualResetEvent ProviderStaticBindCompleted = new System.Threading.ManualResetEvent(false);");
			    sw.WriteLine("");
			    sw.WriteLine("        public void ProviderStaticBind(Zeze.Net.AsyncSocket socket)");
			    sw.WriteLine("        {");
			    sw.WriteLine("            var rpc = new Zezex.Provider.Bind();");
			    foreach (var module in service.Modules)
			    {
			        var fullName = module.Path();
			        if (service.DynamicModules.Contains(fullName))
			            continue;
			        sw.WriteLine($"            rpc.Argument.ModuleIds.Add({module.Id}); // {fullName}");
			    }
			    sw.WriteLine("            rpc.Send(socket, (protocol) => { ProviderStaticBindCompleted.Set(); return 0; });");
			    sw.WriteLine("        }");
			    sw.WriteLine("");
			}
			*/
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}

	public final void MakePartialInSrc() {
		try (OutputStreamWriter sw = service.getProject().getSolution().OpenWriter(srcDir, service.getName() + ".cs", false)) {
			if (null == sw) {
				return;
			}
    
			sw.write("" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Serialize;");
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + service.getProject().getSolution().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public sealed partial class " + service.getName() + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        // 重载需要的方法。" + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}
}
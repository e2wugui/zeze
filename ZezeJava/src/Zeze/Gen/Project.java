package Zeze.Gen;

import Zeze.Gen.cs.*;
import Zeze.*;
import java.util.*;

public class Project {
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}
	private Solution Solution;
	public final Solution getSolution() {
		return Solution;
	}
	private void setSolution(Solution value) {
		Solution = value;
	}
	private String Platform;
	public final String getPlatform() {
		return Platform;
	}
	private void setPlatform(String value) {
		Platform = value;
	}
	private String Gendir;
	public final String getGendir() {
		return Gendir;
	}
	private void setGendir(String value) {
		Gendir = value;
	}
	private String ScriptDir;
	public final String getScriptDir() {
		return ScriptDir;
	}
	private void setScriptDir(String value) {
		ScriptDir = value;
	}
	private HashSet<String> GenTables = new HashSet<String> ();
	public final HashSet<String> getGenTables() {
		return GenTables;
	}
	private TreeMap<String, Service> Services = new TreeMap<String, Service> ();
	public final TreeMap<String, Service> getServices() {
		return Services;
	}
	private void setServices(TreeMap<String, Service> value) {
		Services = value;
	}

	// setup when compile
	private ArrayList<Module> Modules;
	public final ArrayList<Module> getModules() {
		return Modules;
	}
	private void setModules(ArrayList<Module> value) {
		Modules = value;
	}

	private XmlElement self;

	public final HashSet<Module> GetAllModules() {
		HashSet<Module> all = new HashSet<Module>();
		for (Module m : getModules()) {
			m.Depends(all);
		}
		for (Service service : getServices().values()) {
			for (Module m : service.getModules()) {
				m.Depends(all);
			}
		}
		return all;
	}

	public Project(Solution solution, XmlElement self) {
		setSolution(solution);

		setName(self.GetAttribute("name").strip());
		setPlatform(self.GetAttribute("platform").strip());
		setGendir(self.GetAttribute("gendir").strip());
		if (getGendir().length() == 0) {
			setGendir(".");
		}
		setScriptDir(self.GetAttribute("scriptdir").strip());

		for (String target : self.GetAttribute("GenTables").split("[,]", -1)) {
			getGenTables().add(target);
		}

		//Program.AddNamedObject(FullName, this);

		this.self = self; // 保存，在编译的时候使用。

		if (getSolution().getProjects().containsKey(getName())) {
			throw new RuntimeException("duplicate project name: " + getName());
		}
		getSolution().getProjects().put(getName(), this);

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "module":
					// ref 对象在编译的时候查找和设置。将保存在 Modules 中。
					break;
				case "service":
					new Service(this, e);
					break;
				case "ModuleStartOrder":
					var refs = Program.Refs(e, "start", "module");
					ArrayList<String> refFulNames = Program.ToFullNameIfNot(getSolution().getName(), refs);
					for (int i = 0; i < refFulNames.size(); ++i) {
						getModuleStartOrderNames().add(Program.FullModuleNameToFullClassName(refFulNames.get(i)));
					}
					break;
				case "bean":
				case "beankey":
					// Make 的时候解析。
					break;
				default:
					throw new RuntimeException("unkown element name: " + e.Name);
			}
		}
	}

	private ArrayList<String> ModuleStartOrderNames = new ArrayList<String> ();
	public final ArrayList<String> getModuleStartOrderNames() {
		return ModuleStartOrderNames;
	}
	private void setModuleStartOrderNames(ArrayList<String> value) {
		ModuleStartOrderNames = value;
	}
	private ArrayList<Module> ModuleStartOrder;
	public final ArrayList<Module> getModuleStartOrder() {
		return ModuleStartOrder;
	}
	private void setModuleStartOrder(ArrayList<Module> value) {
		ModuleStartOrder = value;
	}

	public final void Compile() {
		Collection<String> refs = Program.Refs(self, "module");
		ArrayList<String> refFulNames = Program.ToFullNameIfNot(getSolution().getName(), refs);
		for (int i = 0; i < refFulNames.size(); ++i) {
			refFulNames.set(i, Program.FullModuleNameToFullClassName(refFulNames.get(i)));
		}

		setModules(Program.CompileModuleRef(refFulNames));
		setModuleStartOrder(Program.CompileModuleRef(getModuleStartOrderNames()));

		for (Service service : getServices().values()) {
			service.Compile();
		}
	}

	/** setup in make
	*/
	private TreeMap<String, Module> AllModules = new TreeMap<String, Module> ();
	public final TreeMap<String, Module> getAllModules() {
		return AllModules;
	}
	private TreeMap<String, Protocol> AllProtocols = new TreeMap<String, Protocol> ();
	public final TreeMap<String, Protocol> getAllProtocols() {
		return AllProtocols;
	}
	private TreeMap<String, Table> AllTables = new TreeMap<String, Table> ();
	public final TreeMap<String, Table> getAllTables() {
		return AllTables;
	}
	private TreeMap<String, Zeze.Gen.Types.Bean> AllBeans = new TreeMap<String, Zeze.Gen.Types.Bean> ();
	public final TreeMap<String, Zeze.Gen.Types.Bean> getAllBeans() {
		return AllBeans;
	}
	private TreeMap<String, Zeze.Gen.Types.BeanKey> AllBeanKeys = new TreeMap<String, Zeze.Gen.Types.BeanKey> ();
	public final TreeMap<String, Zeze.Gen.Types.BeanKey> getAllBeanKeys() {
		return AllBeanKeys;
	}

	public final void Make() {
		for (var m : GetAllModules()) {
			getAllModules().put(m.FullName, m);
		}

		var _AllProtocols = new HashSet<Protocol>();
		for (Module mod : getAllModules().values()) { // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
			mod.Depends(_AllProtocols);
		}
		for (var p : _AllProtocols) {
			getAllProtocols().put(p.FullName, p);
		}

		var _AllTables = new HashSet<Table>();
		for (Module mod : getAllModules().values()) { // 这里本不该用 AllModules。只要第一层的即可，里面会递归。
			mod.Depends(_AllTables);
		}
		for (var t : _AllTables) {
			getAllTables().put(t.FullName, t);
		}

		var _AllBeans = new HashSet<Zeze.Gen.Types.Bean>();
		var _AllBeanKeys = new HashSet<Zeze.Gen.Types.BeanKey>(); {
			HashSet<Zeze.Gen.Types.Type> depends = new HashSet<Zeze.Gen.Types.Type>();
			for (Protocol protocol : getAllProtocols().values()) {
				protocol.Depends(depends);
			}
			for (Table table : getAllTables().values()) {
				table.Depends(depends);
			}
			// 加入模块中定义的所有bean和beankey。
			for (Module mod : getAllModules().values()) {
				for (var b : mod.getBeanKeys().values()) {
					depends.add(b);
				}
				for (var b : mod.getBeans().values()) {
					depends.add(b);
				}
			}
			// 加入额外引用的bean,beankey，一般引入定义在不是本项目模块中的。
			for (String n : Program.Refs(self, "bean")) {
				depends.add(Program.<Zeze.Gen.Types.Bean>GetNamedObject(n));
			}
			for (String n : Program.Refs(self, "beankey")) {
				depends.add(Program.<Zeze.Gen.Types.BeanKey>GetNamedObject(n));
			}
			for (Zeze.Gen.Types.Type type : depends) {
				if (type.isBean()) {
					if (type.isKeyable()) {
						_AllBeanKeys.add(type instanceof Zeze.Gen.Types.BeanKey ? (Zeze.Gen.Types.BeanKey)type : null);
					}
					else {
						_AllBeans.add(type instanceof Zeze.Gen.Types.Bean ? (Zeze.Gen.Types.Bean)type : null);
					}
				}
			}
		}
		for (var b : _AllBeans) {
			getAllBeans().put(b.FullName, b);
		}
		for (var b : _AllBeanKeys) {
			getAllBeanKeys().put(b.FullName, b);
		}

		if (getPlatform().length() == 0) {
			setPlatform("cs");
		}

		// 设置Module被哪个Service引用。必须在Make前设置。换 Project 会覆盖调引用。
		for (Service service : getServices().values()) {
			service.SetModuleReference();
		}

		switch (getPlatform()) {
			case "cs":
				(new Zeze.Gen.cs.Maker(this)).Make();
				break;
			case "lua":
				(new Zeze.Gen.lua.Maker(this)).Make();
				break;
			case "cs+lua":
				(new Zeze.Gen.cs.Maker(this)).Make();
				(new Zeze.Gen.lua.Maker(this)).Make();
				break;
			case "cxx+lua":
				(new Zeze.Gen.cxx.Maker(this)).Make();
				(new Zeze.Gen.lua.Maker(this)).Make();
				break;
			case "ts":
			case "cxx+ts":
				(new Zeze.Gen.ts.Maker(this)).Make();
				break;
			case "cs+ts":
				(new Zeze.Gen.cs.Maker(this)).Make();
				(new Zeze.Gen.ts.Maker(this)).Make();
				break;
			default:
				throw new RuntimeException("unsupport platform: " + getPlatform());
		}
	}
}
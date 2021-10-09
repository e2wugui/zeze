package Zeze.Gen;

import Zeze.Gen.Types.*;
import Zeze.*;
import java.util.*;

public class Module extends ModuleSpace {
	public final Service getReferenceService() {
		return _ReferenceService;
	}
	private Service _ReferenceService;

	public final void SetReferenceService(Service service) {
		_ReferenceService = service;
		for (Module m : getModules().values()) {
			m.SetReferenceService(service);
		}
	}

	public final String getFullName() {
		return Path();
	}

	public Module(ModuleSpace space, XmlElement self) {
		super(space, self, true);
		if (space.getModules().containsKey(getName())) {
			throw new RuntimeException("duplicate module name：" + getName());
		}
		space.getModules().put(getName(), this);
		Program.AddNamedObject(Path(".", String.format("Module%1$s", getName())), this);
		Program.AddNamedObject(Path(".", "AbstractModule"), this);

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "enum":
					Add(new Enum(e));
					break;
				case "bean":
					new Bean(this, e);
					break;
				case "module":
					new Module(this, e);
					break;
				case "protocol":
					new Protocol(this, e);
					break;
				case "rpc":
					new Rpc(this, e);
					break;
				case "table":
					new Table(this, e);
					break;
				case "beankey":
					new BeanKey(this, e);
					break;
				case "protocolref":
					// delay parse
					// 引进其他模块定义的协议。由于引入的协议Id对一个进程不能重复。
					// 所以再次没法引入本Project.Service中已经包含的协议。
					// 这个功能用来引入在其他Project.Module中定义的协议。
					// 【注意】引入的协议保留原来的moduleid，逻辑如果需要判断moduleid的话自己特殊处理。
					break;
				default:
					throw new RuntimeException("unknown nodename=" + e.Name + " in module=" + Path());
			}
		}
	}

	public final void Depends(HashSet<Module> modules) {
		if (false == modules.add(this)) {
			throw new RuntimeException("Module ref duplicate: " + Path());
		}

		for (Module module : this.getModules().values()) {
			module.Depends(modules);
		}
	}

	public final void Depends(HashSet<Protocol> depends) {
		for (Protocol p : getProtocols().values()) {
			depends.add(p);
		}

		for (Module module : getModules().values()) {
			module.Depends(depends);
		}
	}

	public final void Depends(HashSet<Table> depends) {
		for (Table table : getTables().values()) {
			depends.add(table);
		}

		for (Module module : getModules().values()) {
			module.Depends(depends);
		}
	}
}
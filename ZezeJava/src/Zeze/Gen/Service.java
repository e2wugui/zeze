package Zeze.Gen;

import Zeze.*;
import java.util.*;

public class Service { // Zeze.Net.Service
	private Project Project;
	public final Project getProject() {
		return Project;
	}
	private void setProject(Project value) {
		Project = value;
	}
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}
	private String Handle;
	public final String getHandle() {
		return Handle;
	}
	private void setHandle(String value) {
		Handle = value;
	}
	private int HandleFlags;
	public final int getHandleFlags() {
		return HandleFlags;
	}
	private String Base;
	public final String getBase() {
		return Base;
	}
	private void setBase(String value) {
		Base = value;
	}

	private XmlElement self;

	public final String getFullName() {
		return getProject().getSolution().Path(".", getName());
	}

	// setup when compile
	private ArrayList<Module> Modules;
	public final ArrayList<Module> getModules() {
		return Modules;
	}
	private void setModules(ArrayList<Module> value) {
		Modules = value;
	}
	//public HashSet<string> DynamicModules { get; } = new HashSet<string>();
	//public bool IsProvider { get; private set; } = false;

	private HashSet<Protocol> AllProtocols;
	public final HashSet<Protocol> GetAllProtocols() {
		if (AllProtocols != null) {
			return AllProtocols;
		}
		AllProtocols = new HashSet<Protocol>();
		for (Module module : getModules()) {
			module.Depends(AllProtocols);
		}
		return AllProtocols;
	}

	//public HashSet<Module> AllModules { get; private set; } = new HashSet<Module>();

	public Service(Project project, XmlElement self) {
		this.self = self;
		setProject(project);
		setName(self.GetAttribute("name").strip());
		setHandle(self.GetAttribute("handle"));
		HandleFlags = Program.ToHandleFlags(getHandle());
		if (getHandleFlags() == 0) {
			throw new RuntimeException("handle miss. " + getName() + " in project " + project.getName());
		}
		setBase(self.GetAttribute("base"));
		//IsProvider = self.GetAttribute("provider").Equals("true");

		//Program.AddNamedObject(FullName, this);

		if (project.getServices().containsKey(getName())) {
			throw new RuntimeException("duplicate service " + getName() + " in project " + project.getName());
		}
		project.getServices().put(getName(), this);
		/*
		XmlNodeList childNodes = self.ChildNodes;
		foreach (XmlNode node in childNodes)
		{
		    if (XmlNodeType.Element != node.NodeType)
		        continue;

		    XmlElement e = (XmlElement)node;
		    switch (e.Name)
		    {
		        case "module":
		            // ref 对象在编译的时候查找和设置。将保存在 Modules 中。
		            if (e.GetAttribute("dynamic").Equals("true"))
		                DynamicModules.Add(e.GetAttribute("ref"));
		            break;
		    }
		}
		var fullNameRefs = Program.ToFullNameIfNot(Project.Solution.Name, DynamicModules);
		DynamicModules.Clear();
		foreach (var fullName in fullNameRefs)
		    DynamicModules.Add(fullName);
		*/
	}

	public final void Compile() {
		Collection<String> refs = Program.Refs(self, "module");
		ArrayList<String> refFulNames = Program.ToFullNameIfNot(getProject().getSolution().getName(), refs);
		for (int i = 0; i < refFulNames.size(); ++i) {
			refFulNames.set(i, Program.FullModuleNameToFullClassName(refFulNames.get(i)));
		}
		setModules(Program.CompileModuleRef(refFulNames));
	}

	public final void SetModuleReference() {
		for (Module m : getModules()) {
			m.SetReferenceService(this);
		}
	}
}
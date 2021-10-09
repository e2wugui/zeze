package Zeze.Gen;

import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class ModuleSpace {
	private String Name;
	public final String getName() {
		return Name;
	}
	private void setName(String value) {
		Name = value;
	}
	public final String getNamePinyin() {
		return Program.ToPinyin(getName());
	}
	private ModuleSpace Parent;
	public final ModuleSpace getParent() {
		return Parent;
	}
	private void setParent(ModuleSpace value) {
		Parent = value;
	}
	private Zeze.Util.Ranges ProtocolIdRanges = new Zeze.Util.Ranges();
	public final Zeze.Util.Ranges getProtocolIdRanges() {
		return ProtocolIdRanges;
	}
	private short Id;
	public final short getId() {
		return Id;
	}

	private XmlElement Self;
	public final XmlElement getSelf() {
		return Self;
	}

	public final ModuleSpace GetRootModuleSpace() {
		ModuleSpace last = this;
		for (ModuleSpace p = getParent(); null != p; p = p.getParent()) {
			last = p;
		}
		return last;
	}

	public final Solution getSolution() {
		return (Solution)GetRootModuleSpace();
	}

	public final int PathDepth() {
		int depth = 0;
		for (ModuleSpace p = getParent(); null != p; p = p.getParent()) {
			++depth;
		}
		return depth;
	}


	public final String Path(String sep) {
		return Path(sep, null);
	}

	public final String Path() {
		return Path(".", null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public string Path(string sep = ".", string ObjectName = null)
	public final String Path(String sep, String ObjectName) {
		String path = getName();
		for (ModuleSpace p = getParent(); null != p; p = p.getParent()) {
			path = p.getName() + sep + path;
		}
		if (ObjectName.equals(null)) {
			return path;
		}

		return path + sep + ObjectName;
	}


	public final String PathPinyin(String sep) {
		return PathPinyin(sep, null);
	}

	public final String PathPinyin() {
		return PathPinyin(".", null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public string PathPinyin(string sep = ".", string ObjectName = null)
	public final String PathPinyin(String sep, String ObjectName) {
		String path = getNamePinyin();
		for (ModuleSpace p = getParent(); null != p; p = p.getParent()) {
			path = p.getNamePinyin() + sep + path;
		}
		if (ObjectName.equals(null)) {
			return path;
		}

		return path + sep + ObjectName;
	}


	public final String GetFullPath(String baseDir) {
		return GetFullPath(baseDir, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public string GetFullPath(string baseDir, string fileName = null)
	public final String GetFullPath(String baseDir, String fileName) {
		String fullName = Path(String.valueOf(File.separatorChar));
		String fullDir = Paths.get(baseDir).resolve(fullName).toString();
		if (fileName != null) {
			fullDir = Paths.get(fullDir).resolve(fileName).toString();
		}
		return fullDir;
	}



	public final System.IO.StreamWriter OpenWriter(String baseDir, String fileName) {
		return OpenWriter(baseDir, fileName, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public System.IO.StreamWriter OpenWriter(string baseDir, string fileName, bool overwrite = true)
	public final System.IO.StreamWriter OpenWriter(String baseDir, String fileName, boolean overwrite) {
		String fullDir = GetFullPath(baseDir);
		//Program.Print("CreateDirectory:" + fullDir);
		(new File(fullDir)).mkdirs();
		String fullFileName = Paths.get(fullDir).resolve(fileName).toString();
		boolean exists = (new File(fullFileName)).isFile();
		if (!exists || overwrite) {
			Program.Print("file " + (exists ? "overwrite" : "new") + " '" + fullFileName + "'");
//C# TO JAVA CONVERTER WARNING: The java.io.OutputStreamWriter constructor does not accept all the arguments passed to the System.IO.StreamWriter constructor:
//ORIGINAL LINE: System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
			OutputStreamWriter sw = new OutputStreamWriter(fullFileName, java.nio.charset.StandardCharsets.UTF_8);
			return sw;
		}
		Program.Print("file skip '" + fullFileName + "'");
		return null;
	}

	private HashMap<String, Module> Modules = new HashMap<String, Module> ();
	public final HashMap<String, Module> getModules() {
		return Modules;
	}
	private void setModules(HashMap<String, Module> value) {
		Modules = value;
	}
	private TreeMap<String, Zeze.Gen.Types.Bean> Beans = new TreeMap<String, Zeze.Gen.Types.Bean> ();
	public final TreeMap<String, Zeze.Gen.Types.Bean> getBeans() {
		return Beans;
	}
	private void setBeans(TreeMap<String, Zeze.Gen.Types.Bean> value) {
		Beans = value;
	}
	private TreeMap<String, Zeze.Gen.Types.BeanKey> BeanKeys = new TreeMap<String, Zeze.Gen.Types.BeanKey> ();
	public final TreeMap<String, Zeze.Gen.Types.BeanKey> getBeanKeys() {
		return BeanKeys;
	}
	private void setBeanKeys(TreeMap<String, Zeze.Gen.Types.BeanKey> value) {
		BeanKeys = value;
	}
	private TreeMap<String, Protocol> Protocols = new TreeMap<String, Protocol> ();
	public final TreeMap<String, Protocol> getProtocols() {
		return Protocols;
	}
	private void setProtocols(TreeMap<String, Protocol> value) {
		Protocols = value;
	}
	private TreeMap<String, Table> Tables = new TreeMap<String, Table> ();
	public final TreeMap<String, Table> getTables() {
		return Tables;
	}
	private void setTables(TreeMap<String, Table> value) {
		Tables = value;
	}

	private ArrayList<Zeze.Gen.Types.Enum> Enums = new ArrayList<Zeze.Gen.Types.Enum> ();
	public final ArrayList<Zeze.Gen.Types.Enum> getEnums() {
		return Enums;
	}
	private void setEnums(ArrayList<Zeze.Gen.Types.Enum> value) {
		Enums = value;
	}

	public final void Add(Zeze.Gen.Types.Enum e) {
		getEnums().add(e); // check duplicate
	}

	public final void Add(Zeze.Gen.Types.Bean bean) {
		Program.AddNamedObject(Path(".", bean.getName()), bean);
		getBeans().put(bean.getName(), bean);
	}
	public final void Add(Zeze.Gen.Types.BeanKey bean) {
		Program.AddNamedObject(Path(".", bean.getName()), bean);
		getBeanKeys().put(bean.getName(), bean);
	}

	public final void Add(Protocol protocol) {
		Program.AddNamedObject(Path(".", protocol.getName()), protocol);
		getProtocols().put(protocol.getName(), protocol);
	}

	public final void Add(Table table) {
		Program.AddNamedObject(Path(".", table.getName()), table);
		getTables().put(table.getName(), table);
	}


	public ModuleSpace(ModuleSpace parent, XmlElement self) {
		this(parent, self, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ModuleSpace(ModuleSpace parent, XmlElement self, bool hasId = false)
	public ModuleSpace(ModuleSpace parent, XmlElement self, boolean hasId) {
		Self = self;

		setParent(parent);
		setName(self.GetAttribute("name").strip());
		Program.CheckReserveName(getName());

		if (hasId) {
			Id = Short.parseShort(self.GetAttribute("id"));
			if (getId() <= 0) {
				throw new RuntimeException("module id <= 0 is reserved. @" + this.Path(".", null));
			}

			getSolution().getModuleIdAllowRanges().AssertInclude(getId());
			getSolution().getModuleIdCurrentRanges().CheckAdd(getId());
		}
	}

	public void Compile() {
		for (Zeze.Gen.Types.Bean bean : getBeans().values()) {
			bean.Compile();
		}
		for (Zeze.Gen.Types.BeanKey beanKey : getBeanKeys().values()) {
			beanKey.Compile();
		}
		for (Protocol protocol : getProtocols().values()) {
			protocol.Compile();
		}
		for (Table table : getTables().values()) {
			table.Compile();
		}
		for (Module module : getModules().values()) {
			module.Compile();
		}
		for (var p : Program.CompileProtocolRef(Program.Refs(getSelf(), "protocolref"))) {
			getProtocolIdRanges().CheckAdd(p.getId());
			getProtocols().put(p.getName(), p);
		}
	}
}
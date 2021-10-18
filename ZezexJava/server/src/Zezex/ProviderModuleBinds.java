package Zezex;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;

public class ProviderModuleBinds {

	public static ProviderModuleBinds Load() {
		return Load(null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public static ProviderModuleBinds Load(string xmlfile = null)
	public static ProviderModuleBinds Load(String xmlfile) {
		if (xmlfile == null) {
			xmlfile = "provider.module.binds.xml";
		}

		if (Files.isRegularFile(Paths.get(xmlfile))) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlfile);
				return new ProviderModuleBinds(doc.getDocumentElement());
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return new ProviderModuleBinds();
	}

	private boolean IsDynamicModule(String name) {
		var m = Modules.get(name);
		if (null != m) {
			return m.Providers.isEmpty();
		}
		return false;
	}

	private int GetModuleChoiceType(String name) {
		var m = Modules.get(name);
		if (null != m) {
			return m.ChoiceType;
		}
		return Zezex.Provider.BModule.ChoiceTypeDefault;
	}

	public final void BuildStaticBinds(HashMap<String, Zeze.IModule> AllModules, int serverId, HashMap<Integer, Zezex.Provider.BModule> modules) {
		HashMap<String, Integer> binds = new HashMap<String, Integer>();

		// special binds
		for (var m : getModules().values()) {
			if (m.Providers.contains(serverId)) {
				binds.put(m.FullName, Zezex.Provider.BModule.ConfigTypeSpecial);
			}
		}

		// default binds
		if (false == getProviderNoDefaultModule().contains(serverId)) {
			for (var m : AllModules.values()) {
				if (IsDynamicModule(m.getFullName())) {
					continue; // 忽略动态注册的模块。
				}
				if (getModules().containsKey(m.getFullName())) {
					continue; // 忽略已经有特别配置的模块
				}
				binds.put(m.getFullName(), Zezex.Provider.BModule.ConfigTypeDefault);
			}
		}

		// output
		for (var bind : binds.entrySet()) {
			var m = AllModules.get(bind.getKey());
			if (null != m){
				Zezex.Provider.BModule tempVar = new Zezex.Provider.BModule();
				tempVar.setChoiceType(GetModuleChoiceType(bind.getKey()));
				tempVar.setConfigType(bind.getValue());
				modules.put(m.getId(), tempVar);
			}
		}
	}

	public static class Module {
		private String FullName;
		public final String getFullName() {
			return FullName;
		}
		private int ChoiceType;
		public final int getChoiceType() {
			return ChoiceType;
		}
		private HashSet<Integer> Providers = new HashSet<Integer> ();
		public final HashSet<Integer> getProviders() {
			return Providers;
		}

		private int GetChoiceType(Element self) {
			switch (self.getAttribute("ChoiceType")) {
				case "ChoiceTypeHashAccount":
					return Zezex.Provider.BModule.ChoiceTypeHashAccount;

				case "ChoiceTypeHashRoleId":
					return Zezex.Provider.BModule.ChoiceTypeHashRoleId;

				default:
					return Zezex.Provider.BModule.ChoiceTypeDefault;
			}
		}

		public Module(Element self) {
			FullName = self.getAttribute("name");
			ChoiceType = GetChoiceType(self);
			ProviderModuleBinds.SplitIntoSet(self.getAttribute("providers"), getProviders());
		}
	}

	private HashMap<String, Module> Modules = new HashMap<String, Module> ();
	public final HashMap<String, Module> getModules() {
		return Modules;
	}
	private HashSet<Integer> ProviderNoDefaultModule = new HashSet<Integer> ();
	public final HashSet<Integer> getProviderNoDefaultModule() {
		return ProviderNoDefaultModule;
	}

	private ProviderModuleBinds() {
	}

	private ProviderModuleBinds(Element self) {
		if (false == self.getNodeName().equals("ProviderModuleBinds")) {
			throw new RuntimeException("is it a ProviderModuleBinds config.");
		}

		NodeList childnodes = self.getChildNodes();
		for (int i = 0; i < childnodes.getLength(); ++i) {
			Node node = childnodes.item(i);
			if (Node.ELEMENT_NODE != node.getNodeType()) {
				continue;
			}

			Element e = (Element)node;
			switch (e.getNodeName()) {
				case "module":
					AddModule(new Module(e));
					break;

				case "ProviderNoDefaultModule":
					SplitIntoSet(e.getAttribute("providers"), getProviderNoDefaultModule());
					break;

				default:
					throw new RuntimeException("unknown node name: " + e.getNodeName());
			}
		}
	}

	private void AddModule(Module module) {
		getModules().put(module.getFullName(), module);
	}

	private static void SplitIntoSet(String providers, HashSet<Integer> set) {
		for (var provider : providers.split("[,]", -1)) {
			set.add(Integer.parseInt(provider));
		}
	}
}
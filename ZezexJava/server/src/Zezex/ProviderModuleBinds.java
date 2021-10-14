package Zezex;

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

		if ((new File(xmlfile)).isFile()) {
			XmlDocument doc = new XmlDocument();
			doc.Load(xmlfile);
			return new ProviderModuleBinds(doc.DocumentElement);
		}
		return new ProviderModuleBinds();
	}

	private boolean IsDynamicModule(String name) {
		if (getModules().containsKey(name) && (var m = getModules().get(name)) == var m) {
			return m.Providers.Count == 0;
		}
		return false;
	}

	private int GetModuleChoiceType(String name) {
		if (getModules().containsKey(name) && (var m = getModules().get(name)) == var m) {
			return m.ChoiceType;
		}
		return Zezex.Provider.BModule.ChoiceTypeDefault;
	}

	public final void BuildStaticBinds(HashMap<String, Zeze.IModule> AllModules, int AutoKeyLocalId, HashMap<Integer, Zezex.Provider.BModule> modules) {
		HashMap<String, Integer> binds = new HashMap<String, Integer>();

		// special binds
		for (var m : getModules().values()) {
			if (m.Providers.Contains(AutoKeyLocalId)) {
				binds.put(m.FullName, Zezex.Provider.BModule.ConfigTypeSpecial);
			}
		}

		// default binds
		if (false == getProviderNoDefaultModule().contains(AutoKeyLocalId)) {
			for (var m : AllModules.values()) {
				if (IsDynamicModule(m.FullName)) {
					continue; // 忽略动态注册的模块。
				}
				if (getModules().containsKey(m.FullName)) {
					continue; // 忽略已经有特别配置的模块
				}
				binds.put(m.FullName, Zezex.Provider.BModule.ConfigTypeDefault);
			}
		}

		// output
		for (var bind : binds.entrySet()) {
			TValue m;
			if (AllModules.containsKey(bind.getKey()) && (m = AllModules.get(bind.getKey())) == m) {
				Zezex.Provider.BModule tempVar = new Zezex.Provider.BModule();
				tempVar.setChoiceType(GetModuleChoiceType(bind.getKey()));
				tempVar.setConfigType(bind.getValue());
				modules.put(m.Id, tempVar);
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

		private int GetChoiceType(XmlElement self) {
			switch (self.GetAttribute("ChoiceType")) {
				case "ChoiceTypeHashAccount":
					return Zezex.Provider.BModule.ChoiceTypeHashAccount;

				case "ChoiceTypeHashRoleId":
					return Zezex.Provider.BModule.ChoiceTypeHashRoleId;

				default:
					return Zezex.Provider.BModule.ChoiceTypeDefault;
			}
		}

		public Module(XmlElement self) {
			FullName = self.GetAttribute("name");
			ChoiceType = GetChoiceType(self);
			ProviderModuleBinds.SplitIntoSet(self.GetAttribute("providers"), getProviders());
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

	private ProviderModuleBinds(XmlElement self) {
		if (false == self.Name.equals("ProviderModuleBinds")) {
			throw new RuntimeException("is it a ProviderModuleBinds config.");
		}

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "module":
					AddModule(new Module(e));
					break;

				case "ProviderNoDefaultModule":
					SplitIntoSet(e.GetAttribute("providers"), getProviderNoDefaultModule());
					break;

				default:
					throw new RuntimeException("unknown node name: " + e.Name);
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
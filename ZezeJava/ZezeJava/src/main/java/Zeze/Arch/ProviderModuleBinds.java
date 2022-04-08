package Zeze.Arch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Beans.Provider.BModule;
import Zeze.Services.ServiceManager.SubscribeInfo;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ProviderModuleBinds {
	public static ProviderModuleBinds Load() {
		return Load(null);
	}

	public static ProviderModuleBinds Load(String xmlFile) {
		if (xmlFile == null) {
			xmlFile = "provider.module.binds.xml";
		}

		if (Files.isRegularFile(Paths.get(xmlFile))) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlFile);
				return new ProviderModuleBinds(doc.getDocumentElement());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return new ProviderModuleBinds();
	}

	private boolean IsDynamicModule(String name) {
		var m = Modules.get(name);
		return m != null && m.Providers.isEmpty();
	}

	private int GetModuleChoiceType(String name) {
		var m = Modules.get(name);
		return m != null ? m.ChoiceType : BModule.ChoiceTypeDefault;
	}

	public final void BuildDynamicBinds(HashMap<String, Zeze.IModule> AllModules, int serverId, IntHashMap<BModule> out) {
		for (var m : AllModules.values()) {
			var cm = Modules.get(m.getFullName());
			if (cm == null)
				continue; // dynamic must exist in config
			if (cm.getConfigType() != BModule.ConfigTypeDynamic)
				continue;

			if (!cm.Providers.isEmpty() && !cm.Providers.contains(serverId))
				continue; // dynamic providers. isEmpty means enable in all server.

			var module = new BModule();
			module.setChoiceType(cm.getChoiceType());
			module.setConfigType(BModule.ConfigTypeDynamic);
			module.setSubscribeType(cm.getSubscribeType());
			out.put(m.getId(), module);
		}
	}

	public final void BuildStaticBinds(HashMap<String, Zeze.IModule> AllModules, int serverId, IntHashMap<BModule> modules) {
		HashMap<String, Integer> binds = new HashMap<>();

		// special binds
		for (var m : getModules().values()) {
			if (m.getConfigType() == BModule.ConfigTypeSpecial && m.Providers.contains(serverId)) {
				binds.put(m.FullName, BModule.ConfigTypeSpecial);
			}
		}

		// default binds
		if (!getProviderNoDefaultModule().contains(serverId)) {
			for (var m : AllModules.values()) {
				if (IsDynamicModule(m.getFullName())) {
					continue; // 忽略动态注册的模块。
				}
				if (getModules().containsKey(m.getFullName())) {
					continue; // 忽略已经有特别配置的模块
				}
				binds.put(m.getFullName(), BModule.ConfigTypeDefault);
			}
		}

		// output
		for (var bind : binds.entrySet()) {
			var m = AllModules.get(bind.getKey());
			if (m != null) {
				var tempVar = new BModule();
				tempVar.setChoiceType(GetModuleChoiceType(bind.getKey()));
				tempVar.setConfigType(bind.getValue());
				tempVar.setSubscribeType(SubscribeInfo.SubscribeTypeReadyCommit);
				modules.put(m.getId(), tempVar);
			}
		}
	}

	public static class Module {
		private final String FullName;
		private final int ChoiceType;
		private final int SubscribeType;
		private final int ConfigType; // 为了兼容，没有配置的话，从其他条件推导出来。
		private final IntHashSet Providers = new IntHashSet();

		public final String getFullName() {
			return FullName;
		}

		public final int getChoiceType() {
			return ChoiceType;
		}

		public final int getSubscribeType() {
			return SubscribeType;
		}

		public final int getConfigType() {
			return ConfigType;
		}

		public final IntHashSet getProviders() {
			return Providers;
		}

		private int GetChoiceType(Element self) {
			switch (self.getAttribute("ChoiceType")) {
			case "ChoiceTypeHashAccount":
				return BModule.ChoiceTypeHashAccount;

			case "ChoiceTypeHashRoleId":
				return BModule.ChoiceTypeHashRoleId;

			default:
				return BModule.ChoiceTypeDefault;
			}
		}

		// 这个订阅类型目前用于动态绑定的模块，所以默认为SubscribeTypeSimple。
		private int GetSubscribeType(Element self) {
			//noinspection SwitchStatementWithTooFewBranches
			switch (self.getAttribute("SubscribeType")) {
			case "SubscribeTypeReadyCommit":
				return SubscribeInfo.SubscribeTypeReadyCommit;
			//case "SubscribeTypeSimple":
			//	return SubscribeInfo.SubscribeTypeSimple;
			default:
				return SubscribeInfo.SubscribeTypeSimple;
			}
		}

		public Module(Element self) {
			FullName = self.getAttribute("name");
			ChoiceType = GetChoiceType(self);
			SubscribeType = GetSubscribeType(self);

			ProviderModuleBinds.SplitIntoSet(self.getAttribute("providers"), getProviders());

			String attr = self.getAttribute("ConfigType").trim();
			switch (attr) {
			case "":
				// 兼容，如果没有配置
				ConfigType = Providers.isEmpty() ? BModule.ConfigTypeDynamic : BModule.ConfigTypeSpecial;
				break;

			case "Special":
				ConfigType = BModule.ConfigTypeSpecial;
				break;

			case "Dynamic":
				ConfigType = BModule.ConfigTypeDynamic;
				break;

			case "Default":
				ConfigType = BModule.ConfigTypeDefault;
				break;

			default:
				throw new RuntimeException("unknown ConfigType " + attr);
			}
		}
	}

	private final HashMap<String, Module> Modules = new HashMap<>();
	private final IntHashSet ProviderNoDefaultModule = new IntHashSet();

	private ProviderModuleBinds() {
	}

	public final HashMap<String, Module> getModules() {
		return Modules;
	}

	public final IntHashSet getProviderNoDefaultModule() {
		return ProviderNoDefaultModule;
	}

	private ProviderModuleBinds(Element self) {
		if (!self.getNodeName().equals("ProviderModuleBinds")) {
			throw new RuntimeException("is it a ProviderModuleBinds config.");
		}

		NodeList childNodes = self.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); ++i) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
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

	private static void SplitIntoSet(String providers, IntHashSet set) {
		for (var provider : providers.split(",", -1)) {
			var p = provider.trim();
			if (p.isEmpty())
				continue;
			set.add(Integer.parseInt(p));
		}
	}
}

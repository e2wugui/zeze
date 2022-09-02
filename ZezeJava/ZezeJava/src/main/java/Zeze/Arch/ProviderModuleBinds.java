package Zeze.Arch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Builtin.Provider.BModule;
import Zeze.IModule;
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

	// 动态模块必须在绑定配置里 声明ConfigType="Dynamic" 或 缺省的ConfigType并指定空的providers
	// 动态模块的providers为空则表示所有providers都可以注册该模块, 否则只有指定的providers可以注册
	public final void BuildDynamicBinds(HashMap<String, IModule> AllModules, int serverId, IntHashMap<BModule> out) {
		for (var m : AllModules.values()) {
			var cm = Modules.get(m.getFullName());
			if (cm != null && cm.ConfigType == BModule.ConfigTypeDynamic &&
					(cm.Providers.isEmpty() || cm.Providers.contains(serverId)))
				out.put(m.getId(), new BModule(cm.ChoiceType, BModule.ConfigTypeDynamic, cm.SubscribeType));
		}
	}

	// 非动态模块都为静态模块, 其中声明ConfigType="Special"及providers不为空的只有指定providers会注册该模块
	// 声明ConfigType="Default"且providers为空的所有providers都会注册
	// 其它未在绑定配置定义的模块只要不在ProviderNoDefaultModule配置里的providers都会注册
	public final void BuildStaticBinds(HashMap<String, IModule> AllModules, int serverId, IntHashMap<BModule> out) {
		var noDefaultModule = ProviderNoDefaultModule.contains(serverId);
		for (var m : AllModules.values()) {
			var cm = Modules.get(m.getFullName());
			if (cm == null) {
				if (noDefaultModule)
					continue;
			} else if (cm.ConfigType == BModule.ConfigTypeDynamic)
				continue;
			else if (cm.ConfigType == BModule.ConfigTypeSpecial) {
				if (!cm.Providers.contains(serverId))
					continue;
			} else if (!cm.Providers.isEmpty() && !cm.Providers.contains(serverId)) // ConfigTypeDefault
				continue;
			out.put(m.getId(), cm != null ? new BModule(cm.ChoiceType, cm.ConfigType, cm.SubscribeType)
					: new BModule(BModule.ChoiceTypeDefault, BModule.ConfigTypeDefault, SubscribeInfo.SubscribeTypeSimple));
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

		private static int GetChoiceType(Element self) {
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
		private static int GetSubscribeType(Element self) {
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

			ProviderModuleBinds.SplitIntoSet(self.getAttribute("providers"), Providers);

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
			throw new RuntimeException("is it a ProviderModuleBinds config?");
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
		getModules().put(module.FullName, module);
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

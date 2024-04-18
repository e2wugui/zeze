package Zeze.Arch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Builtin.Provider.BModule;
import Zeze.IModule;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ProviderModuleBinds {
	public static @NotNull ProviderModuleBinds load() {
		return load(null);
	}

	public static @NotNull ProviderModuleBinds load(@Nullable String xmlFile) {
		if (xmlFile == null)
			xmlFile = "provider.module.binds.xml";

		if (Files.isRegularFile(Paths.get(xmlFile))) {
			DocumentBuilderFactory db = DocumentBuilderFactory.newInstance();
			db.setXIncludeAware(true);
			db.setNamespaceAware(true);
			try {
				Document doc = db.newDocumentBuilder().parse(xmlFile);
				return new ProviderModuleBinds(doc.getDocumentElement());
			} catch (Exception ex) {
				Task.forceThrow(ex);
			}
		}
		return new ProviderModuleBinds();
	}

	public static class Module {
		private final @NotNull String fullName;
		private final int choiceType;
		private final int configType; // 为了兼容，没有配置的话，从其他条件推导出来。
		private final IntHashSet providers = new IntHashSet();

		public final @NotNull String getFullName() {
			return fullName;
		}

		public final int getChoiceType() {
			return choiceType;
		}

		public final int getConfigType() {
			return configType;
		}

		public final @NotNull IntHashSet getProviders() {
			return providers;
		}

		private static int getChoiceType(@NotNull Element self) {
			switch (self.getAttribute("ChoiceType")) {
			case "ChoiceTypeHashAccount":
				return BModule.ChoiceTypeHashAccount;

			case "ChoiceTypeHashRoleId":
				return BModule.ChoiceTypeHashRoleId;

			case "ChoiceTypeHashSourceAddress":
				return BModule.ChoiceTypeHashSourceAddress;

			case "ChoiceTypeFeedFullOneByOne":
				return BModule.ChoiceTypeFeedFullOneByOne;

			default:
				return BModule.ChoiceTypeDefault;
			}
		}

		// 这个订阅类型目前用于动态绑定的模块，所以默认为SubscribeTypeSimple。
		public Module(@NotNull Element self) {
			fullName = self.getAttribute("name");
			choiceType = getChoiceType(self);

			ProviderModuleBinds.splitIntoSet(self.getAttribute("providers"), providers);

			String attr = self.getAttribute("ConfigType").trim();
			switch (attr) {
			case "":
				// 兼容，如果没有配置
				configType = providers.isEmpty() ? BModule.ConfigTypeDynamic : BModule.ConfigTypeSpecial;
				break;

			case "Special":
				configType = BModule.ConfigTypeSpecial;
				break;

			case "Dynamic":
				configType = BModule.ConfigTypeDynamic;
				break;

			case "Default":
				configType = BModule.ConfigTypeDefault;
				break;

			default:
				throw new UnsupportedOperationException("unknown ConfigType " + attr);
			}
		}
	}

	private final HashMap<String, Module> modules = new HashMap<>();
	private final IntHashSet providerNoDefaultModule = new IntHashSet();

	private ProviderModuleBinds() {
	}

	private ProviderModuleBinds(@NotNull Element self) {
		if (!self.getNodeName().equals("ProviderModuleBinds"))
			throw new IllegalStateException("is it a ProviderModuleBinds config?");

		NodeList childNodes = self.getChildNodes();
		for (int i = 0, n = childNodes.getLength(); i < n; i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			Element e = (Element)node;
			switch (e.getNodeName()) {
			case "module":
				var module = new Module(e);
				modules.put(module.fullName, module);
				break;

			case "ProviderNoDefaultModule":
				splitIntoSet(e.getAttribute("providers"), providerNoDefaultModule);
				break;

			default:
				throw new UnsupportedOperationException("unknown element name: " + e.getNodeName());
			}
		}
	}

	private static void splitIntoSet(@NotNull String providers, @NotNull IntHashSet set) {
		for (var provider : providers.split(",", -1)) {
			var p = provider.trim();
			if (!p.isEmpty())
				set.add(Integer.parseInt(p));
		}
	}

	public @NotNull HashMap<String, Module> getModules() {
		return modules;
	}

	public @NotNull IntHashSet getProviderNoDefaultModule() {
		return providerNoDefaultModule;
	}

	// 非动态模块都为静态模块, 其中声明ConfigType="Special"及providers不为空的只有指定providers会注册该模块
	// 声明ConfigType="Default"且providers为空的所有providers都会注册
	// 其它未在绑定配置定义的模块只要不在ProviderNoDefaultModule配置里的providers都会注册
	public void buildStaticBinds(@NotNull Map<String, IModule> AllModules, int serverId,
								 @NotNull IntHashMap<BModule.Data> out) {
		var noDefaultModule = providerNoDefaultModule.contains(serverId);
		for (var m : AllModules.values()) {
			var cm = modules.get(m.getFullName());
			if (cm == null) {
				if (noDefaultModule)
					continue;
			} else if (cm.configType == BModule.ConfigTypeDynamic)
				continue;
			else if (cm.configType == BModule.ConfigTypeSpecial) {
				if (!cm.providers.contains(serverId))
					continue;
			} else if (!cm.providers.isEmpty() && !cm.providers.contains(serverId)) // ConfigTypeDefault
				continue;
			out.put(m.getId(), cm != null ? new BModule.Data(cm.choiceType, cm.configType)
					: new BModule.Data(
							BModule.ChoiceTypeDefault,
							BModule.ConfigTypeDefault));
		}
	}

	// 动态模块必须在绑定配置里 声明ConfigType="Dynamic" 或 缺省的ConfigType并指定空的providers
	// 动态模块的providers为空则表示所有providers都可以注册该模块, 否则只有指定的providers可以注册
	public void buildDynamicBinds(@NotNull Map<String, IModule> AllModules, int serverId,
								  @NotNull IntHashMap<BModule.Data> out) {
		for (var m : AllModules.values()) {
			var cm = modules.get(m.getFullName());
			if (cm != null && cm.configType == BModule.ConfigTypeDynamic &&
					(cm.providers.isEmpty() || cm.providers.contains(serverId)))
				out.put(m.getId(), new BModule.Data(cm.choiceType, BModule.ConfigTypeDynamic));
		}
	}
}

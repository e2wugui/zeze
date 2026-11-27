package Zeze.Services.RocketMQ;

import org.apache.rocketmq.client.ClientConfig;

/**
 * RocketMQ {@link ClientConfig} 路由与身份字段复制辅助，供 {@link Consumer} 与 {@link Producer} 构造期透传配置。
 */
final class ClientConfigs {
	private ClientConfigs() {
	}

	/**
	 * 将 src 的路由与身份字段复制到 dst；空字符串不覆盖以保留 dst 默认值与懒推导语义。
	 */
	static void copyRoutingIdentity(ClientConfig src, ClientConfig dst) {
		dst.setNamesrvAddr(src.getNamesrvAddr());
		// namespace 与 namespaceV2 在 ClientConfig 中相互独立，分别按非空透传
		var namespace = src.getNamespace();
		if (namespace != null && !namespace.isEmpty())
			dst.setNamespace(namespace);
		var namespaceV2 = src.getNamespaceV2();
		if (namespaceV2 != null && !namespaceV2.isEmpty())
			dst.setNamespaceV2(namespaceV2);
		if (src.getInstanceName() != null)
			dst.setInstanceName(src.getInstanceName());
		if (src.getUnitName() != null)
			dst.setUnitName(src.getUnitName());
		if (src.getClientIP() != null)
			dst.setClientIP(src.getClientIP());
		dst.setVipChannelEnabled(src.isVipChannelEnabled());
		dst.setUseTLS(src.isUseTLS());
		dst.setLanguage(src.getLanguage());
	}
}

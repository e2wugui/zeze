package Zeze.Services.RocketMQ;

import org.apache.rocketmq.client.ClientConfig;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ClientConfig} 的构造期透传：{@link Consumer}/{@link Producer} 构造器接收
 * ClientConfig 即把路由/身份字段透传给内部 consumer/producer——namespace 不透传=多租户
 * 形态下静默消息不可达，instanceName 不透传=同 JVM 同组多实例部署 start 时才报 clientId
 * 重复且报错远离根因。
 *
 * <p>DefaultMQPushConsumer/TransactionMQProducer 自身继承 ClientConfig，故按字段逐一复制。
 * 5.5.1 新增的 namespaceV2 不透传：运行时（4.9.x）无此 API，非跨版本稳定字段。
 */
final class ClientConfigs {
	private ClientConfigs() {
	}

	/**
	 * 把 src 的路由/身份字段复制到 dst（字符串字段 null/空不设置，保留 dst 同源默认值及
	 * 懒推导语义——如 namespace 由 endpoint 形态 namesrvAddr 懒推导；布尔/枚举字段两端
	 * 默认值相同，无条件复制为无害覆盖）。
	 */
	static void copyRoutingIdentity(@NotNull ClientConfig src, @NotNull ClientConfig dst) {
		dst.setNamesrvAddr(src.getNamesrvAddr());
		// namespace 与 namespaceV2 是 ClientConfig 的两个独立字段（5.x 里 V2 不是 V1 的
		// 等价新名——0d4824c07"去idea警告"把 V1 改读 V2 曾致：用户 setNamespace("NS1")
		// 被静默丢弃（读 V2 空）、TestClientConfigPassThrough 确定性红 ×39/40）。
		// 透传语义=用户配了哪个传哪个，两字段独立复制。
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

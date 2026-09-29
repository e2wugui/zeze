package Zeze.Services.RocketMQ;

import org.apache.rocketmq.client.ClientConfig;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ClientConfig} 的构造期透传（FND30 rocketmq-02）：{@link Consumer}/{@link Producer}
 * 构造器接收 ClientConfig 的参数形态承诺"传入即生效"——此前只透传 namesrvAddr，其余字段
 * 静默丢弃：namespace 丢弃=多租户形态下无报错的静默消息不可达（对端在 NS1%topic 空间收发，
 * 本端用裸 topic，两端 namesrv/group/topic 字符串看起来都"正确"）；instanceName 丢弃=同 JVM
 * 同组多实例部署在 start 时才报 clientId 重复且报错远离误配根因。
 *
 * <p>DefaultMQPushConsumer/TransactionMQProducer 自身继承 ClientConfig，故按字段逐一复制。
 * 5.5.1 新增的 namespaceV2 不透传：运行时（4.9.x）无此 API，非跨版本稳定字段。
 */
final class ClientConfigs {
	private ClientConfigs() {
	}

	/**
	 * 把 src 的路由/身份字段复制到 dst（null/默认值安全：字符串字段 null/空不设置，
	 * 保留 dst 自身与 ClientConfig 同源的默认值及懒推导语义——如 namespace 由 endpoint 形态
	 * namesrvAddr 懒推导；布尔/枚举字段两端的 ClientConfig 默认值相同，无条件复制为无害覆盖）。
	 */
	static void copyRoutingIdentity(@NotNull ClientConfig src, @NotNull ClientConfig dst) {
		dst.setNamesrvAddr(src.getNamesrvAddr());
		var namespace = src.getNamespace();
		if (namespace != null && !namespace.isEmpty())
			dst.setNamespace(namespace);
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

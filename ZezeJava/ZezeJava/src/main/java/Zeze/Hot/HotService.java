package Zeze.Hot;

/**
 * 可热更服务的生命周期接口：start/stop/upgrade 在热更流程中按序调用。
 */
public interface HotService {
	void start() throws Exception;

	default void startLast() throws Exception {
	}

	default void stopBefore() throws Exception {

	}

	/**
	 * 如果服务是有状态的，新版需要从旧版状态恢复，那么stop的时候需要保留状态，后面upgrade时读取。
	 * @throws Exception any
	 */
	void stop() throws Exception;

	/////////////////////////////////////////////////////////////////
	// 用于升级有状态服务
	void upgrade(HotService old) throws Exception;

	// 当缓存了别的模块创建的数据（一般是Bean），“别的”模块更新以后，理想情况需要刷新；
	// 由于缓存别的模块的数据的行为很复杂，暂不定义refresh接口。
}

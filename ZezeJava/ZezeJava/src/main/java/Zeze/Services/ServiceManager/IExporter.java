package Zeze.Services.ServiceManager;

/**
 * SM变更导出器接口：按eAll（全量，按服务）或eEdit（增量）两种模式导出订阅的服务列表。
 */
public interface IExporter {
	enum Type {
		eAll, // SM服务变化发生的时候回调exportAll，参数是当前所有的服务。
		eEdit, // SM服务变化发生时，回调exportEdit，参数是增量（包括第一次的批量）。
	}
	Type getType();

	default void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {

	}

	default void exportEdit(BEditService edit) {

	}

	/** 停机时释放导出器持有的底层资源（如线程池），默认空实现。 */
	default void close() {

	}
}

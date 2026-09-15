package Zeze.Services.ServiceManager;

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

	/**
	 * 停机释放：关闭实现持有的底层资源（线程池等）。Exporter.stop()停机时逐个调用；
	 * 默认空实现，无资源可释放的实现（如ExporterNginxConfig）不必覆盖。
	 */
	default void close() {

	}
}

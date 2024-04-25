package Zeze.Services.ServiceManager;

public interface IExporter {
	enum Type {
		eAll, // SM服务变化发生的时候回调exportAll，参数是当前所有的服务。
		eEdit, // SM服务变化发生时，回调exportEdit，参数是增量（包括第一次的批量）。
	}
	Type getType();

	default void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {

	}

	default void exportEdit(BEditService edit) throws Exception {

	}
}

package Zeze.Services.ServiceManager;

public class ExporterNgnixHttp implements IExporter {
	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) {

	}

	private final String url;

	/**
	 * 构造Ngnix配置HTTP输出器。
	 * 当SM信息发生变化，会把服务列表通过http接口，输出Ngnix中。
	 *
	 * @param param http-url
	 */
	public ExporterNgnixHttp(String param) {
		this.url = param;
	}
}

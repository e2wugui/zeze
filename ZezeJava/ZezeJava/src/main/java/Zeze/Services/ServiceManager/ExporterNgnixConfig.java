package Zeze.Services.ServiceManager;

public class ExporterNgnixConfig implements IExporter {
	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) {

	}

	private final String file;

	/**
	 * 构造Ngnix配置文件输出器。
	 * 当SM信息发生变化，会把服务列表输出到配置文件。
	 *
	 * @param param 配置文件路径名字
	 */
	public ExporterNgnixConfig(String param) {
		this.file = param;
	}
}

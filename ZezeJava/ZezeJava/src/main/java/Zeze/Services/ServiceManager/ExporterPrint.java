package Zeze.Services.ServiceManager;

import org.jetbrains.annotations.Nullable;

/** 调试用导出器：把每次SM增量变更直接打印到标准输出。 */
public class ExporterPrint implements IExporter {
	@Override
	public Type getType() {
		return Type.eEdit;
	}

	@Override
	public void exportEdit(BEditService edit) {
		System.out.println(edit);
	}

	public ExporterPrint(@Nullable ExporterConfig config) {
	}
}

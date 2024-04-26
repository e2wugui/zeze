package Zeze.Services.ServiceManager;

import org.jetbrains.annotations.Nullable;

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

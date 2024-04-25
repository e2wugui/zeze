package Zeze.Services.ServiceManager;

import org.jetbrains.annotations.NotNull;

public class ExporterPrint implements IExporter {
	@Override
	public Type getType() {
		return Type.eEdit;
	}

	@Override
	public void exportEdit(BEditService edit) {
		System.out.println(edit);
	}

	public ExporterPrint(@NotNull String param, @NotNull String param2) {

	}
}

package Zeze.Services.ServiceManager;

import java.util.Properties;
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

	public ExporterPrint(@NotNull ExporterConfig config) {

	}
}

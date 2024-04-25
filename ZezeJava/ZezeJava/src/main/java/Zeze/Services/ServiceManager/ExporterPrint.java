package Zeze.Services.ServiceManager;

public class ExporterPrint implements IExporter {
	@Override
	public Type getType() {
		return Type.eEdit;
	}

	@Override
	public void exportEdit(BEditService edit) {
		System.out.println(edit);
	}

	public ExporterPrint(String param) {

	}
}

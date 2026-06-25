package Zeze.Builtin.LinkdBase;

import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLinkdBase extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleLinkdBase.class);

	public void Start(zezeboot.App app) {
	}

	public void Stop(zezeboot.App app) {
	}

	@Override
	protected long ProcessReportError(Zeze.Builtin.LinkdBase.ReportError p) {
		logger.info("ReportError: {}", AsyncSocket.toStr(p.Argument));
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkdBase(zezeboot.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

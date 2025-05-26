package ClientZezex.Linkd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleLinkd extends AbstractModule {
    public static final Logger logger = LogManager.getLogger();

    public void Start(ClientGame.App app) throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    @Override
    protected long ProcessSc(ClientZezex.Linkd.Sc p) {
        logger.info("Sc " + p.Argument);
        return 0;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

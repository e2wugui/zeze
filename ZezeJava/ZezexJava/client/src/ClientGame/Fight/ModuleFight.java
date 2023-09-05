package ClientGame.Fight;

import Zeze.Arch.ProviderImplement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleFight extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(ModuleFight.class);
    public void Start(ClientGame.App app) throws Exception {
    }

    @Override
    public void StartLast() throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    @Override
    protected long ProcessAreYouFightRequest(ClientGame.Fight.AreYouFight r) {
        logger.info("ClientGame.ProcessAreYouFightRequest");
        r.SendResult();
        return 0;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFight(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

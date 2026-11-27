package Demo.Fight;

import Zeze.Game.LoginArgument;
import Zeze.Game.Online;
import Zeze.Game.OnlineSpec;
import Zeze.Transaction.*;
import Demo.*;
import Zeze.Util.EventDispatcher;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModuleFight extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleFight.class);
	public TaskCompletionSource<Boolean> areYouFight = new TaskCompletionSource<>();
	private EventDispatcher.Canceler loginEventCanceler;

	public boolean isAreYouFightDone() {
		return areYouFight.isDone();
	}

	public void setAreYouFightResult(boolean value) {
		areYouFight.setResult(value);
	}

	public void Start(App app) {
		loginEventCanceler = app.Provider.getOnline().getLoginEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
			var online = (Online)sender;
			var login = (LoginArgument)arg;
			OnlineSpec.ofRole(online, login.roleId).sendRpc(new AreYouFight(), (p) -> {
				logger.info("AreYouFight done.");
				setAreYouFightResult(true);
				return 0;
			});
			return 0;
		});
	}

	public void Stop(App app) {
		if (loginEventCanceler != null) {
			loginEventCanceler.cancel();
			loginEventCanceler = null;
		}
	}

	public Fighter GetFighter(BFighterId fighterId) {
		return new Fighter(fighterId, _tfighters.getOrAdd(fighterId));
	}

	public long CalculateFighter(BFighterId fighterId) {
		// fighter 计算属性现在不主动通知客户端，需要客户端需要的时候来读取。

		Fighter fighter = new Fighter(fighterId, new BFighter());
		_tfighters.getOrAdd(fighterId).assign(fighter.getBean());
		return Procedure.Success;
	}

	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		Zeze.Util.TaskSpec.ofProcedure(Demo.App.getInstance().Zeze.newProcedure(() -> CalculateFighter(fighterId),
				"CalculateFighter")).run();
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFight(Demo.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

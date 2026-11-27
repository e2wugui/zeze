package Game.Fight;

import java.util.Objects;
import Zeze.Game.LoginArgument;
import Zeze.Game.Online;
import Zeze.Game.OnlineSpec;
import Zeze.Transaction.*;
import Game.*;
import Zeze.Util.EventDispatcher;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

public final class ModuleFight extends AbstractModule {
	private static final Logger logger = LogManager.getLogger(ModuleFight.class);
	public final TaskCompletionSource<Boolean> areYouFight = new TaskCompletionSource<>();
	private EventDispatcher.@Nullable Canceler loginEventCanceler;

	public boolean isAreYouFightDone() {
		return areYouFight.isDone();
	}

	public void setAreYouFightResult(boolean value) {
		areYouFight.setResult(value);
	}

	public void Start(App app) {
		loginEventCanceler = Objects.requireNonNull(Objects.requireNonNull(app.Provider).getOnline()).getLoginEvents()
				.add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
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
		switch (fighterId.getType()) {
		case BFighterId.TypeRole:
			Objects.requireNonNull(App.Game_Buf).getBufs(fighterId.getInstanceId()).calculateFighter(fighter);
			Objects.requireNonNull(App.Game_Equip).calculateFighter(fighter);
			break;
		}
		_tfighters.getOrAdd(fighterId).assign(fighter.getBean());
		return Procedure.Success;
	}

	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		TaskSpec.ofProcedure(Objects.requireNonNull(Game.App.getInstance().Zeze).newProcedure(() -> CalculateFighter(fighterId),
				"CalculateFighter")).run();
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFight(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

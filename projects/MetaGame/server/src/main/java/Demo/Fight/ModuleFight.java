package Demo.Fight;

import Zeze.Game.LoginArgument;
import Zeze.Game.Online;
import Zeze.Hot.HotService;
import Zeze.Transaction.*;
import Demo.*;
import Zeze.Util.EventDispatcher;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class ModuleFight extends AbstractModule implements IModuleFight {
	private static final Logger logger = LogManager.getLogger(ModuleFight.class);
	public TaskCompletionSource<Boolean> areYouFight = new TaskCompletionSource<>();

	@Override
	public boolean isAreYouFightDone() {
		return areYouFight.isDone();
	}

	@Override
	public void setAreYouFightResult(boolean value) {
		areYouFight.setResult(value);
	}

	public static class LoginEventHandle implements EventDispatcher.EventHandle {
		@Override
		public long invoke(@NotNull Object sender, EventDispatcher.EventArgument arg) throws Exception {
			var online = (Online)sender;
			var login = (LoginArgument)arg;
			var context = online.providerApp.zeze.getHotManager().getModuleContext("Game.Fight", IModuleFight.class);
			var service = context.getService();
			online.sendOnlineRpc(login.roleId, new AreYouFight(), (p) -> {
				logger.info("AreYouFight done.");
				service.setAreYouFightResult(true);
				return 0;
			});
			return 0;
		}
	}

	public void Start(App app) {
		app.Provider.getOnline().getLoginEvents().addHot(EventDispatcher.Mode.RunEmbed, LoginEventHandle.class);
	}

	public void Stop(App app) {
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

	@Override
	public void StartCalculateFighter(long roleId) {
		BFighterId fighterId = new BFighterId(BFighterId.TypeRole, roleId);
		Zeze.Util.Task.run(Demo.App.getInstance().Zeze.newProcedure(() -> CalculateFighter(fighterId),
				"CalculateFighter"), null, null, DispatchMode.Normal);
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFight(Demo.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

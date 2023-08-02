package Game.Rank;

import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Hot.HotService;

public interface IModuleRank extends HotService {
	RedirectFuture<ModuleRank.TestToServerResult> TestToServer(int serverId, int in);
	RedirectFuture<ModuleRank.TestHashResult> TestHash(int hash, int in);
	RedirectAllFuture<ModuleRank.TestToAllResult> TestToAll(int hash, int in) throws Exception;
}

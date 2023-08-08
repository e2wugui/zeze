package Game.Rank;

import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Hot.HotService;

public interface IModuleRank extends HotService {
	RedirectFuture<TestToServerResult> TestToServer(int serverId, int in);
	RedirectFuture<TestHashResult> TestHash(int hash, int in);
	RedirectAllFuture<TestToAllResult> TestToAll(int hash, int in) throws Exception;
}

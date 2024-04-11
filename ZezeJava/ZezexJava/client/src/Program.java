import ClientGame.App;

public class Program {
	public static void main(String [] args) throws Exception {
		App.getInstance().Start("127.0.0.1", 10000);
		App.getInstance().ClientGame_Login.auth();
		try {
			var role = App.getInstance().ClientGame_Login.getOrCreateRole("HotTestRole");
			App.getInstance().Zeze_Builtin_Game_Online.login(role.getId());
			App.getInstance().ClientGame_Equip.reportLogin(role.getId());
			App.getInstance().ClientGame_Equip.startTimer();
			Program.class.wait();
		} finally {
			App.getInstance().Stop();
		}
	}
}

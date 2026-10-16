import java.util.Objects;
import ClientGame.App;
import ClientZezex.Linkd.Cs;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Program {
	public synchronized static void main(String[] args) throws Exception {
		App.getInstance().Start("127.0.0.1", 10000);
		Objects.requireNonNull(App.getInstance().ClientGame_Login).auth();
		try {
			var role = Objects.requireNonNull(App.getInstance().ClientGame_Login).getOrCreateRole("HotTestRole");
			Objects.requireNonNull(App.getInstance().Zeze_Builtin_Game_Online).login(role.getId());
			Objects.requireNonNull(App.getInstance().ClientGame_Equip).reportLogin(role.getId());
			Objects.requireNonNull(App.getInstance().ClientGame_Equip).startTimer();
			Program.class.wait();
		} finally {
			App.getInstance().Stop();
		}
	}
}

package UnitTest.Zeze.RocketMQ;

import Zeze.Services.RocketMQ.Message;
import Zeze.Services.RocketMQ.TransactionListener;

class UserLogin {
	boolean isLogin = false;

	// a very long transaction
	public void inputAndCheckPassword() throws Exception {
		Thread.sleep(10000); // wait for user input password
		isLogin = true;
	}
}

class inputPasswordTransaction extends TransactionListener {

	UserLogin userLogin;

	public inputPasswordTransaction(UserLogin userLogin) {
		this.userLogin = userLogin;
	}

	@Override
	public State sendHalfMessage(Message message, Object arg) {
		try {
			userLogin.inputAndCheckPassword();
		} catch (Exception e) {
			e.printStackTrace();
			return State.ROLLBACK_MESSAGE;
		}
		if (userLogin.isLogin) {
			return State.COMMIT_MESSAGE;
		}
		return State.UNKNOW;
	}

	@Override
	public State checkTransaction(Message message) {
		if (userLogin.isLogin)
			return State.COMMIT_MESSAGE;
		return State.UNKNOW;
	}
}

public class TestTransactionProvider2 {
	public static void main(String[] args) {
		UserLogin userLogin = new UserLogin();

	}
}

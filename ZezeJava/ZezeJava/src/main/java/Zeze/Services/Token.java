package Zeze.Services;

public class Token extends AbstractToken {
	@Override
	protected long ProcessGetTokenRequest(Zeze.Builtin.Token.GetToken r) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	@Override
	protected long ProcessNewTokenRequest(Zeze.Builtin.Token.NewToken r) {
		return Zeze.Transaction.Procedure.NotImplement;
	}
}

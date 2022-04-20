package Zeze.Game;

import Zeze.Beans.Game.Online.BAccount;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Online extends AbstractOnline {
	protected static final Logger logger = LogManager.getLogger(Online.class);

	public Online(Service service) {
		RegisterProtocols(service);
		RegisterZezeTables(service.getZeze());
	}

	public long addRole(String account, long roleId) {
		BAccount bAccount = _taccount.getOrAdd(account);
		if (bAccount.getName().isEmpty())
			bAccount.setName(account);
		if (bAccount.getRoles().contains(roleId))
			return RAlreadyExistRoleId;
		bAccount.getRoles().add(roleId);
		return Procedure.Success;
	}

	public long removeRole(String account, long roleId) {
		BAccount bAccount = _taccount.get(account);
		if (bAccount == null)
			return RNotExistAccount;
		if (!bAccount.getRoles().remove(roleId))
			return RNotExistRoleId;
		return Procedure.Success;
	}

	public void sendRole(long roleId, Protocol<?> p) {
		//TODO
	}

	public void sendAccount(String account, Protocol<?> p) {
		//TODO
	}

	@Override
	protected long ProcessLoginRequest(Zeze.Beans.Game.Online.Login r) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	@Override
	protected long ProcessLogoutRequest(Zeze.Beans.Game.Online.Logout r) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	@Override
	protected long ProcessReliableNotifyConfirmRequest(Zeze.Beans.Game.Online.ReliableNotifyConfirm r) {
		return Zeze.Transaction.Procedure.NotImplement;
	}

	@Override
	protected long ProcessReLoginRequest(Zeze.Beans.Game.Online.ReLogin r) {
		return Zeze.Transaction.Procedure.NotImplement;
	}
}

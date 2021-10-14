package Game.Equip;

import Game.*;

// auto-generated



public final class Equipement extends Zeze.Net.Rpc<Game.Equip.BEquipement, Zeze.Transaction.EmptyBean> {
	public static final int ModuleId_ = 7;
	public static final int ProtocolId_ = 53522;
	public static final int TypeId_ = ModuleId_ << 16 | ProtocolId_;

	@Override
	public int getModuleId() {
		return ModuleId_;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}
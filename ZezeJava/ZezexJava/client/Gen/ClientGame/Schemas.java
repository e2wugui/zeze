// auto-generated @formatter:off
package ClientGame;

public class Schemas extends Zeze.Schemas {
    public Schemas() {
        addTable(new Zeze.Schemas.Table("ClientGame_Equip_tequip", "long", "ClientGame.Equip.BEquips", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Equip_tHotRemove", "long", "ClientGame.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Equip_tHotTest", "long", "ClientGame.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Equip_tMemoryRetreat", "long", "ClientGame.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Equip_tMemoryToNormal", "long", "ClientGame.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Equip_tNormalToMemory", "long", "ClientGame.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Fight_tfighters", "ClientGame.Fight.BFighterId", "ClientGame.Fight.BFighter", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Login_taccount", "string", "ClientGame.Login.BAccount", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Login_trole", "long", "ClientGame.Login.BRole", 0));
        addTable(new Zeze.Schemas.Table("ClientGame_Login_trolename", "string", "ClientGame.Login.BRoleId", 0));
    }
}

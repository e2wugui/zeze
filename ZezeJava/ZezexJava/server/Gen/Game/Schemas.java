// auto-generated @formatter:off
package Game;

public class Schemas extends Zeze.Schemas {
    public Schemas() {
        addTable(new Zeze.Schemas.Table("Game_Buf_tbufs", "long", "Game.Buf.BBufs", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tequip", "long", "Game.Equip.BEquips", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tHotRemove", "long", "Game.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tHotTest", "long", "Game.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tMemoryRetreat", "long", "Game.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tMemoryToNormal", "long", "Game.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tNormalToMemory", "long", "Game.Equip.BEquipExtra", 0));
        addTable(new Zeze.Schemas.Table("Game_Equip_tRelationalTest", "long", "Game.Equip.BRelational", 0));
        addTable(new Zeze.Schemas.Table("Game_Fight_tfighters", "Game.Fight.BFighterId", "Game.Fight.BFighter", 0));
        addTable(new Zeze.Schemas.Table("Game_Login_taccount", "string", "Game.Login.BAccount", 0));
        addTable(new Zeze.Schemas.Table("Game_Login_trole", "long", "Game.Login.BRole", 0));
        addTable(new Zeze.Schemas.Table("Game_Login_trolename", "string", "Game.Login.BRoleId", 0));
        addTable(new Zeze.Schemas.Table("Game_LongSet_tIndexs", "string", "Game.LongSet.BNodeId", 0));
        addTable(new Zeze.Schemas.Table("Game_LongSet_tNodeRoot", "string", "Game.LongSet.BNodeRoot", 0));
        addTable(new Zeze.Schemas.Table("Game_LongSet_tNodes", "long", "Game.LongSet.BNode", 0));
        addTable(new Zeze.Schemas.Table("Game_Skill_tskills", "long", "Game.Skill.BSkills", 0));
        addTable(new Zeze.Schemas.Table("Game_Timer_tIndexs", "long", "Game.Timer.BIndex", 0));
        addTable(new Zeze.Schemas.Table("Game_Timer_tNodeRoot", "int", "Game.Timer.BNodeRoot", 0));
        addTable(new Zeze.Schemas.Table("Game_Timer_tNodes", "long", "Game.Timer.BNode", 0));
        addBean_Game_Buf_BBufs();
        addBean_Game_Buf_BBuf();
        addBean_Game_Buf_BBufExtra();
        addBean_Game_Equip_BEquips();
        addBean_Game_Equip_BItem();
        addBean_Game_Item_BHorseExtra();
        addBean_Game_Item_BFoodExtra();
        addBean_Game_Equip_BEquipExtra();
        addBean_Game_Equip_BRelational();
        addBean_Game_Fight_BFighterId();
        addBean_Game_Fight_BFighter();
        addBean_Game_Login_BAccount();
        addBean_Game_Login_BRole();
        addBean_Game_Login_BRoleId();
        addBean_Game_LongSet_BNodeId();
        addBean_Game_LongSet_BNodeRoot();
        addBean_Game_LongSet_BNode();
        addBean_Game_LongSet_NameValue();
        addBean_Game_LongSet_Timestamp();
        addBean_Game_Skill_BSkills();
        addBean_Game_Skill_BSkill();
        addBean_Game_Skill_BSkillAttackExtra();
        addBean_Game_Timer_BIndex();
        addBean_Game_Timer_BNodeRoot();
        addBean_Game_Timer_BNode();
        addBean_Game_Timer_BTimer();
    }

    private void addBean_Game_Buf_BBufs() {
        var bean = new Zeze.Schemas.Bean("Game.Buf.BBufs", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Bufs";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "Game.Buf.BBuf";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Buf_BBuf() {
        var bean = new Zeze.Schemas.Bean("Game.Buf.BBuf", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Id";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "AttachTime";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "ContinueTime";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "Extra";
            var.typeName = "dynamic";
            var.dynamicBeans.put(7506982410669108623L, "Game.Buf.BBufExtra");
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Buf_BBufExtra() {
        var bean = new Zeze.Schemas.Bean("Game.Buf.BBufExtra", false);
        addBean(bean);
    }

    private void addBean_Game_Equip_BEquips() {
        var bean = new Zeze.Schemas.Bean("Game.Equip.BEquips", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Items";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "Game.Equip.BItem";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Equip_BItem() {
        var bean = new Zeze.Schemas.Bean("Game.Equip.BItem", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Id";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Number";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Extra";
            var.typeName = "dynamic";
            var.dynamicBeans.put(-6414823809446200925L, "Game.Item.BHorseExtra");
            var.dynamicBeans.put(-5635260117858385112L, "Game.Item.BFoodExtra");
            var.dynamicBeans.put(1076067654005167423L, "Game.Equip.BEquipExtra");
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Item_BHorseExtra() {
        var bean = new Zeze.Schemas.Bean("Game.Item.BHorseExtra", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Speed";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Item_BFoodExtra() {
        var bean = new Zeze.Schemas.Bean("Game.Item.BFoodExtra", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Ammount";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Equip_BEquipExtra() {
        var bean = new Zeze.Schemas.Bean("Game.Equip.BEquipExtra", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Attack";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Defence";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "HotRemoveVar";
            var.typeName = "int";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Equip_BRelational() {
        var bean = new Zeze.Schemas.Bean("Game.Equip.BRelational", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Id";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Fight_BFighterId() {
        var bean = new Zeze.Schemas.Bean("Game.Fight.BFighterId", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Type";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "InstanceId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Fight_BFighter() {
        var bean = new Zeze.Schemas.Bean("Game.Fight.BFighter", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Attack";
            var.typeName = "float";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Defence";
            var.typeName = "float";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Login_BAccount() {
        var bean = new Zeze.Schemas.Bean("Game.Login.BAccount", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Roles";
            var.typeName = "set";
            var.valueName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "LastLoginRoleId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Login_BRole() {
        var bean = new Zeze.Schemas.Bean("Game.Login.BRole", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Id";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Name";
            var.typeName = "string";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Login_BRoleId() {
        var bean = new Zeze.Schemas.Bean("Game.Login.BRoleId", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Id";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_LongSet_BNodeId() {
        var bean = new Zeze.Schemas.Bean("Game.LongSet.BNodeId", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "NodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_LongSet_BNodeRoot() {
        var bean = new Zeze.Schemas.Bean("Game.LongSet.BNodeRoot", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "HeadNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "TailNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_LongSet_BNode() {
        var bean = new Zeze.Schemas.Bean("Game.LongSet.BNode", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "PrevNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "NextNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Set";
            var.typeName = "map";
            var.keyName = "Game.LongSet.NameValue";
            var.valueName = "Game.LongSet.Timestamp";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_LongSet_NameValue() {
        var bean = new Zeze.Schemas.Bean("Game.LongSet.NameValue", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Name";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Value";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_LongSet_Timestamp() {
        var bean = new Zeze.Schemas.Bean("Game.LongSet.Timestamp", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Time";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Skill_BSkills() {
        var bean = new Zeze.Schemas.Bean("Game.Skill.BSkills", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Skills";
            var.typeName = "map";
            var.keyName = "int";
            var.valueName = "Game.Skill.BSkill";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Skill_BSkill() {
        var bean = new Zeze.Schemas.Bean("Game.Skill.BSkill", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "Id";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Extra";
            var.typeName = "dynamic";
            var.dynamicBeans.put(5301582953788202231L, "Game.Skill.BSkillAttackExtra");
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Skill_BSkillAttackExtra() {
        var bean = new Zeze.Schemas.Bean("Game.Skill.BSkillAttackExtra", false);
        addBean(bean);
    }

    private void addBean_Game_Timer_BIndex() {
        var bean = new Zeze.Schemas.Bean("Game.Timer.BIndex", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "ServerId";
            var.typeName = "int";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "NodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Timer_BNodeRoot() {
        var bean = new Zeze.Schemas.Bean("Game.Timer.BNodeRoot", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "HeadNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "TailNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "LoadSerialNo";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Timer_BNode() {
        var bean = new Zeze.Schemas.Bean("Game.Timer.BNode", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "PrevNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "NextNodeId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Timers";
            var.typeName = "map";
            var.keyName = "long";
            var.valueName = "Game.Timer.BTimer";
            bean.addVariable(var);
        }
        addBean(bean);
    }

    private void addBean_Game_Timer_BTimer() {
        var bean = new Zeze.Schemas.Bean("Game.Timer.BTimer", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 1;
            var.name = "TimerId";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 2;
            var.name = "Name";
            var.typeName = "string";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 3;
            var.name = "Delay";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 4;
            var.name = "Period";
            var.typeName = "long";
            bean.addVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.id = 5;
            var.name = "RemainTimes";
            var.typeName = "long";
            bean.addVariable(var);
        }
        addBean(bean);
    }
}

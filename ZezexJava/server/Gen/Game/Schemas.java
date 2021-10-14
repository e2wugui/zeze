// auto-generated
package Game;

public class Schemas extends Zeze.Schemas {
    public Schemas() {
        AddTable(new Zeze.Schemas.Table("Game_Bag_tbag", "long", "Game.Bag.BBag"));
        AddTable(new Zeze.Schemas.Table("Game_Buf_tbufs", "long", "Game.Buf.BBufs"));
        AddTable(new Zeze.Schemas.Table("Game_Equip_tequip", "long", "Game.Equip.BEquips"));
        AddTable(new Zeze.Schemas.Table("Game_Fight_tfighters", "Game.Fight.BFighterId", "Game.Fight.BFighter"));
        AddTable(new Zeze.Schemas.Table("Game_Login_taccount", "string", "Game.Login.BAccount"));
        AddTable(new Zeze.Schemas.Table("Game_Login_tonline", "long", "Game.Login.BOnline"));
        AddTable(new Zeze.Schemas.Table("Game_Login_trole", "long", "Game.Login.BRoleData"));
        AddTable(new Zeze.Schemas.Table("Game_Login_trolename", "string", "Game.Login.BRoleId"));
        AddTable(new Zeze.Schemas.Table("Game_Rank_trank", "Game.Rank.BConcurrentKey", "Game.Rank.BRankList"));
        AddTable(new Zeze.Schemas.Table("Game_Rank_trankcounters", "long", "Game.Rank.BRankCounters"));
        AddTable(new Zeze.Schemas.Table("Game_Skill_tskills", "long", "Game.Skill.BSkills"));
        {
            var bean = new Zeze.Schemas.Bean("Game.Bag.BBag", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Money";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Capacity";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "Items";
            var.TypeName = "map";
            var.KeyName = "int";
            var.ValueName = "Game.Bag.BItem";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Bag.BItem", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Id";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Number";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "Extra";
            var.TypeName = "dynamic";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Item.BHorseExtra", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Speed";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Item.BFoodExtra", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Ammount";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Equip.BEquipExtra", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Attack";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Defence";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Buf.BBufs", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Bufs";
            var.TypeName = "map";
            var.KeyName = "int";
            var.ValueName = "Game.Buf.BBuf";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Buf.BBuf", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Id";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "AttachTime";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "ContinueTime";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 4;
            var.Name = "Extra";
            var.TypeName = "dynamic";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Buf.BBufExtra", false);
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Equip.BEquips", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Items";
            var.TypeName = "map";
            var.KeyName = "int";
            var.ValueName = "Game.Bag.BItem";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Fight.BFighterId", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Type";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "InstanceId";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Fight.BFighter", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Attack";
            var.TypeName = "float";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Defence";
            var.TypeName = "float";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Login.BAccount", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Name";
            var.TypeName = "string";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Roles";
            var.TypeName = "list";
            var.ValueName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "LastLoginRoleId";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Login.BOnline", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "LinkName";
            var.TypeName = "string";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "LinkSid";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "State";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 4;
            var.Name = "ReliableNotifyMark";
            var.TypeName = "set";
            var.ValueName = "string";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 5;
            var.Name = "ReliableNotifyQueue";
            var.TypeName = "list";
            var.ValueName = "binary";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 6;
            var.Name = "ReliableNotifyConfirmCount";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 7;
            var.Name = "ReliableNotifyTotalCount";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 8;
            var.Name = "ProviderId";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 9;
            var.Name = "ProviderSessionId";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Login.BRoleData", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Name";
            var.TypeName = "string";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Login.BRoleId", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Id";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Rank.BConcurrentKey", true);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "RankType";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "ConcurrentId";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "TimeType";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 4;
            var.Name = "Year";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 5;
            var.Name = "Offset";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Rank.BRankList", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "RankList";
            var.TypeName = "list";
            var.ValueName = "Game.Rank.BRankValue";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Rank.BRankValue", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "RoleId";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Value";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 3;
            var.Name = "ValueEx";
            var.TypeName = "binary";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 4;
            var.Name = "AwardTaken";
            var.TypeName = "bool";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Rank.BRankCounters", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Counters";
            var.TypeName = "map";
            var.KeyName = "Game.Rank.BConcurrentKey";
            var.ValueName = "Game.Rank.BRankCounter";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Rank.BRankCounter", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Value";
            var.TypeName = "long";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Skill.BSkills", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Skills";
            var.TypeName = "map";
            var.KeyName = "int";
            var.ValueName = "Game.Skill.BSkill";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Skill.BSkill", false);
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 1;
            var.Name = "Id";
            var.TypeName = "int";
            bean.AddVariable(var);
        }
        {
            var var = new Zeze.Schemas.Variable();
            var.Id = 2;
            var.Name = "Extra";
            var.TypeName = "dynamic";
            bean.AddVariable(var);
        }
        AddBean(bean);
    }
        {
            var bean = new Zeze.Schemas.Bean("Game.Skill.BSkillAttackExtra", false);
        AddBean(bean);
    }
    }
}

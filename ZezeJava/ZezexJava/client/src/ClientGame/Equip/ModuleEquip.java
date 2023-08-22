package ClientGame.Equip;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.Task;

public class ModuleEquip extends AbstractModule {
    AtomicInteger sendHot = new AtomicInteger();
    private void testSendHot() {
        var r = new SendHot();
        r.Argument.setAttack(sendHot.incrementAndGet());
        r.Send(App.ClientService.GetSocket(), (p) -> {
            System.out.println("SendHot " + r.getResultCode() + " count=" + r.Argument.getAttack());
            return 0;
        });
    }

    private void testSendHotRemove() {
        var r = new SendHotRemove();
        r.Argument.setAttack(sendHot.incrementAndGet());
        r.Send(App.ClientService.GetSocket(), (p) -> {
            System.out.println("SendHotRemove " + r.getResultCode() + " count=" + r.Argument.getAttack());
            return 0;
        });
    }

    private void testSendHotAdd() {
        var r = new SendHotAdd();
        r.Argument.setAttack(sendHot.incrementAndGet());
        r.Send(App.ClientService.GetSocket(), (p) -> {
            System.out.println("SendHotAdd " + r.getResultCode() + " count=" + r.Argument.getAttack());
            return 0;
        });
    }

    Future<?> timerSendHot;
    Future<?> timerSendHotRemove;
    Future<?> timerSendHotAdd;

    public void Start(ClientGame.App app) throws Exception {
        timerSendHot = Task.scheduleUnsafe(Zeze.Util.Random.getInstance().nextLong(5000), 5000, this::testSendHot);
        timerSendHotRemove = Task.scheduleUnsafe(Zeze.Util.Random.getInstance().nextLong(5000), 5000, this::testSendHotRemove);
        timerSendHotAdd = Task.scheduleUnsafe(Zeze.Util.Random.getInstance().nextLong(5000), 5000, this::testSendHotAdd);
    }

    public void Stop(ClientGame.App app) throws Exception {
        if (null != timerSendHot) {
            timerSendHot.cancel(true);
            timerSendHot = null;
        }
        if (null != timerSendHotRemove) {
            timerSendHotRemove.cancel(true);
            timerSendHotRemove = null;
        }
        if (null != timerSendHotAdd) {
            timerSendHotAdd.cancel(true);
            timerSendHotAdd = null;
        }
    }

    @Override
    protected long ProcessSEquipement(ClientGame.Equip.SEquipement p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleEquip(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}

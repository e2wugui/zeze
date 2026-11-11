import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import ConfCsDynamicLogs.Demo.Amount;
import ConfCsDynamicLogs.Demo.Detail;
import ConfCsDynamicLogs.Demo.Order;
import ConfCsDynamicLogs.Demo.Quantity;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Collections.LogOneFixture;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableDynamic;

public final class WriteChanges {
    private static final int DETAIL_FIELD_ID = 10;

    public static void main(String[] args) throws Exception {
        var config = new Config();
        config.setServiceManager("disable");
        config.setTakeoverMode("off");
        config.setCheckpointMode(CheckpointMode.Immediately);
        config.setDefaultTableConf(new Config.TableConf());
        var database = new Config.DatabaseConf();
        database.setDatabaseUrl("confcs_dynamic_logs");
        config.getDatabaseConfMap().put("", database);
        var app = new Application("ConfCsDynamicLogs", config);
        try {
            app.start();
            var table = new TableDynamic<Long, Order>(app, "confcs_dynamic_orders",
                    key -> {
                        var bb = ByteBuffer.Allocate();
                        bb.WriteLong(key);
                        return bb;
                    },
                    ByteBuffer::ReadLong, Order::new, false, null, Long.class, Order.class);
            var changes = new ArrayList<byte[]>();
            var childEdits = new ArrayList<byte[]>();
            table.getChangeListenerMap().addListener((key, record) -> {
                if (changes.size() == 6 || changes.size() == 7)
                    childEdits.add(LogOneFixture.encodeChildEdit(table.encodeKey((Long)key), record, DETAIL_FIELD_ID));
                changes.add(table.encodeChangeListenerWithSpecialName("orders", key, record).Copy());
            });
            run(app, () -> table.put(1L, new Order()));
            run(app, () -> {
                var order = table.get(1L);
                var item = Order.newDynamicBean_Quantities();
                item.setBean(new Quantity(42));
                order.getQuantities().add(item);
            });
            run(app, () -> table.get(1L).setDirectQuantity(new Quantity(84)));
            run(app, () -> {
                var order = table.get(1L);
                var item = Order.newDynamicBean_QuantityMap();
                item.setBean(new Quantity(21));
                order.getQuantityMap().put(3, item);
                var amount = Order.newDynamicBean_AmountSorted();
                amount.setBean(new Amount(63));
                order.getAmountSorted().put(5, amount);
            });
            run(app, () -> ((Quantity)table.get(1L).getQuantities().get(0).getBean()).setValue(43));
            run(app, () -> table.get(1L).getDirectQuantity().reset());
            run(app, () -> {
                var detail = table.get(1L).getDetail();
                detail.setPayload(new Quantity(64));
                detail.setRevision(1);
            });
            run(app, () -> ((Quantity)table.get(1L).getDetail().getPayload().getBean()).setValue(65));
            run(app, () -> {
                var detail = new Detail();
                detail.setPayload(new Quantity(90));
                detail.setRevision(2);
                table.get(1L).setDetail(detail);
            });
            if (changes.size() != 9)
                throw new AssertionError("expected 9 committed change notifications, got " + changes.size());
            if (childEdits.size() != 2)
                throw new AssertionError("expected 2 child-only LogOne payloads, got " + childEdits.size());
            changes.addAll(childEdits);
            for (int i = 0; i < changes.size(); i++)
                Files.write(Path.of(args[0], i + ".bin"), changes.get(i));
        } finally {
            app.stop();
        }
    }

    private static void run(Application app, Runnable change) {
        long result = app.newProcedure(() -> {
            change.run();
            return Procedure.Success;
        }, "WriteChange").call();
        if (result != Procedure.Success)
            throw new AssertionError("transaction failed: " + result);
    }
}

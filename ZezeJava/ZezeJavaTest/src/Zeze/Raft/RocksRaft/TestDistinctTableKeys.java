package Zeze.Raft.RocksRaft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RaftLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestDistinctTableKeys {
    @TempDir Path directory;

    @Test
    public void compareEquivalentDecimalKeysRemainDistinctThroughReadAndReplay() throws Exception {
        Task.tryInitThreadPool();
        var home = directory.resolve("raft").toAbsolutePath().toString();
        // Headless Raft：不绑定端口，无Application/内存库，TempDir独占数据目录。
        var cfg = RaftConfig.loadFromString("<raft Name=\"127.0.0.1:24994\" DbHome=\""
                + home + "\"><node Host=\"127.0.0.1\" Port=\"24994\"/>"
                + "<node Host=\"127.0.0.1\" Port=\"24995\"/>"
                + "<node Host=\"127.0.0.1\" Port=\"24996\"/></raft>");
        try (var rocks = new Rocks("127.0.0.1:24994", RocksMode.Pessimism, cfg, new Config(), false)) {
            rocks.registerTableTemplate("DecimalKeys", BigDecimal.class, Zeze.Raft.RocksRaft.Test.Bean2.class);
            var table = requireNonNull(rocks.<BigDecimal, Zeze.Raft.RocksRaft.Test.Bean2>getTableTemplate("DecimalKeys")).openTable();
            var first = new BigDecimal("1.0");
            var second = new BigDecimal("1.00");
            assertEquals(0, first.compareTo(second));
            assertNotEquals(first, second);
            seed(table, first, 7);
            seed(table, second, 8);
            var tx = new Transaction[1];
            assertEquals(Zeze.Transaction.Procedure.RaftRetry, rocks.newProcedure(() -> {
                tx[0] = requireNonNull(Transaction.getCurrent());
                var a = requireNonNull(table.get(first));
                var b = requireNonNull(table.get(second));
                assertEquals(7, a.getI());
                assertEquals(8, b.getI(), "compareTo等价不能合并编码与equals不同的表键");
                assertNotSame(a, b);
                b.setI(9);
                assertEquals(7, a.getI(), "修改第二个键不能污染第一个键");
                return 0;
            }).call());
            var changes = requireNonNull(tx[0].getChanges());
            assertEquals(1, changes.getRecords().size());
            assertTrue(changes.getRecords().containsKey(new TableKey(table.getName(), second)));
            var wire = ByteBuffer.Allocate();
            changes.encode(wire);
            var replay = new Changes(rocks);
            replay.decode(ByteBuffer.Wrap(wire.Copy()));
            rocks.followerApply(replay, new RaftLog(1, 1, replay));
            assertEquals(7, requireNonNull(table.selectDirty(first)).getI());
            assertEquals(9, requireNonNull(table.selectDirty(second)).getI());
        }
    }

    private static void seed(Table<BigDecimal, Zeze.Raft.RocksRaft.Test.Bean2> table,
                             BigDecimal key, int number) throws Exception {
        var keyBytes = ByteBuffer.Allocate();
        table.encodeKey(keyBytes, key);
        var bean = new Zeze.Raft.RocksRaft.Test.Bean2();
        bean.setI(number);
        var value = ByteBuffer.Allocate();
        bean.encode(value);
        table.getRocksTable().put(keyBytes.Copy(), value.Copy());
    }
}
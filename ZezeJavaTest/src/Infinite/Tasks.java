package Infinite;

public class Tasks {
    public static abstract class Task implements Runnable {
        long key;
        demo.App App;
    }

    public static class Table1Long2Add1 extends Task {
        @Override
        public void run() {
            var value = App.demo_Module1.getTable1().getOrAdd(key);
            value.setLong2(value.getLong2() + 1);
        }
    }

    public static class Table1List9AddOrRemove extends Task {
        @Override
        public void run() {
            var value = App.demo_Module1.getTable1().getOrAdd(key);
            // 使用 bool4 变量：用来决定添加或者删除。
            if (value.isBool4()) {
                if (!value.getList9().isEmpty())
                    value.getList9().remove(value.getList9().size() - 1);

                value.setBool4(!value.getList9().isEmpty());
            } else {
                value.getList9().add(new demo.Bean1());
                if (value.getList9().size() > 50)
                    value.setBool4(true); // 改成删除模式。
            }
        }
    }
}

// auto-generated @formatter:off
package dynamicred.Demo;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public class Order extends Zeze.Transaction.Bean implements OrderReadOnly {
    public static final long TYPEID = 7473753921427161578L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> _Amounts;
    private static final Zeze.Transaction.DynamicField dynamicField_Amounts
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 1, Order::getSpecialTypeIdFromBean_1, Order::createBeanFromSpecialTypeId_1, dynamicred.Demo.Amount.class);
    private static final Zeze.Transaction.Collections.List2Meta<Zeze.Transaction.DynamicBean> meta1_Amounts
            = Zeze.Transaction.Collections.List2Meta.createDynamic(dynamicField_Amounts);

    public static Zeze.Transaction.DynamicBean newDynamicBean_Amounts() {
        return new Zeze.Transaction.DynamicBean(dynamicField_Amounts);
    }

    public static long getSpecialTypeIdFromBean_1(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 5587247785002748608L)
            return 7L; // dynamicred.Demo.Amount
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:Amounts:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_1(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Amount();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> _Quantities;
    private static final Zeze.Transaction.DynamicField dynamicField_Quantities
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 2, Order::getSpecialTypeIdFromBean_2, Order::createBeanFromSpecialTypeId_2, dynamicred.Demo.Quantity.class);
    private static final Zeze.Transaction.Collections.List2Meta<Zeze.Transaction.DynamicBean> meta1_Quantities
            = Zeze.Transaction.Collections.List2Meta.createDynamic(dynamicField_Quantities);

    public static Zeze.Transaction.DynamicBean newDynamicBean_Quantities() {
        return new Zeze.Transaction.DynamicBean(dynamicField_Quantities);
    }

    public static long getSpecialTypeIdFromBean_2(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4144526925789372221L)
            return 7L; // dynamicred.Demo.Quantity
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:Quantities:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_2(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Quantity();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> _Lines;
    private static final Zeze.Transaction.DynamicField dynamicField_Lines
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 3, Order::getSpecialTypeIdFromBean_3, Order::createBeanFromSpecialTypeId_3, dynamicred.Demo.Line.class);
    private static final Zeze.Transaction.Collections.List2Meta<Zeze.Transaction.DynamicBean> meta1_Lines
            = Zeze.Transaction.Collections.List2Meta.createDynamic(dynamicField_Lines);

    public static Zeze.Transaction.DynamicBean newDynamicBean_Lines() {
        return new Zeze.Transaction.DynamicBean(dynamicField_Lines);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 5260758296393585156L)
            return 11L; // dynamicred.Demo.Line
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:Lines:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_3(long _t_) {
        if (_t_ == 11L)
            return new dynamicred.Demo.Line();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> _AmountGrid;
    private static final Zeze.Transaction.DynamicField dynamicField_AmountGrid
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 4, Order::getSpecialTypeIdFromBean_4, Order::createBeanFromSpecialTypeId_4, dynamicred.Demo.Amount.class);
    private static final Zeze.Transaction.GTable.GTable2.Factory<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> factory_AmountGrid
            = Zeze.Transaction.GTable.GTable2.getFactory(Integer.class, Integer.class, dynamicField_AmountGrid);

    public static Zeze.Transaction.DynamicBean newDynamicBean_AmountGrid() {
        return new Zeze.Transaction.DynamicBean(dynamicField_AmountGrid);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 5587247785002748608L)
            return 7L; // dynamicred.Demo.Amount
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:AmountGrid:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_4(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Amount();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> _QuantityGrid;
    private static final Zeze.Transaction.DynamicField dynamicField_QuantityGrid
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 5, Order::getSpecialTypeIdFromBean_5, Order::createBeanFromSpecialTypeId_5, dynamicred.Demo.Quantity.class);
    private static final Zeze.Transaction.GTable.GTable2.Factory<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> factory_QuantityGrid
            = Zeze.Transaction.GTable.GTable2.getFactory(Integer.class, Integer.class, dynamicField_QuantityGrid);

    public static Zeze.Transaction.DynamicBean newDynamicBean_QuantityGrid() {
        return new Zeze.Transaction.DynamicBean(dynamicField_QuantityGrid);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4144526925789372221L)
            return 7L; // dynamicred.Demo.Quantity
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:QuantityGrid:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_5(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Quantity();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _DirectAmount;
    private static final Zeze.Transaction.DynamicField dynamicField_DirectAmount
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 6, Order::getSpecialTypeIdFromBean_6, Order::createBeanFromSpecialTypeId_6, dynamicred.Demo.Amount.class);

    public static final long DynamicTypeId_DirectAmount_dynamicred_Demo_Amount = 7L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_DirectAmount() {
        return new Zeze.Transaction.DynamicBean(dynamicField_DirectAmount);
    }

    public static long getSpecialTypeIdFromBean_6(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 5587247785002748608L)
            return 7L; // dynamicred.Demo.Amount
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:DirectAmount:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_6(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Amount();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _DirectQuantity;
    private static final Zeze.Transaction.DynamicField dynamicField_DirectQuantity
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 7, Order::getSpecialTypeIdFromBean_7, Order::createBeanFromSpecialTypeId_7, dynamicred.Demo.Quantity.class);

    public static final long DynamicTypeId_DirectQuantity_dynamicred_Demo_Quantity = 7L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_DirectQuantity() {
        return new Zeze.Transaction.DynamicBean(dynamicField_DirectQuantity);
    }

    public static long getSpecialTypeIdFromBean_7(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4144526925789372221L)
            return 7L; // dynamicred.Demo.Quantity
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:DirectQuantity:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_7(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Quantity();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.Collections.PMap2<Integer, Zeze.Transaction.DynamicBean> _QuantityMap;
    private static final Zeze.Transaction.DynamicField dynamicField_QuantityMap
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 8, Order::getSpecialTypeIdFromBean_8, Order::createBeanFromSpecialTypeId_8, dynamicred.Demo.Quantity.class);
    private static final Zeze.Transaction.Collections.Map2Meta<Integer, Zeze.Transaction.DynamicBean> meta2_QuantityMap
            = Zeze.Transaction.Collections.Map2Meta.createDynamic(Integer.class, dynamicField_QuantityMap);

    public static Zeze.Transaction.DynamicBean newDynamicBean_QuantityMap() {
        return new Zeze.Transaction.DynamicBean(dynamicField_QuantityMap);
    }

    public static long getSpecialTypeIdFromBean_8(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4144526925789372221L)
            return 7L; // dynamicred.Demo.Quantity
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:QuantityMap:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_8(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Quantity();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.Collections.PSortedMap2<Integer, Zeze.Transaction.DynamicBean> _AmountSorted;
    private static final Zeze.Transaction.DynamicField dynamicField_AmountSorted
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 9, Order::getSpecialTypeIdFromBean_9, Order::createBeanFromSpecialTypeId_9, dynamicred.Demo.Amount.class);
    private static final Zeze.Transaction.Collections.SortedMap2Meta<Integer, Zeze.Transaction.DynamicBean> meta2_AmountSorted
            = Zeze.Transaction.Collections.SortedMap2Meta.createDynamic(Integer.class, dynamicField_AmountSorted);

    public static Zeze.Transaction.DynamicBean newDynamicBean_AmountSorted() {
        return new Zeze.Transaction.DynamicBean(dynamicField_AmountSorted);
    }

    public static long getSpecialTypeIdFromBean_9(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 5587247785002748608L)
            return 7L; // dynamicred.Demo.Amount
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:AmountSorted:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_9(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Amount();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> _QuantityGridSource;
    private static final Zeze.Transaction.DynamicField dynamicField_QuantityGridSource
            = Zeze.Transaction.DynamicField.declare(Order.class, TYPEID, 10, Order::getSpecialTypeIdFromBean_10, Order::createBeanFromSpecialTypeId_10, dynamicred.Demo.Quantity.class);
    private static final Zeze.Transaction.GTable.GTable2.Factory<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> factory_QuantityGridSource
            = Zeze.Transaction.GTable.GTable2.getFactory(Integer.class, Integer.class, dynamicField_QuantityGridSource);

    public static Zeze.Transaction.DynamicBean newDynamicBean_QuantityGridSource() {
        return new Zeze.Transaction.DynamicBean(dynamicField_QuantityGridSource);
    }

    public static long getSpecialTypeIdFromBean_10(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4144526925789372221L)
            return 7L; // dynamicred.Demo.Quantity
        throw new UnsupportedOperationException("Unknown Bean! dynamic@dynamicred.Demo.Order:QuantityGridSource:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_10(long _t_) {
        if (_t_ == 7L)
            return new dynamicred.Demo.Quantity();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> getAmounts() {
        return _Amounts;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getAmountsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Amounts);
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> getQuantities() {
        return _Quantities;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getQuantitiesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Quantities);
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> getLines() {
        return _Lines;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getLinesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Lines);
    }

    public Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getAmountGrid() {
        return _AmountGrid;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getAmountGridReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_AmountGrid.getPMap2());
    }

    public Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getQuantityGrid() {
        return _QuantityGrid;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getQuantityGridReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_QuantityGrid.getPMap2());
    }

    public Zeze.Transaction.DynamicBean getDirectAmount() {
        return _DirectAmount;
    }

    public dynamicred.Demo.Amount getDirectAmount_dynamicred_Demo_Amount() {
        return (dynamicred.Demo.Amount)_DirectAmount.getBean();
    }

    public void setDirectAmount(dynamicred.Demo.Amount _v_) {
        _DirectAmount.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDirectAmountReadOnly() {
        return _DirectAmount;
    }

    @Override
    public dynamicred.Demo.AmountReadOnly getDirectAmount_dynamicred_Demo_AmountReadOnly() {
        return (dynamicred.Demo.Amount)_DirectAmount.getBean();
    }

    public Zeze.Transaction.DynamicBean getDirectQuantity() {
        return _DirectQuantity;
    }

    public dynamicred.Demo.Quantity getDirectQuantity_dynamicred_Demo_Quantity() {
        return (dynamicred.Demo.Quantity)_DirectQuantity.getBean();
    }

    public void setDirectQuantity(dynamicred.Demo.Quantity _v_) {
        _DirectQuantity.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDirectQuantityReadOnly() {
        return _DirectQuantity;
    }

    @Override
    public dynamicred.Demo.QuantityReadOnly getDirectQuantity_dynamicred_Demo_QuantityReadOnly() {
        return (dynamicred.Demo.Quantity)_DirectQuantity.getBean();
    }

    public Zeze.Transaction.Collections.PMap2<Integer, Zeze.Transaction.DynamicBean> getQuantityMap() {
        return _QuantityMap;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getQuantityMapReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_QuantityMap);
    }

    public Zeze.Transaction.Collections.PSortedMap2<Integer, Zeze.Transaction.DynamicBean> getAmountSorted() {
        return _AmountSorted;
    }

    @Override
    public Zeze.Transaction.Collections.PSortedMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getAmountSortedReadOnly() {
        return new Zeze.Transaction.Collections.PSortedMap2ReadOnly<>(_AmountSorted);
    }

    public Zeze.Transaction.GTable.GTable2<Integer, Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getQuantityGridSource() {
        return _QuantityGridSource;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getQuantityGridSourceReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_QuantityGridSource.getPMap2());
    }

    @SuppressWarnings("deprecation")
    public Order() {
        _Amounts = new Zeze.Transaction.Collections.PList2<>(meta1_Amounts);
        _Amounts.variableId(1);
        _Quantities = new Zeze.Transaction.Collections.PList2<>(meta1_Quantities);
        _Quantities.variableId(2);
        _Lines = new Zeze.Transaction.Collections.PList2<>(meta1_Lines);
        _Lines.variableId(3);
        _AmountGrid = new Zeze.Transaction.GTable.GTable2<>(factory_AmountGrid);
        _AmountGrid.variableId(4);
        _QuantityGrid = new Zeze.Transaction.GTable.GTable2<>(factory_QuantityGrid);
        _QuantityGrid.variableId(5);
        _DirectAmount = newDynamicBean_DirectAmount();
        _DirectQuantity = newDynamicBean_DirectQuantity();
        _QuantityMap = new Zeze.Transaction.Collections.PMap2<>(meta2_QuantityMap);
        _QuantityMap.variableId(8);
        _AmountSorted = new Zeze.Transaction.Collections.PSortedMap2<>(meta2_AmountSorted);
        _AmountSorted.variableId(9);
        _QuantityGridSource = new Zeze.Transaction.GTable.GTable2<>(factory_QuantityGridSource);
        _QuantityGridSource.variableId(10);
    }

    @Override
    public void reset() {
        _Amounts.clear();
        _Quantities.clear();
        _Lines.clear();
        _AmountGrid.clear();
        _QuantityGrid.clear();
        _DirectAmount.reset();
        _DirectQuantity.reset();
        _QuantityMap.clear();
        _AmountSorted.clear();
        _QuantityGridSource.clear();
        _unknown_ = null;
    }

    public void assign(Order _o_) {
        _Amounts.clear();
        for (var _e_ : _o_._Amounts)
            _Amounts.add(_e_.copy());
        _Quantities.clear();
        for (var _e_ : _o_._Quantities)
            _Quantities.add(_e_.copy());
        _Lines.clear();
        for (var _e_ : _o_._Lines)
            _Lines.add(_e_.copy());
        _AmountGrid.assign(_o_._AmountGrid);
        _QuantityGrid.assign(_o_._QuantityGrid);
        _DirectAmount.assign(_o_._DirectAmount);
        _DirectQuantity.assign(_o_._DirectQuantity);
        _QuantityMap.clear();
        for (var _e_ : _o_._QuantityMap.entrySet())
            _QuantityMap.put(_e_.getKey(), _e_.getValue().copy());
        _AmountSorted.clear();
        for (var _e_ : _o_._AmountSorted.entrySet())
            _AmountSorted.put(_e_.getKey(), _e_.getValue().copy());
        _QuantityGridSource.assign(_o_._QuantityGridSource);
        _unknown_ = _o_._unknown_;
    }

    public Order copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public Order copy() {
        var _c_ = new Order();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(Order _a_, Order _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("dynamicred.Demo.Order: {\n");
        _s_.append(_i1_).append("Amounts=[");
        if (!_Amounts.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Amounts) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Amounts.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Quantities=[");
        if (!_Quantities.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Quantities) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Quantities.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Lines=[");
        if (!_Lines.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _Lines) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Lines.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("AmountGrid={");
        if (!_AmountGrid.isEmpty()) {
            _s_.append('\n');
            _AmountGrid.buildString(_s_, _l_ + 4);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("QuantityGrid={");
        if (!_QuantityGrid.isEmpty()) {
            _s_.append('\n');
            _QuantityGrid.buildString(_s_, _l_ + 4);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("DirectAmount=");
        _DirectAmount.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("DirectQuantity=");
        _DirectQuantity.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("QuantityMap={");
        if (!_QuantityMap.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _QuantityMap.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_QuantityMap.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("AmountSorted={");
        if (!_AmountSorted.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _AmountSorted.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_AmountSorted.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("QuantityGridSource={");
        if (!_QuantityGridSource.isEmpty()) {
            _s_.append('\n');
            _QuantityGridSource.buildString(_s_, _l_ + 4);
        }
        _s_.append("}\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            var _x_ = _Amounts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Quantities;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _Lines;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _AmountGrid;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _QuantityGrid;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _DirectAmount;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _DirectQuantity;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _QuantityMap;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _AmountSorted;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _QuantityGridSource;
            int _n_ = _x_.getPMap2().size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.getPMap2().entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _Amounts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                    var _e_ = Order.newDynamicBean_Amounts();
                    _o_.ReadDynamic(_e_, _t_);
                    _x_.add(_e_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Quantities;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                    var _e_ = Order.newDynamicBean_Quantities();
                    _o_.ReadDynamic(_e_, _t_);
                    _x_.add(_e_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Lines;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                    var _e_ = Order.newDynamicBean_Lines();
                    _o_.ReadDynamic(_e_, _t_);
                    _x_.add(_e_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _AmountGrid;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_AmountGrid.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _QuantityGrid;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_QuantityGrid.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _o_.ReadDynamic(_DirectAmount, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _o_.ReadDynamic(_DirectQuantity, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            var _x_ = _QuantityMap;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = Order.newDynamicBean_QuantityMap();
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            var _x_ = _AmountSorted;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = Order.newDynamicBean_AmountSorted();
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            var _x_ = _QuantityGridSource;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(_QuantityGridSource.getPMap2().createValue(), _t_);
                    _x_.getPMap2().put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof Order))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (Order)_o_;
        if (!_Amounts.equals(_b_._Amounts))
            return false;
        if (!_Quantities.equals(_b_._Quantities))
            return false;
        if (!_Lines.equals(_b_._Lines))
            return false;
        if (!_AmountGrid.equals(_b_._AmountGrid))
            return false;
        if (!_QuantityGrid.equals(_b_._QuantityGrid))
            return false;
        if (!_DirectAmount.equals(_b_._DirectAmount))
            return false;
        if (!_DirectQuantity.equals(_b_._DirectQuantity))
            return false;
        if (!_QuantityMap.equals(_b_._QuantityMap))
            return false;
        if (!_AmountSorted.equals(_b_._AmountSorted))
            return false;
        if (!_QuantityGridSource.equals(_b_._QuantityGridSource))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Amounts.initRootInfo(_r_, this);
        _Quantities.initRootInfo(_r_, this);
        _Lines.initRootInfo(_r_, this);
        _AmountGrid.initRootInfo(_r_, this);
        _QuantityGrid.initRootInfo(_r_, this);
        _DirectAmount.initRootInfo(_r_, this);
        _DirectQuantity.initRootInfo(_r_, this);
        _QuantityMap.initRootInfo(_r_, this);
        _AmountSorted.initRootInfo(_r_, this);
        _QuantityGridSource.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Amounts.initRootInfoWithRedo(_r_, this);
        _Quantities.initRootInfoWithRedo(_r_, this);
        _Lines.initRootInfoWithRedo(_r_, this);
        _AmountGrid.initRootInfoWithRedo(_r_, this);
        _QuantityGrid.initRootInfoWithRedo(_r_, this);
        _DirectAmount.initRootInfoWithRedo(_r_, this);
        _DirectQuantity.initRootInfoWithRedo(_r_, this);
        _QuantityMap.initRootInfoWithRedo(_r_, this);
        _AmountSorted.initRootInfoWithRedo(_r_, this);
        _QuantityGridSource.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Amounts) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _Quantities) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _Lines) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _AmountGrid.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
        }
        for (var _v_ : _QuantityGrid.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
        }
        if (_DirectAmount.negativeCheck())
            return true;
        if (_DirectQuantity.negativeCheck())
            return true;
        for (var _v_ : _QuantityMap.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _AmountSorted.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _QuantityGridSource.getPMap2().values()) {
            if (_v_.negativeCheck()) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Amounts.followerApply(_v_); break;
                case 2: _Quantities.followerApply(_v_); break;
                case 3: _Lines.followerApply(_v_); break;
                case 4: _AmountGrid.getPMap2().followerApply(_v_); break;
                case 5: _QuantityGrid.getPMap2().followerApply(_v_); break;
                case 6: _DirectAmount.followerApply(_v_); break;
                case 7: _DirectQuantity.followerApply(_v_); break;
                case 8: _QuantityMap.followerApply(_v_); break;
                case 9: _AmountSorted.followerApply(_v_); break;
                case 10: _QuantityGridSource.getPMap2().followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Amounts, Zeze.Transaction.DynamicBean.class, _r_.getString(_pn_ + "Amounts"));
        Zeze.Serialize.Helper.decodeJsonList(_Quantities, Zeze.Transaction.DynamicBean.class, _r_.getString(_pn_ + "Quantities"));
        Zeze.Serialize.Helper.decodeJsonList(_Lines, Zeze.Transaction.DynamicBean.class, _r_.getString(_pn_ + "Lines"));
        Zeze.Serialize.Helper.decodeJsonGTable2(this, "AmountGrid", _AmountGrid, _r_.getString(_pn_ + "AmountGrid"));
        Zeze.Serialize.Helper.decodeJsonGTable2(this, "QuantityGrid", _QuantityGrid, _r_.getString(_pn_ + "QuantityGrid"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_DirectAmount, _r_.getString(_pn_ + "DirectAmount"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_DirectQuantity, _r_.getString(_pn_ + "DirectQuantity"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "QuantityMap", _QuantityMap, _r_.getString(_pn_ + "QuantityMap"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "AmountSorted", _AmountSorted, _r_.getString(_pn_ + "AmountSorted"));
        Zeze.Serialize.Helper.decodeJsonGTable2(this, "QuantityGridSource", _QuantityGridSource, _r_.getString(_pn_ + "QuantityGridSource"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Amounts", Zeze.Serialize.Helper.encodeJson(_Amounts));
        _s_.appendString(_pn_ + "Quantities", Zeze.Serialize.Helper.encodeJson(_Quantities));
        _s_.appendString(_pn_ + "Lines", Zeze.Serialize.Helper.encodeJson(_Lines));
        _s_.appendString(_pn_ + "AmountGrid", Zeze.Serialize.Helper.encodeJson(_AmountGrid));
        _s_.appendString(_pn_ + "QuantityGrid", Zeze.Serialize.Helper.encodeJson(_QuantityGrid));
        _s_.appendString(_pn_ + "DirectAmount", Zeze.Serialize.Helper.encodeJson(_DirectAmount));
        _s_.appendString(_pn_ + "DirectQuantity", Zeze.Serialize.Helper.encodeJson(_DirectQuantity));
        _s_.appendString(_pn_ + "QuantityMap", Zeze.Serialize.Helper.encodeJson(_QuantityMap));
        _s_.appendString(_pn_ + "AmountSorted", Zeze.Serialize.Helper.encodeJson(_AmountSorted));
        _s_.appendString(_pn_ + "QuantityGridSource", Zeze.Serialize.Helper.encodeJson(_QuantityGridSource));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Amounts", "list", "", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Quantities", "list", "", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Lines", "list", "", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "AmountGrid", "gtable", "int,int", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "QuantityGrid", "gtable", "int,int", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "DirectAmount", "dynamic", "", "dynamicred.Demo.Amount"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "DirectQuantity", "dynamic", "", "dynamicred.Demo.Quantity"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "QuantityMap", "map", "int", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "AmountSorted", "sortedmap", "int", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(10, "QuantityGridSource", "gtable", "int,int", "dynamic"));
        return _v_;
    }
}

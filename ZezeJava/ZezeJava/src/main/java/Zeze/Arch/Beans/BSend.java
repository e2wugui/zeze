package Zeze.Arch.Beans;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongList;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;

public class BSend implements Serializable {
	public static final long TYPEID = 545774009128015305L;

	private final LongList _linkSids = new LongList();
	private long _protocolType;
	private @NotNull Binary _protocolWholeData; // 完整的协议打包，包括了 type, size

	public @NotNull LongList getLinkSids() {
		return _linkSids;
	}

	public long getProtocolType() {
		return _protocolType;
	}

	public void setProtocolType(long value) {
		_protocolType = value;
	}

	public @NotNull Binary getProtocolWholeData() {
		return _protocolWholeData;
	}

	public void setProtocolWholeData(@NotNull Binary value) {
		//noinspection ConstantValue
		if (value == null)
			throw new IllegalArgumentException();
		_protocolWholeData = value;
	}

	public BSend() {
		_protocolWholeData = Binary.Empty;
	}

	public BSend(long _protocolType_, @NotNull Binary _protocolWholeData_) {
		_protocolType = _protocolType_;
		//noinspection ConstantValue
		if (_protocolWholeData_ == null)
			throw new IllegalArgumentException();
		_protocolWholeData = _protocolWholeData_;
	}

	@Override
	public long typeId() {
		return TYPEID;
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		buildString(sb, 0);
		return sb.append(System.lineSeparator()).toString();
	}

	public void buildString(@NotNull StringBuilder sb, int level) {
		sb.append(Str.indent(level)).append("Zeze.Arch.Beans.BSend: {").append(System.lineSeparator());
		level += 4;
		sb.append(Str.indent(level)).append("linkSids=[");
		if (!_linkSids.isEmpty()) {
			sb.append(System.lineSeparator());
			level += 4;
			for (int i = 0, n = _linkSids.size(); i < n; i++) {
				sb.append(Str.indent(level)).append("Item=").append(_linkSids.get(i)).append(',').append(System.lineSeparator());
			}
			level -= 4;
			sb.append(Str.indent(level));
		}
		sb.append(']').append(',').append(System.lineSeparator());
		sb.append(Str.indent(level)).append("protocolType=").append(getProtocolType()).append(',').append(System.lineSeparator());
		sb.append(Str.indent(level)).append("protocolWholeData=").append(_protocolWholeData).append(System.lineSeparator());
		level -= 4;
		sb.append(Str.indent(level)).append('}');
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void encode(@NotNull ByteBuffer _o_) {
		int _i_ = 0;
		{
			var _x_ = _linkSids;
			int _n_ = _x_.size();
			if (_n_ != 0) {
				_i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
				_o_.WriteListType(_n_, ByteBuffer.INTEGER);
				for (int _j_ = 0; _j_ < _n_; _j_++)
					_o_.WriteLong(_x_.get(_j_));
			}
		}
		{
			long _x_ = _protocolType;
			if (_x_ != 0) {
				_i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
				_o_.WriteLong(_x_);
			}
		}
		{
			var _x_ = _protocolWholeData;
			if (_x_.size() != 0) {
				_i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
				_o_.WriteBinary(_x_);
			}
		}
		_o_.WriteByte(0);
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void decode(@NotNull IByteBuffer _o_) {
		int _t_ = _o_.ReadByte();
		int _i_ = _o_.ReadTagSize(_t_);
		if (_i_ == 1) {
			var _x_ = _linkSids;
			_x_.clear();
			if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
				int n = _o_.ReadTagSize(_t_ = _o_.ReadByte());
				if (_x_.capacity() < n) {
					_x_.wraps(new long[Math.min(n, 0x10000)]);
					_x_.clear();
				}
				for (; n > 0; n--)
					_x_.add(_o_.ReadLong(_t_));
			} else
				_o_.SkipUnknownFieldOrThrow(_t_, "Collection");
			_i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
		if (_i_ == 2) {
			_protocolType = _o_.ReadLong(_t_);
			_i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
		if (_i_ == 3) {
			_protocolWholeData = _o_.ReadBinary(_t_);
			_i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
		while (_t_ != 0) {
			_o_.SkipUnknownField(_t_);
			_o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
	}
}

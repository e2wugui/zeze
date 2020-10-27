#pragma once

namespace limax {

	class Codec {
	public:
		virtual void update(int8_t c) = 0;
		virtual void update(int8_t data[], int32_t off, int32_t len) = 0;
		virtual void flush() = 0;
		virtual ~Codec();
		static std::shared_ptr<Codec> Null();
	};

	class CodecException {
	};

	class BufferedSink : public Codec {
		std::shared_ptr<Codec> sink;
		static const int32_t capacity = 8192;
		int8_t buffer[capacity];
		int32_t pos;
		void flushInternal();
	public:
		BufferedSink(std::shared_ptr<Codec> _sink);
		virtual void update(int8_t c) override;
		virtual void update(int8_t data[], int32_t off, int32_t len) override;
		virtual void flush() override;
	};

	class SinkStream : public Codec {
		std::shared_ptr<std::ostream> os;
	public:
		SinkStream(std::shared_ptr<std::ostream> os);
		virtual void update(int8_t c) override;
		virtual void update(int8_t data[], int32_t off, int32_t len) override;
		virtual void flush() override;
	};

	class SinkOctets : public Codec	{
		Octets& o;
	public:
		SinkOctets(Octets &_o);
		virtual void update(int8_t c) override;
		virtual void update(int8_t data[], int32_t off, int32_t len) override;
		virtual void flush() override;
	};

	class Base64Encode : public Codec
	{
		static int8_t *ENCODE;
		static const int8_t B64PAD = '=';
		std::shared_ptr<Codec> sink;
		int8_t b0;
		int8_t b1;
		int8_t b2;
		int32_t n;
		void update0(int8_t r[], int32_t j, int8_t data[], int32_t off, int32_t len);
		void update1(int8_t r[], int8_t data[], int32_t off, int32_t len);
		void update2(int8_t r[], int8_t data[], int32_t off, int32_t len);
		void update(int8_t r[], int8_t data[], int32_t off, int32_t len);
	public:
		Base64Encode(std::shared_ptr<Codec> _sink);
		virtual void update(int8_t c) override;
		virtual void update(int8_t data[], int32_t off, int32_t len) override;
		virtual void flush() override;
		static Octets transform(Octets data);
	};

	class Base64Decode : public Codec
	{
		static int8_t DECODE[];
		std::shared_ptr<Codec> sink;
		int8_t b0;
		int8_t b1;
		int8_t b2;
		int8_t b3;
		int32_t n;
		int32_t update0(int8_t r[], int32_t j, int8_t data[], int32_t off, int32_t len);
		int32_t update1(int8_t r[], int8_t data[], int32_t off, int32_t len);
		int32_t update2(int8_t r[], int8_t data[], int32_t off, int32_t len);
		int32_t update3(int8_t r[], int8_t data[], int32_t off, int32_t len);
		int32_t update(int8_t r[], int8_t data[], int32_t off, int32_t len);
	public:
		Base64Decode(std::shared_ptr<Codec> _sink);
		virtual void update(int8_t c) override;
		virtual void update(int8_t data[], int32_t off, int32_t len) override;
		virtual void flush() override;
		static Octets transform(Octets data);
	};

} // namespace limax {


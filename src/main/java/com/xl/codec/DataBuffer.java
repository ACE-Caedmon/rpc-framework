package com.xl.codec;

import com.google.protobuf.AbstractMessage.Builder;
import io.netty.buffer.ByteBuf;

/**
 * @author Chenlong
 * 缓冲区包装装饰器类，提供在Bytebuf之上更常用的接口
 * */
public interface DataBuffer {

	byte readByte();
	boolean readBoolean();
	short readShort();
	int readInt();
	float readFloat();
	long readLong();
	double readDouble();
	String readString();
	void readBytes(byte[] dst);
	Builder readProtoBuf(Builder builder);
	<T> T readJSON(Class<T> clazz);
	<T> T readJSON();
	DataBuffer readBinary(int length);
	void writeByte(byte b);
	void writeBoolean(boolean b);
	void writeShort(short s);
	void writeInt(int i);
	void writeFloat(float f);
	void writeLong(long l);
	void writeDouble(double d);
	void writeString(String s);
	void writeProtoBuf(Builder<?> builder);
	void writeJSON(Object bean);
	void writeBytes(DataBuffer buffer);
	ByteBuf getByteBuf();
	void writeBytes(byte[] bytes);

}

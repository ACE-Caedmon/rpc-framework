package com.xiaoluo.rpc.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author Caedmon
 * ByteBuf 封装类，提供基本对象读取写入缓冲流的方法
 * */
public class DefaultPracticalBuffer implements PracticalBuffer {
	private ByteBuf buf;
	private Logger logger=LoggerFactory.getLogger(DefaultPracticalBuffer.class);
	private static final SerializerFeature[] FEATURES=new SerializerFeature[]{SerializerFeature.WriteClassName};
	public DefaultPracticalBuffer(ByteBuf buf){
		this.buf=buf;
	}
	@Override
	public byte readByte() {
		// TODO Auto-generated method stub
		return buf.readByte();
	}

	@Override
	public boolean readBoolean() {
		// TODO Auto-generated method stub
		return buf.readBoolean();
	}

	@Override
	public short readShort() {
		// TODO Auto-generated method stub
		return buf.readShort();
	}

	@Override
	public int readInt() {
		// TODO Auto-generated method stub
		return buf.readInt();
	}

	@Override
	public float readFloat() {
		// TODO Auto-generated method stub
		return buf.readFloat();
	}

	@Override
	public long readLong() {
		// TODO Auto-generated method stub
		return buf.readLong();
	}

	@Override
	public double readDouble() {
		// TODO Auto-generated method stub
		return buf.readDouble();
	}

	@Override
	public String readString() {
		int length=buf.readInt();
		byte[] content=buf.readBytes(length).array();
		return new String(content,Charset.forName("UTF-8"));
	}

	@Override
	public Message.Builder readProtoBuf(Message.Builder builder) {
		int length=buf.readInt();
		byte[] dst=new byte[length];
		buf.readBytes(dst);
		try {
			builder.mergeFrom(dst);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		builder.build();
		return builder;
	}

	@Override
	public void writeByte(byte b) {
		buf.writeByte(b);
		
	}

	@Override
	public void writeBoolean(boolean b) {
		buf.writeBoolean(b);
		
	}

	@Override
	public void writeShort(short s) {
		buf.writeShort(s);
		
	}

	@Override
	public void writeInt(int i) {
		buf.writeInt(i);
		
	}

	@Override
	public void writeFloat(float f) {
		buf.writeFloat(f);
		
	}

	@Override
	public void writeLong(long l) {
		buf.writeLong(l);
		
	}

	@Override
	public void writeDouble(double d) {
		buf.writeDouble(d);
		
	}

	@Override
	public void writeString(String s) {
		// TODO Auto-generated method stub
		byte[] arr=s.getBytes(Charset.forName("UTF-8"));
		int length=arr.length;
		buf.writeInt(length);
		buf.writeBytes(arr);
	}

	@Override
	public void writeProtoBuf(Message.Builder builder) {
		byte[] dst=builder.build().toByteArray();
		buf.writeInt(dst.length);
		buf.writeBytes(dst);
	}

	@Override
	public Object readJSON(Class clazz) {
		String json=readString();
		return JSON.parseObject(json, clazz);
	}

	@Override
	public void writeJSON(Object bean) {
		String json= JSON.toJSONString(bean, FEATURES);
		writeString(json);
	}

	@Override
	public void writeBytes(PracticalBuffer buffer) {
		buf.writeBytes(buffer.getByteBuf());
	}

	@Override
	public ByteBuf getByteBuf() {
		return buf;
	}

	@Override
	public PracticalBuffer readBinary(int length) {
		ByteBuf byteBuf=buf.readBytes(length);
		return new DefaultPracticalBuffer(byteBuf);
	}

	@Override
	public void readBytes(byte[] dst) {
		buf.readBytes(dst);
	}

	@Override
	public void writeBytes(byte[] src) {
		buf.writeBytes(src);
	}
}

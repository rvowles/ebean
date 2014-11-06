package com.avaje.ebeaninternal.server.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * Base type for DateTime types.
 */
public abstract class ScalarTypeBaseDateTime<T> extends ScalarTypeBase<T> {

    public ScalarTypeBaseDateTime(Class<T> type, boolean jdbcNative, int jdbcType) {
        super(type, jdbcNative, jdbcType);
    }
    
    public abstract long convertToMillis(Object value);
    
    public abstract Timestamp convertToTimestamp(T t);
    
    public abstract T convertFromTimestamp(Timestamp ts);
    
    public void bind(DataBind b, T value) throws SQLException {
        if (value == null){
            b.setNull(Types.TIMESTAMP);
        } else {
            Timestamp ts = convertToTimestamp(value);
            b.setTimestamp(ts);
        }
    }

    public T read(DataReader dataReader) throws SQLException {
        
        Timestamp ts = dataReader.getTimestamp();
        if (ts == null){
            return null;
        } else {
            return convertFromTimestamp(ts);
        }
    }
    
    @Override
    public Object jsonRead(JsonParser ctx, Event event) {
      if (ctx.isIntegralNumber()) {
        long millis = ctx.getLong();
        return parseDateTime(millis);
      } else {
        String string = ctx.getString();
        throw new RuntimeException("convert "+string);
      }
    }
    
    @Override
    public void jsonWrite(JsonGenerator ctx, String name, Object value) {
      long millis = convertToMillis(value);
      ctx.write(name, millis);
    }

    public String formatValue(T t) {
        Timestamp ts = convertToTimestamp(t);
        return ts.toString();
    }

    public T parse(String value) {
        Timestamp ts = Timestamp.valueOf(value);
        return convertFromTimestamp(ts);
    }
    
    public T parseDateTime(long systemTimeMillis) {
        Timestamp ts = new Timestamp(systemTimeMillis);
        return convertFromTimestamp(ts);
    }

    public boolean isDateTimeCapable() {
        return true;
    }

    public Object readData(DataInput dataInput) throws IOException {
        if (!dataInput.readBoolean()) {
            return null;
        } else {
            long val = dataInput.readLong();
            Timestamp ts = new Timestamp(val);
            return convertFromTimestamp(ts);
        }
    }

    @SuppressWarnings("unchecked")
    public void writeData(DataOutput dataOutput, Object v) throws IOException {
        
        T value = (T)v;
        if (value == null){
            dataOutput.writeBoolean(false);
        } else {
            dataOutput.writeBoolean(true);
            Timestamp ts = convertToTimestamp(value);
            dataOutput.writeLong(ts.getTime());            
        }
    }
    
}

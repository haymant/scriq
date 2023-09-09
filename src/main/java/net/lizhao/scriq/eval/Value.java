package net.lizhao.scriq.eval;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class Value {
    public static Value VOID = new Value(new Object());

    final Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Boolean asBoolean() {
        return (Boolean)value;
    }

    public BigDecimal asBigDecimal() {
        return (BigDecimal)value;
    }

    public String asString() {
        return String.valueOf(value);
    }

    public CompletableFuture<Object> asFuture() { return (CompletableFuture<Object>)value; }

    public boolean isBigDecimal() {
        return value instanceof BigDecimal;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isFuture() { return value instanceof CompletableFuture; }

    @Override
    public int hashCode() {

        if(value == null) {
            return 0;
        }

        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if(value == o) {
            return true;
        }

        if (this.isBigDecimal()) {
            if (o instanceof Integer
                    || o instanceof Long
                    || o instanceof Short
                    || o instanceof Byte) {
                return this.value.equals(BigDecimal.valueOf(((Number) o).longValue()));
            } else if (o instanceof BigDecimal) {
                return this.value.equals(o);
            } else if (o instanceof String) {
                return this.value.equals(new BigDecimal((String) o));
            } else if (o instanceof  Double){
                return this.value.equals(BigDecimal.valueOf(((Number) o).doubleValue()));
            } else {
                return this.value.equals(((Value)o).value);
            }
        }

        if (this.isString()) {
            return this.value.equals(o);
        }

        if(value == null || o == null || o.getClass() != this.getClass()) {
            return false;
        }

        return this.value.equals(((Value)o).value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

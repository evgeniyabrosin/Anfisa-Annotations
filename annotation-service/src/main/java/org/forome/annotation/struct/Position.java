package org.forome.annotation.struct;

import java.util.Objects;

public class Position<T> {

    public final T start;
    public final T end;

    public Position(T position) {
        this(position, position);
    }

    public Position(T start, T end) {
        if (start == null) throw new IllegalArgumentException();
        if (end == null) throw new IllegalArgumentException();

        this.start = start;
        this.end = end;
    }

    public boolean isSingle() {
        return Objects.equals(start, end);
    }

    @Override
    public String toString() {
        StringBuilder sBuilder = new StringBuilder()
                .append("Position(");
        if (isSingle()) {
            sBuilder.append(start);
        } else {
            sBuilder.append("start: ").append(start);
            sBuilder.append(", end: ").append(end);
        }
        sBuilder.append(')');
        return sBuilder.toString();
    }
}

package org.forome.annotation.struct;

public class Position {

    public final long start;
    public final long end;

    public Position(long position) {
        this.start = this.end = position;
    }

    public Position(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public boolean isSingle() {
        return (start == end);
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

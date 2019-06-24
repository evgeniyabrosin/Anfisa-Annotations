package org.forome.annotation.struct;

public class Position {

    public final Long start;
    public final Long end;

    public Position(Long position) {
        this.start = this.end = position;
    }

    public Position(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public Position(Integer start, Integer end) {
        this.start = (start != null) ? start.longValue() : null;
        this.end = (end != null) ? end.longValue() : null;
    }

}

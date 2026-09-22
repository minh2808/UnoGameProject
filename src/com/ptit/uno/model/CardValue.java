package com.ptit.uno.model;

import java.io.Serializable;

public enum CardValue implements Serializable {
    ZERO(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    SKIP(20),
    REVERSE(20),
    DRAW_TWO(20),
    WILD(50),
    WILD_DRAW_FOUR(50);

    private final int point;

    CardValue(int point) {
        this.point = point;
    }

    public int getPoint() {
        return point;
    }
}

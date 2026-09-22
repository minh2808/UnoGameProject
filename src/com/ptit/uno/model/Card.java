package com.ptit.uno.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Thực thể Card đại diện cho 1 lá bài UNO.
 * Tuân thủ chuẩn JavaBean (thuộc tính private, constructor rỗng, getter/setter, Serializable).
 */
public class Card implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private CardColor color;
    private CardValue value;

    public Card() {
    }

    public Card(String id, CardColor color, CardValue value) {
        this.id = id;
        this.color = color;
        this.value = value;
    }

    public Card(CardColor color, CardValue value) {
        this(color.name() + "_" + value.name(), color, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CardColor getColor() {
        return color;
    }

    public void setColor(CardColor color) {
        this.color = color;
    }

    public CardValue getValue() {
        return value;
    }

    public void setValue(CardValue value) {
        this.value = value;
    }

    public boolean isWild() {
        return color == CardColor.WILD || value == CardValue.WILD || value == CardValue.WILD_DRAW_FOUR;
    }

    public boolean isActionCard() {
        return value == CardValue.SKIP || value == CardValue.REVERSE || value == CardValue.DRAW_TWO;
    }

    public int getPoint() {
        return value != null ? value.getPoint() : 0;
    }

    /**
     * Kiểm tra xem lá bài này có đánh đè lên lá topCard được không.
     */
    public boolean canPlayOn(Card topCard, CardColor activeColor) {
        if (this.isWild()) {
            return true;
        }
        if (activeColor != null && this.color == activeColor) {
            return true;
        }
        if (topCard != null) {
            if (this.color == topCard.getColor()) {
                return true;
            }
            if (this.value == topCard.getValue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "[" + color + " " + value + "]";
    }
}

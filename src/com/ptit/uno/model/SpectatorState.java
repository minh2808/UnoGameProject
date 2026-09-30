package com.ptit.uno.model;

import java.io.Serializable;
import java.util.List;

public class SpectatorState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private GameState gameState;
    private List<Card> hostHand;
    private List<Card> guestHand;
    
    public SpectatorState(GameState gameState, List<Card> hostHand, List<Card> guestHand) {
        this.gameState = gameState;
        this.hostHand = hostHand;
        this.guestHand = guestHand;
    }

    public GameState getGameState() {
        return gameState;
    }

    public List<Card> getHostHand() {
        return hostHand;
    }

    public List<Card> getGuestHand() {
        return guestHand;
    }
}

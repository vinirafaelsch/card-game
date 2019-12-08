package server;

import java.util.ArrayList;

/**
 *
 * @author vinirafaelsch
 */
public class Jogo {
    
    Jogador player1;
    Jogador player2;
    ArrayList<Card> deck1;
    ArrayList<Card> deck2;
    
    public Jogo (Jogador player1, Jogador player2) {
        this.player1 = player1;
        this.player2 = player2;
        deck1 = new ArrayList<>();
        deck2 = new ArrayList<>();
        
        for(int i = 0; i < 10; i++) {
            deck1.add(Card.getRandomCard());
            deck2.add(Card.getRandomCard());
        }
    }
}

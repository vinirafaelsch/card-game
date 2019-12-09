package server;

import java.util.ArrayList;

/**
 * @author vinirafaelsch
 */
public class Jogo {

    Jogador player1;
    Jogador player2;

    public Jogo(Jogador player1, Jogador player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1.deck = new ArrayList<>();
        this.player2.deck = new ArrayList<>();

        this.player1.deck.add(Card.ITACHI);
        this.player1.deck.add(Card.KAKASHI);

        this.player2.deck.add(Card.NEJI);
        this.player2.deck.add(Card.SASUKE);

//        for(int i = 0; i < 10; i++) {
//            this.player1.deck.add(Card.getRandomCard());
//            this.player2.deck.add(Card.getRandomCard());
//        }
    }
}

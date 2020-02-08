package game;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Tuple2<T1, T2>  {
    final T1 v1; final T2 v2;
    Tuple2(T1 v1, T2 v2) {
        this.v1 = v1; this.v2 = v2;
    }
}

class Tuple3<T1, T2, T3>  {
    final T1 v1; final T2 v2; final T3 v3;
    Tuple3(T1 v1, T2 v2, T3 v3) {
        this.v1 = v1; this.v2 = v2; this.v3 = v3;
    }
}

class Tuple4<T1, T2, T3, T4>  {
    final T1 v1; final T2 v2; final T3 v3; final T4 v4;
    Tuple4(T1 v1, T2 v2, T3 v3, T4 v4) {
        this.v1 = v1; this.v2 = v2; this.v3 = v3; this.v4 = v4;
    }
}

enum Suit {
    Heart,
    Diamond,
    Spade,
    Club
}

enum Rank {
    King(10),
    Queen(10),
    Jack(10),
    Ten(10),
    Nine(9),
    Eight(8),
    Seven(7),
    Six(6),
    Five(5),
    Four(4),
    Three(3),
    Two(2),
    Ace(1);

    final Integer value;

    Rank(Integer value) {
        this.value = value;
    }

    boolean isAce() {
        return this == Ace;
    }
}

class Card {
    final Rank rank;
    final Suit suit;

    Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }
}

class Deck {
    final List<Card> cards;

    Deck(List<Card> cards) {
        this.cards = cards;
    }

    static List<Card> allCards() {
        List<Card> cards = new ArrayList<>();
        for(Suit suit: Suit.values()) {
            for(Rank rank: Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        return cards;
    }

    static Deck shuffle(Function<List<Card>, List<Card>> shuffleCards) {
        return new Deck(shuffleCards.apply(allCards()));
    }

    Tuple2<Card, Deck> dealCard() {
        Card topCard = cards.get(0);
        return new Tuple2<>(topCard, new Deck(cards.subList(1, cards.size())));
    }
}

class Hand {
    static final int winningValue = 21;
    final List<Card> cards;
    final int value;
    final boolean containsAce;
    final int specialValue;
    final boolean isBlackJack;
    final boolean isBust;
    final int bestValue;

    Hand(List<Card> cards) {
        this.cards = cards;
        this.value = cards.stream().map(card -> card.rank.value).reduce(Integer::sum).orElse(0);
        this.containsAce = cards.stream().anyMatch(card -> card.rank.isAce());
        this.specialValue = containsAce ? value + 10 : value;
        this.isBlackJack = value == winningValue || specialValue == winningValue;
        this.isBust = value > winningValue;
        this.bestValue = Stream.of(value, specialValue).filter(value -> value <= winningValue).reduce(Integer::max).orElse(0);
    }

    boolean winsOver(Hand other) {
        return bestValue > other.bestValue;
    }

    String showCards(boolean dealer) {
        if (dealer) {
            return cards.get(0).rank.value + " X";
        } else {
            return cards.stream().map(card -> card.rank.value.toString()).collect(Collectors.joining(", "));
        }
    }

    Hand addCard(Card card) {
        return new Hand(Stream.concat(cards.stream(), Stream.of(card)).collect(Collectors.toList()));
    }
}

class Dealer {
    static Tuple3<Hand, Hand, Deck> dealHands(Deck deck) {
        Tuple2<Card, Deck> first = deck.dealCard();
        Tuple2<Card, Deck> second = first.v2.dealCard();
        Tuple2<Card, Deck> third = second.v2.dealCard();
        Tuple2<Card, Deck> fourth = third.v2.dealCard();
        return new Tuple3<>(new Hand(Arrays.asList(first.v1, second.v1)), new Hand(Arrays.asList(third.v1, fourth.v1)), fourth.v2);
    }
}

class GameState {
    final Integer credit;

    GameState(int credit) {
        this.credit = credit;
    }
}

public class Game {

    public static void main(String[] args) {
        Function<List<Card>, List<Card>> randomShuffler = (list) -> {
            List<Card> shuffled = Arrays.stream(list.toArray(new Card[0])).collect(Collectors.toList());
            Collections.shuffle(shuffled);
            return shuffled;
        };
        Supplier<Boolean> shouldContinue = () -> {
            System.out.println("Do you want to continue?");
            return "y".equalsIgnoreCase(new Scanner(System.in).nextLine());
        };
        Supplier<Boolean> shouldStand = () -> {
            System.out.println("Hit or Stand?");
            return "s".equalsIgnoreCase(new Scanner(System.in).nextLine());
        };
        Function<Integer, Integer> betSupplier = (currentCredit) -> {
            System.out.println("Please enter bet (credit: " + currentCredit + ") ");
            int newBet = new Scanner(System.in).nextInt();
            while(newBet > currentCredit) {
                System.out.println("Too high. Please enter bet (credit: " + currentCredit + ") ");
                newBet = new Scanner(System.in).nextInt();
            }
            return newBet;
        };
        GameState gameState = new GameState(100);
        gameLoop(gameState, randomShuffler, betSupplier, shouldContinue, shouldStand);
    }

    static void gameLoop(GameState gameState, Function<List<Card>, List<Card>> shuffleFn, Function<Integer, Integer> betSupplier, Supplier<Boolean> shouldContinue, Supplier<Boolean> shouldStand) {
        Deck deck = Deck.shuffle(shuffleFn);
        int bet = betSupplier.apply(gameState.credit);
        Tuple3<Hand, Hand, Deck> hands = Dealer.dealHands(deck);
        boolean playerWon = roundLoop(hands.v1, hands.v2, hands.v3, false, shouldStand);
        GameState newState = new GameState(gameState.credit + (playerWon ? bet : -bet));
        if (newState.credit > 0 && shouldContinue.get()) {
            gameLoop(newState, shuffleFn, betSupplier, shouldContinue, shouldStand);
        } else if (newState.credit <= 0) {
            System.out.println("You have no money left");
        } else {
            System.out.println("Exiting");
        }
    }

    static boolean roundLoop(Hand playerHand, Hand dealerHand, Deck deck, boolean stand, Supplier<Boolean> shouldStand) {
        if (playerHand.isBust) {
            return summary(playerHand, dealerHand, false);
        } else if(stand) {
            return summary(playerHand, dealerHand,dealerHand.isBust || playerHand.winsOver(dealerHand));
        } else {
            Tuple4<Hand, Hand, Deck, Boolean> round = hitOrStand(playerHand, dealerHand, deck, shouldStand);
            return roundLoop(round.v1, round.v2, round.v3, round.v4, shouldStand);
        }
    }

    static Tuple4<Hand, Hand, Deck, Boolean> hitOrStand(Hand playerHand, Hand dealerHand, Deck deck, Supplier<Boolean> shouldStand) {
        showCards(playerHand, dealerHand, true);
        boolean stand = shouldStand.get();
        if (stand) {
            while(dealerHand.value < 17) {
                Tuple2<Card, Deck> dealt = deck.dealCard();
                dealerHand = dealerHand.addCard(dealt.v1);
                deck = dealt.v2;
            }
        } else {
            Tuple2<Card, Deck> dealt = deck.dealCard();
            playerHand = playerHand.addCard(dealt.v1);
            deck = dealt.v2;
        }
        return new Tuple4<>(playerHand, dealerHand, deck, stand);
    }

    static void showCards(Hand playerHand, Hand dealerHand, boolean showDealer) {
        System.out.println("Dealer hand: " + dealerHand.showCards(showDealer));
        System.out.println("Player hand: " + playerHand.showCards(false));
    }

    static boolean summary(Hand playerHand, Hand dealerHand, boolean won) {
        System.out.println("*** You " + (won ? "win" : "loose!") + " ***");
        showCards(playerHand, dealerHand, false);
        return won;
    }
}

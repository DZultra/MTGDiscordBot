package net.dzultra.MTGDiscordBot;

import java.util.List;

public class MTGCard {
    public String name;
    public int count;
    public String color;
    public String folder;
    public int page;
    public int attack;
    public int defense;
    public String type;
    public List<String> cardClass;
    public int neutral_cost;
    public int green_cost;
    public int red_cost;
    public int white_cost;
    public int black_cost;
    public int blue_cost;
    public int loyalty;

    public MTGCard() {}

    public MTGCard(String name, int count, String color, String folder, int page,
                   Integer attack, Integer defense, String type, List<String> cardClass,
                   int neutral_cost, int green_cost, int red_cost, int white_cost, int black_cost, int blue_cost,
                   Integer loyalty) {
        this.name = name;
        this.count = count;
        this.color = color;
        this.folder = folder;
        this.page = page;
        this.type = type;
        this.neutral_cost = neutral_cost;
        this.green_cost = green_cost;
        this.red_cost = red_cost;
        this.white_cost = white_cost;
        this.black_cost = black_cost;
        this.blue_cost = blue_cost;

        if (type != null && type.toLowerCase().contains("kreatur")) {
            if (attack == null || defense == null) {
                throw new IllegalArgumentException("Creature cards must have attack and defense!");
            }
            this.attack = attack;
            this.defense = defense;
            this.cardClass = cardClass;
            this.loyalty = -1;
        } else if (type != null && type.equalsIgnoreCase("Planeswalker")) {
            if (loyalty == null) {
                throw new IllegalArgumentException("Planeswalker cards must have loyalty!");
            }
            this.attack = -1;
            this.defense = -1;
            this.cardClass = List.of();
            this.loyalty = loyalty;
        } else {
            this.attack = -1;
            this.defense = -1;
            this.cardClass = List.of();
            this.loyalty = -1;
        }
    }
}

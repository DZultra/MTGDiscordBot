package net.dzultra.MTGDiscordBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import net.dzultra.MTGDiscordBot.MTGCard;
import net.dzultra.MTGDiscordBot.MTGCardDatabase;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;

public class GetCardCommand {
    public static Mono<Void> getCardCommand(ChatInputInteractionEvent event) {
        MTGCard card;
        String card_name = event.getOptionAsString("name").get();

        try {
            card = MTGCardDatabase.getCard(card_name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (card == null) {
            return event.reply("No card with this name has been found!\nPlease try a different name.")
                    .withEphemeral(true);
        } else {
            String iconURL = "";
            if (card.color.equalsIgnoreCase("green")) {
                iconURL = "https://i.imgur.com/qmg2p1y.png";
            } else if (card.color.equalsIgnoreCase("red")) {
                iconURL = "https://i.imgur.com/scLqjsL.png";
            } else if (card.color.equalsIgnoreCase("white")) {
                iconURL = "https://i.imgur.com/rpfiHz0.png";
            } else if (card.color.equalsIgnoreCase("black")) {
                iconURL = "https://i.imgur.com/xxBLewo.png";
            } else if (card.color.equalsIgnoreCase("blue")) {
                iconURL = "https://i.imgur.com/2FxwJs8.png";
            } else {
                iconURL = null;
            }

            EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
                    .color(
                            card.color.equalsIgnoreCase("green") ? Color.GREEN :
                                    card.color.equalsIgnoreCase("red") ? Color.RED :
                                            card.color.equalsIgnoreCase("white") ? Color.WHITE :
                                                    card.color.equalsIgnoreCase("black") ? Color.DARK_GRAY :
                                                            card.color.equalsIgnoreCase("blue") ? Color.BLUE :
                                                                    Color.DARK_GOLDENROD
                    )
                    .author(card.name, null, iconURL)
                    .title(card.type + " · " + card.color.substring(0, 1).toUpperCase() + card.color.substring(1))
                    .description("**" + card.folder + "**  \nPage: **" + card.page + "**")
                    .addField("Count", String.valueOf(card.count), true)
                    .addField("Attack", card.attack >= 0 ? String.valueOf(card.attack) : "—", true)
                    .addField("Defense", card.defense >= 0 ? String.valueOf(card.defense) : "—", true)
                    .addField("Classes", (card.cardClass != null && !card.cardClass.isEmpty())
                            ? String.join(", ", card.cardClass)
                            : "*None*", false)
                    .addField("Mana Cost",
                            String.format(
                                    "Green: %d | Red: %d | White: %d | Black: %d | Blue: %d | Neutral: %d",
                                    card.green_cost, card.red_cost, card.white_cost, card.black_cost, card.blue_cost, card.neutral_cost
                            ),
                            false)
                    .timestamp(Instant.now())
                    .footer("Magic The Gathering", "https://i.imgur.com/SEh8aKw.png");

            if (card.loyalty > 0) {
                embedBuilder.addField("Loyality", String.valueOf(card.loyalty), true);
            }

            EmbedCreateSpec embed = embedBuilder.build();

            return event.reply().withEmbeds(embed).withEphemeral(true);
        }
    }

    public static ApplicationCommandRequest getCardBuilder() {
        return ApplicationCommandRequest.builder()
                .name("getcard")
                .description("Gives you information about a Card using its name.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();
    }
}

package net.dzultra.MTGDiscordBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final List<MTGCard> cards = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        long applicationId = 1409238066559979590L;
        long guildId = 1129737425653071992L;

        DiscordClient client = DiscordClient.create(Files.readString(Path.of("src/main/resources/assets/token")));

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
                            }))
                    .then();

            Mono<Void> getCardByName = gateway.on(ChatInputInteractionEvent.class, event -> {
                if (event.getCommandName().equals("cardbyname")) {
                    MTGCard card;
                    String card_name = event.getOptionAsString("name").get();

                    try {
                        card = MTGCardDatabase.getCard(card_name);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    event.deferReply().withEphemeral(true);

                    if (card == null) {
                        return event.createFollowup().event()
                                .reply("No card with this name has been found!\nPlease try a different name.")
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

                        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                .color(Color.DARK_GOLDENROD)
                                .author(card.name, null, iconURL)
                                .addField(card.folder, "Page: " + card.page, false)
                                .addField("- Count -", String.valueOf(card.count), false)
                                .addField("- Type -", String.valueOf(card.type), false)
                                .addField("- Attack -", String.valueOf(card.attack), false)
                                .addField("- Defense -", String.valueOf(card.defense), false)
                                .addField("- Loyalty -", String.valueOf(card.loyalty), false)
                                .addField("----------------", "", false)
                                .addField("- Green Cost -", String.valueOf(card.green_cost), true)
                                .addField("- Red Cost -", String.valueOf(card.red_cost), true)
                                .addField("- White Cost -", String.valueOf(card.white_cost), true)
                                .addField("- Black Cost -", String.valueOf(card.black_cost), true)
                                .addField("- Blue Cost -", String.valueOf(card.blue_cost), true)
                                .addField("- Neutral Cost -", String.valueOf(card.neutral_cost), true)
                                .timestamp(Instant.now())
                                .footer("Magic The Gathering", "https://i.imgur.com/SEh8aKw.png")
                                .build();

                        return event.createFollowup().event()
                                .reply("")
                                .withEmbeds(embed)
                                .withEphemeral(true);
                    }

                }
                return Mono.empty();
            }).then();

            return printOnLogin.and(getCardByName);
        });

        ApplicationCommandRequest greetCmdRequest = ApplicationCommandRequest.builder()
                .name("cardbyname")
                .description("Gives you information about a Card using its name.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        client.getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, greetCmdRequest)
                .subscribe();

        login.block();

//        addCard(new MTGCard(
//                "Silver Knight",
//                1,
//                "White",
//                "White Folder",
//                5,
//                2, 2,
//                "Creature",
//                List.of("Knight"),
//                0, 0, 0, 2, 0, 0,
//                null
//        ));
//        MTGCardDatabase.saveAllCards(cards);
//        MTGCardDatabase.removeCard("Silver Knight", 1);



    }

    protected static void addCard(MTGCard newCard) throws IOException {
        File cardFile = new File("src/main/resources/cards", newCard.name + ".json");

        if (cardFile.exists()) {
            // Read existing card
            MTGCard existingCard = MTGCardDatabase.mapper.readValue(cardFile, MTGCard.class);
            existingCard.count += newCard.count; // increase count
            MTGCardDatabase.saveCard(existingCard); // overwrite with new count
        } else {
            // New card, just save
            MTGCardDatabase.saveCard(newCard);
        }
    }
}

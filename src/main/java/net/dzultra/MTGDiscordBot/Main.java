package net.dzultra.MTGDiscordBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
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
import java.util.Map;

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
                                System.out.printf("Logged in as %s#%s%n", self.getUsername(), "");
                            }))
                    .then();

            Mono<Void> getCardByName = gateway.on(ChatInputInteractionEvent.class, event -> {
//                System.out.println("Options received for /" + event.getCommandName() + ":");
//                event.getOptions().forEach(opt ->
//                        System.out.println(" - " + opt.getName() + " = " + opt.getValue().map(Object::toString).orElse("null"))
//                );

                if (event.getCommandName().equals("getcard")) {
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

                } else if (event.getCommandName().equals("addcard")) {
                    try {
                        // ---------- REQUIRED OPTIONS ----------
                        String name = event.getOption("name").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseThrow(() -> new IllegalArgumentException("Missing option: name"));

                        int count = event.getOption("count").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalArgumentException("Missing option: count"));

                        String color = event.getOption("color").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseThrow(() -> new IllegalArgumentException("Missing option: color"));

                        String folder = event.getOption("folder").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseThrow(() -> new IllegalArgumentException("Missing option: folder"));

                        int page = event.getOption("page").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalArgumentException("Missing option: page"));

                        String type = event.getOption("type").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseThrow(() -> new IllegalArgumentException("Missing option: type"));

                        // ---------- OPTIONAL OPTIONS ----------
                        int attack = event.getOption("attack").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(-1);

                        int defense = event.getOption("defense").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(-1);

                        int neutral_cost = event.getOption("neutral_cost").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(0);

                        int green_cost = event.getOption("green_cost").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(0);

                        int red_cost = event.getOption("red_cost").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(0);

                        int white_cost = event.getOption("white_cost").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(0);

                        int black_cost = event.getOption("black_cost").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(0);

                        int blue_cost = event.getOption("blue_cost").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(0);

                        int loyalty = event.getOption("loyalty").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(Math::toIntExact).orElse(-1);

                        // ---------- CLASSES HANDLING ----------
                        String classesRaw = event.getOption("classes").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

                        List<String> classesList = classesRaw.isBlank()
                                ? List.of()
                                : List.of(classesRaw.split("\\s+"));

                        // ---------- CREATE CARD ----------
                        MTGCard newCard = new MTGCard(
                                name, count, color, folder, page,
                                attack, defense, type, classesList,
                                neutral_cost, green_cost, red_cost, white_cost, black_cost, blue_cost,
                                loyalty
                        );

                        // Add or update card in your list
                        addCard(newCard);

                        // Save everything
                        MTGCardDatabase.saveAllCards(cards);

                        return event.reply("✅ Card **" + name + "** added successfully!")
                                .withEphemeral(true);

                    } catch (Exception e) {
                        e.printStackTrace(); // <-- NOW you will see the exact cause in console
                        return event.reply("❌ Failed to add card: " + e.getMessage())
                                .withEphemeral(true);
                    }
                } else if (event.getCommandName().equals("removecard")) {
                    try {
                        String name = event.getOption("name")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: name"));

                        int countToRemove = event.getOption("count")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: count"));

                        if (countToRemove <= 0) {
                            return event.reply("❌ Amount to remove needs to be bigger than 0")
                                    .withEphemeral(true);
                        }

                        File cardFile = new File("src/main/data/cards", name + ".json");
                        if (!cardFile.exists()) {
                            return event.reply("❌ No Card with the Name **" + name + "** found.")
                                    .withEphemeral(true);
                        }

                        MTGCard card = MTGCardDatabase.mapper.readValue(cardFile, MTGCard.class);
                        card.count -= countToRemove;

                        if (card.count < 1) {
                            cardFile.delete();
                            cards.removeIf(c -> c.name.equalsIgnoreCase(name));
                            return event.reply("🗑️ Card **" + name + "** has been deleted. (Count below 0)")
                                    .withEphemeral(true);
                        } else {
                            MTGCardDatabase.saveCard(card);
                            cards.removeIf(c -> c.name.equalsIgnoreCase(name));
                            cards.add(card);
                            return event.reply("🗑️ **" + countToRemove + "** Cards have been removed from **" + name + "**.\n" +
                                            "Remaining Amount: **" + card.count + "**")
                                    .withEphemeral(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return event.reply("❌ Error while trying to remove Card " + e.getMessage())
                                .withEphemeral(true);
                    }
                } else if (event.getCommandName().equals("addexistingcard")) {
                    try {
                        String name = event.getOption("name")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: name"));

                        int countToAdd = event.getOption("count")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: count"));

                        if (countToAdd <= 0) {
                            return event.reply("❌ The amount to add must be greater than 0.")
                                    .withEphemeral(true);
                        }

                        File cardFile = new File("src/main/data/cards", name + ".json");
                        if (!cardFile.exists()) {
                            return event.reply("❌ No Card has been found with the Name **" + name + "**")
                                    .withEphemeral(true);
                        }

                        MTGCard card = MTGCardDatabase.mapper.readValue(cardFile, MTGCard.class);
                        card.count += countToAdd;
                        MTGCardDatabase.saveCard(card);

                        cards.removeIf(c -> c.name.equalsIgnoreCase(name));
                        cards.add(card);

                        return event.reply("✅ Count of Card **" + name + "** has been increased by **" + countToAdd + "**\n" +
                                        "New Total: **" + card.count + "**")
                                .withEphemeral(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return event.reply("❌ Error while trying to increase amount of existing card: " + e.getMessage())
                                .withEphemeral(true);
                    }
                }
                return Mono.empty();
            }).then();

            return printOnLogin.and(getCardByName);
        });

        ApplicationCommandRequest getCard = ApplicationCommandRequest.builder()
                .name("getcard")
                .description("Gives you information about a Card using its name.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        ApplicationCommandRequest addCard = ApplicationCommandRequest.builder()
                .name("addcard")
                .description("Add a Card to the Database")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("count")
                        .description("Amount of this Card being added")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("color")
                        .description("Color of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("folder")
                        .description("Name of the Folder the card is inside")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("page")
                        .description("The Page the Card is on")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("type")
                        .description("Type of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("attack")
                        .description("Attack Points")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("defense")
                        .description("Defense Points")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("classes")
                        .description("Classes of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("neutral_cost")
                        .description("Neutral Mana Cost")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("green_cost")
                        .description("Green Mana Cost")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("red_cost")
                        .description("Red Mana Cost")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("white_cost")
                        .description("White Mana Cost")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("black_cost")
                        .description("Black Mana Cost")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("blue_cost")
                        .description("Blue Mana Cost")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("loyalty")
                        .description("Base Loyalty")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(false)
                        .build()
                )
                .build();

        ApplicationCommandRequest removeCard = ApplicationCommandRequest.builder()
                .name("removecard")
                .description("Removes a specific count of card")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("count")
                        .description("Amount of Cards to remove")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(true)
                        .build()
                ).build();

        ApplicationCommandRequest addExistingCard = ApplicationCommandRequest.builder()
                .name("addexistingcard")
                .description("Increase amount of existing Card")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("count")
                        .description("Amount of Cards to add")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .required(true)
                        .build()
                ).build();

        Map<String, ApplicationCommandData> discordCommands = client
                .getApplicationService()
                .getGuildApplicationCommands(applicationId, guildId)
                .collectMap(ApplicationCommandData::name)
                .block();

        ApplicationCommandData addCardCmd = discordCommands.get(addCard.name());
        ApplicationCommandData getCardCmd = discordCommands.get(getCard.name());
        ApplicationCommandData removeCardCmd = discordCommands.get(removeCard.name());
        ApplicationCommandData addExistingCardCmd = discordCommands.get(addExistingCard.name());

        client.getApplicationService()
                .modifyGuildApplicationCommand(applicationId, guildId, addCardCmd.id().asLong(), addCard)
                .subscribe();
        client.getApplicationService()
                .modifyGuildApplicationCommand(applicationId, guildId, getCardCmd.id().asLong(), getCard)
                .subscribe();
        client.getApplicationService()
                .modifyGuildApplicationCommand(applicationId, guildId, removeCardCmd.id().asLong(), removeCard)
                .subscribe();
        client.getApplicationService()
                .modifyGuildApplicationCommand(applicationId, guildId, addExistingCardCmd.id().asLong(), addExistingCard)
                .subscribe();

//        client.getApplicationService()
//                .deleteGuildApplicationCommand(applicationId, guildId, cardByNameCmd.id().asLong())
//                .subscribe();
//        client.getApplicationService()
//                .createGuildApplicationCommand(applicationId, guildId, addExistingCard)
//                .subscribe();

        login.block();
//        MTGCardDatabase.removeCard("Silver Knight", 1);



    }

    protected static void addCard(MTGCard newCard) throws IOException {
        File cardFile = new File("src/main/data/cards", newCard.name + ".json");

        if (cardFile.exists()) {
            // Read existing card
            MTGCard existingCard = MTGCardDatabase.mapper.readValue(cardFile, MTGCard.class);
            existingCard.count += newCard.count; // increase count
            MTGCardDatabase.saveCard(existingCard); // overwrite with new count

            // Update in-memory list
            cards.removeIf(c -> c.name.equalsIgnoreCase(existingCard.name));
            cards.add(existingCard);
        } else {
            // New card, just save
            MTGCardDatabase.saveCard(newCard);
            cards.add(newCard); // also add to in-memory list
        }
    }
}

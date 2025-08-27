package net.dzultra.MTGDiscordBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import net.dzultra.MTGDiscordBot.MTGCard;
import net.dzultra.MTGDiscordBot.MTGCardDatabase;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static net.dzultra.MTGDiscordBot.Main.cards;

public class AddCardCommand {

    public static Mono<Void> addCardCommand(ChatInputInteractionEvent event) {
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

    public static ApplicationCommandRequest addCardBuilder() {
        return ApplicationCommandRequest.builder()
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
    }
}

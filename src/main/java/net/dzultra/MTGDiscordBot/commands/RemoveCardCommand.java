package net.dzultra.MTGDiscordBot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import net.dzultra.MTGDiscordBot.DataHandler;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;

public class RemoveCardCommand {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Mono<Void> removeCardCommand(ChatInputInteractionEvent event) {
        try {
            // --- Get options ---
            String name = DataHandler.formatName(event.getOption("name")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: name")));

            int countToRemove = event.getOption("count")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong)
                    .map(Math::toIntExact)
                    .orElse(1);

            if (countToRemove <= 0) {
                return event.reply("❌ Count to remove must be greater than 0.")
                        .withEphemeral(true);
            }

            if (!DataHandler.isNameValid(name)) {
                return event.reply("❌ Invalid characters in name. Avoid using: <>:\"/\\|?* and control characters.")
                        .withEphemeral(true).then();
            }

            Path cardPath = DataHandler.getCardPath(name.toLowerCase());

            if (!Files.exists(cardPath)) {
                return event.reply("❌ Card `" + name + "` does not exist in the database.")
                        .withEphemeral(true);
            }

            // --- Read JSON ---
            JsonNode root = mapper.readTree(Files.readString(cardPath));
            int currentCount = root.get("count").asInt();

            if (countToRemove > currentCount) {
                if (currentCount == 1) {
                    return event.reply("❌ Only " + currentCount + " copy of **" + name + "** exists, you tried to remove " + countToRemove + ".")
                            .withEphemeral(true);
                } else {
                    return event.reply("❌ Only " + currentCount + " copies of **" + name + "** exist, you tried to remove " + countToRemove + ".")
                            .withEphemeral(true);
                }
            }

            int newCount = currentCount - countToRemove;

            if (newCount > 0) {
                ((ObjectNode) root).put("count", newCount);
                Files.writeString(cardPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
                if (countToRemove == 1) {
                    return event.reply("✅ Removed " + countToRemove + " copy of **" + name + "**. Remaining: " + newCount)
                            .withEphemeral(true);
                } else {
                    return event.reply("✅ Removed " + countToRemove + " copies of **" + name + "**. Remaining: " + newCount)
                            .withEphemeral(true);
                }
            } else {
                Files.delete(cardPath);
                return event.reply("🗑️ Removed all copies of **" + name + "**. Card File deleted from database.")
                        .withEphemeral(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return event.reply("❌ Error while trying to remove card: " + e.getMessage())
                    .withEphemeral(true);
        }
    }

    public static ApplicationCommandRequest removeCardBuilder() {
        return ApplicationCommandRequest.builder()
                .name("removecard")
                .description("Removes a specific count of a card")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(3) // STRING
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("count")
                        .description("Amount of Cards to remove")
                        .type(4) // INTEGER
                        .required(false)
                        .build())
                .build();
    }
}

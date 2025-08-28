package net.dzultra.MTGDiscordBot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;

public class GetCardCommand {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Mono<Void> getCardCommand(ChatInputInteractionEvent event) {
        try {
            // --- Get options ---
            String name = event.getOption("name")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: name"));

            Path cardPath = Path.of("src/main/cards/" + name + ".json");

            if (!Files.exists(cardPath)) {
                return event.reply("❌ Card `" + name + "` not found in database.").withEphemeral(true).then();
            }

            // --- Read JSON file ---
            JsonNode root = mapper.readTree(Files.readString(cardPath));
            String cardName = root.get("name").asText();
            String imageUrl = root.get("image_link").asText();
            int count = root.get("count").asInt();
            String folder = root.get("folder").asText();
            int page = root.get("page").asInt();

            EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                    .title(cardName)
                    .addField("Folder", folder, true)
                    .addField("Page", String.valueOf(page), true)
                    .image(imageUrl)
                    .color(Color.GRAY);

            if (count == 1) {
                embed.description("You own **" + count + "** copy of this card.");
            } else {
                embed.description("You own **" + count + "** copies of this card.");
            }

            // --- Send feedback embed ---
            return event.reply()
                    .withEmbeds(embed.build())
                    .withEphemeral(true)
                    .then();

        } catch (Exception e) {
            e.printStackTrace();
            return event.reply("❌ Error while trying to get card: " + e.getMessage())
                    .withEphemeral(true).then();
        }
    }

    public static ApplicationCommandRequest getCardBuilder() {
        return ApplicationCommandRequest.builder()
                .name("getcard")
                .description("Gives you information about a Card using its name.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(3) // String
                        .required(true)
                        .build())
                .build();
    }
}

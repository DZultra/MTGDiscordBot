package net.dzultra.MTGDiscordBot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import net.dzultra.MTGDiscordBot.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddCardCommand {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AddCardCommand.class);

    public static Mono<Void> addCardCommand(ChatInputInteractionEvent event) {
        try {
            // --- Get options ---
            String name = event.getOption("name")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: name"));

            String folder = event.getOption("folder")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: folder"));

            int page = event.getOption("page")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong)
                    .map(Math::toIntExact)
                    .orElseThrow(() -> new IllegalArgumentException("❌ Missing Option: page"));

            int countToAdd = event.getOption("count")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong)
                    .map(Math::toIntExact)
                    .orElse(1);

            if (countToAdd <= 0) {
                return event.reply("❌ Count must be greater than 0.").withEphemeral(true).then();
            }

            if (page <= 0) {
                return event.reply("❌ Page must be greater than 0.").withEphemeral(true).then();
            }

            Path cardPath = Path.of("src/main/cards/" + name + ".json");

            if (Files.exists(cardPath)) {
                // --- Update existing file ---
                JsonNode root = mapper.readTree(Files.readString(cardPath));
                int currentCount = root.get("count").asInt();
                ((ObjectNode) root).put("count", currentCount + countToAdd);
                Files.writeString(cardPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

                return event.reply("✅ Updated **" + name + "** count to **" + (currentCount + countToAdd) + "**")
                        .withEphemeral(true);
            } else {
                // --- Fetch from Scryfall ---
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.scryfall.com/cards/named?fuzzy=" + name.replace(" ", "+")))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        return event.reply("❌ Could not find card with name: `" + name + "`")
                                .withEphemeral(true);
                    }

                    JsonNode apiData = mapper.readTree(response.body());

                    if (apiData.has("object") && apiData.get("object").asText().equals("error")) {
                        return event.reply("❌ Card `" + name + "` not found on Scryfall.")
                                .withEphemeral(true);
                    }

                    String imageUrl = apiData.get("image_uris").get("border_crop").asText();

                    ObjectNode newCard = mapper.createObjectNode();
                    newCard.put("name", name);
                    newCard.put("image_link", imageUrl);
                    newCard.put("count", countToAdd);
                    newCard.put("folder", folder);
                    newCard.put("page", page);

                    Files.createDirectories(cardPath.getParent());
                    Files.writeString(cardPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newCard));

                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .image(imageUrl)
                            .color(Color.GRAY)
                            .build();

                    return event.reply("✅ Added new card: **" + name + "**\n**Count:** " + countToAdd + "x")
                            .withEmbeds(embed)
                            .withEphemeral(true);
                } catch (Exception e) {
                    String message = "❌ Error fetching card data from Scryfall: " + e.getMessage();
                    //e.printStackTrace();
                    Main.logger.severe(message);
                    return event.reply(message)
                            .withEphemeral(true);
                }
            }
        } catch (Exception e) {
            String message = "❌ Error while trying to add card: " + e.getMessage();
            //e.printStackTrace();
            Main.logger.severe(message);
            return event.reply(message)
                    .withEphemeral(true);
        }
    }

    public static ApplicationCommandRequest addCardBuilder() {
        return ApplicationCommandRequest.builder()
                .name("addcard")
                .description("Increase amount of existing Card or add a new one from Scryfall")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Name of the Card")
                        .type(3) // STRING Value
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("folder")
                        .description("Name of the Card")
                        .type(3) // STRING Value
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("page")
                        .description("Name of the Card")
                        .type(4) // INTEGER Value
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("count")
                        .description("Amount of Cards to add")
                        .type(4) // INTEGER Value
                        .required(false)
                        .build())
                .build();
    }
}

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

import static net.dzultra.MTGDiscordBot.Main.cards;

public class AddExistingCardCommand {
    public static Mono<Void> addExistingCardCommand(ChatInputInteractionEvent event) {
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

    public static ApplicationCommandRequest addExistingCardBuilder(){
        return ApplicationCommandRequest.builder()
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
    }
}

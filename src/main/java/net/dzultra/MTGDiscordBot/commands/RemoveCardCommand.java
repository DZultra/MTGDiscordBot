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

public class RemoveCardCommand {
    public static Mono<Void> removeCardCommand(ChatInputInteractionEvent event) {
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
    }

    public static ApplicationCommandRequest removeCardBuilder(){
        return ApplicationCommandRequest.builder()
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
    }
}

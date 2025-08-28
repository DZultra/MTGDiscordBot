package net.dzultra.MTGDiscordBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import net.dzultra.MTGDiscordBot.commands.AddCardCommand;
import net.dzultra.MTGDiscordBot.commands.GetCardCommand;
import net.dzultra.MTGDiscordBot.commands.RemoveCardCommand;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class Main {
    public static Logger logger = Logger.getLogger("[MTGDiscordBot]");

    public static void main(String[] args) throws IOException {
        DiscordClient client = DiscordClient.create(DataHandler.getToken());
        long guildId = 1129737425653071992L;
        long applicationId;

        try {
            applicationId = client.getApplicationId().block();
            //logger.info("Application ID: " + applicationId);
        } catch (Exception e) {
            logger.severe("❌ Failed to create Discord client. Invalid token?: " + e.getMessage());
            //e.printStackTrace();
            return;
        }

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                logger.info("Logged in as " + self.getUsername());
                            }))
                    .then();

            Mono<Void> commands = gateway.on(ChatInputInteractionEvent.class, event -> switch (event.getCommandName()) {
                case "addcard" -> AddCardCommand.addCardCommand(event);
                case "removecard" -> RemoveCardCommand.removeCardCommand(event);
                case "getcard" -> GetCardCommand.getCardCommand(event);
                default -> event.reply("❌ Unknown command\nThis command might be deprecated!").withEphemeral(true).then();
            }).then();

            return printOnLogin.and(commands);
        });

        ApplicationCommandRequest getCard = GetCardCommand.getCardBuilder();
        ApplicationCommandRequest removeCard = RemoveCardCommand.removeCardBuilder();
        ApplicationCommandRequest addCard = AddCardCommand.addCardBuilder();

        Map<String, ApplicationCommandData> discordCommands = client
                .getApplicationService()
                .getGuildApplicationCommands(applicationId, guildId)
                .collectMap(ApplicationCommandData::name)
                .block();

        String[] commandNames = {addCard.name(), getCard.name(), removeCard.name()};
        ApplicationCommandRequest[] commandRequests = {addCard, getCard, removeCard};

        for (int i = 0; i < commandNames.length; i++) {
            ApplicationCommandData cmdData = discordCommands.get(commandNames[i]);
            client.getApplicationService()
                    .modifyGuildApplicationCommand(applicationId, guildId, cmdData.id().asLong(), commandRequests[i])
                    .subscribe();
        }
//        client.getApplicationService()
//                .deleteGuildApplicationCommand(applicationId, guildId,discordCommands.get("").id().asLong())
//                .subscribe();
//        client.getApplicationService()
//                .createGuildApplicationCommand(applicationId, guildId, addExistingCard)
//                .subscribe();

        login.block();
    }
}

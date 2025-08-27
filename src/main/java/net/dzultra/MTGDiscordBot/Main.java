package net.dzultra.MTGDiscordBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import net.dzultra.MTGDiscordBot.commands.AddCardCommand;
import net.dzultra.MTGDiscordBot.commands.AddExistingCardCommand;
import net.dzultra.MTGDiscordBot.commands.GetCardCommand;
import net.dzultra.MTGDiscordBot.commands.RemoveCardCommand;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static final List<MTGCard> cards = new ArrayList<>();

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
                    GetCardCommand.getCardCommand(event);
                } else if (event.getCommandName().equals("addcard")) {
                    AddCardCommand.addCardCommand(event);
                } else if (event.getCommandName().equals("removecard")) {
                    RemoveCardCommand.removeCardCommand(event);
                } else if (event.getCommandName().equals("addexistingcard")) {
                    AddExistingCardCommand.addExistingCardCommand(event);
                }
                return Mono.empty();
            }).then();

            return printOnLogin.and(getCardByName);
        });

        ApplicationCommandRequest getCard = GetCardCommand.getCardBuilder();

        ApplicationCommandRequest addCard = AddCardCommand.addCardBuilder();

        ApplicationCommandRequest removeCard = RemoveCardCommand.removeCardBuilder();

        ApplicationCommandRequest addExistingCard = AddExistingCardCommand.addExistingCardBuilder();

        Map<String, ApplicationCommandData> discordCommands = client
                .getApplicationService()
                .getGuildApplicationCommands(applicationId, guildId)
                .collectMap(ApplicationCommandData::name)
                .block();

        String[] commandNames = {addCard.name(), getCard.name(), removeCard.name(), addExistingCard.name()};
        ApplicationCommandRequest[] commandRequests = {addCard, getCard, removeCard, addExistingCard};

        for (int i = 0; i < commandNames.length; i++) {
            ApplicationCommandData cmdData = discordCommands.get(commandNames[i]);
            client.getApplicationService()
                    .modifyGuildApplicationCommand(applicationId, guildId, cmdData.id().asLong(), commandRequests[i])
                    .subscribe();
        }
//        client.getApplicationService()
//                .deleteGuildApplicationCommand(applicationId, guildId, cardByNameCmd.id().asLong())
//                .subscribe();
//        client.getApplicationService()
//                .createGuildApplicationCommand(applicationId, guildId, addExistingCard)
//                .subscribe();

        login.block();
    }
}

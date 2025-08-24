package net.dzultra.MTGDiscordBot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MTGCardDatabase {
    protected static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String resourcePath = "src/main/resources/cards";

    protected static void saveCard(MTGCard card) throws IOException {
        File dir = new File("src/main/resources/cards");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, card.name + ".json");
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(file, card);
    }
    protected static void saveAllCards(List<MTGCard> cards) throws IOException {
        for (MTGCard card : cards) {
            saveCard(card);
        }
    }

    protected static void removeCard(String cardName, int removeCount) throws IOException {
        File cardFile = new File(resourcePath, cardName + ".json");
        if (!cardFile.exists()) {
            return; // card file not found
        }

        // Read card from JSON
        MTGCard card = mapper.readValue(cardFile, MTGCard.class);

        if (removeCount >= card.count) {
            // Remove all -> delete file
            if (!cardFile.delete()) {
                throw new IOException("Failed to delete file: " + cardFile.getAbsolutePath());
            }
        } else {
            // Just reduce count
            card.count -= removeCount;
            mapper.writerWithDefaultPrettyPrinter().writeValue(cardFile, card);
        }
    }

    public static MTGCard getCard(String cardName) throws IOException {
        File dir = new File(resourcePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return null;
        }

        for (File file : files) {
            MTGCard card = mapper.readValue(file, MTGCard.class);
            if (card.name.equalsIgnoreCase(cardName)) {
                return card;
            }
        }

        return null; // not found
    }
}


package net.dzultra.MTGDiscordBot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class MTGPageFixer {

    public static void main(String[] args) {
        // Path to your database folder
        File folder = new File("C:\\Users\\Daniel Zink\\.MTGDiscordBot\\cards");

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("The provided path is not valid: " + folder.getAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (files == null) {
            System.out.println("No JSON files found in folder.");
            return;
        }

        for (File file : files) {
            try {
                JsonNode root = mapper.readTree(file);

                if (root.has("page") && root.get("page").isInt()) {
                    int page = root.get("page").asInt();

                    if (page >= 35 && page <= 54) {
                        int newPage = page - 34;

                        ((ObjectNode) root).put("page", newPage);

                        mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);

                        System.out.println("Updated " + file.getName() + " from page " + page + " to " + newPage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading/writing file: " + file.getName());
                e.printStackTrace();
            }
        }

        System.out.println("Page adjustment complete.");
    }
}

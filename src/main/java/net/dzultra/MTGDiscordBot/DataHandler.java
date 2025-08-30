package net.dzultra.MTGDiscordBot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataHandler {
    public static boolean isNameValid(String name) {
        String regex = "^[^<>:\"/\\\\|?*\\x00]+$";
        return name.matches(regex);
    }

    public static Path getCardPath(String name) {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".MTGDiscordBot", "cards", name + ".json");
    }

    public static String getToken() throws IOException {
        String userHome = System.getProperty("user.home");
        Path path = Path.of(userHome, ".MTGDiscordBot", "token");
        return Files.readString(path).trim();
    }

    public static String formatName(String name) {
        if (name == null || name.isEmpty()) return name;

        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb.toString();
    }
}

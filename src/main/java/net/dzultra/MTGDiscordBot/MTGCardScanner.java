package net.dzultra.MTGDiscordBot;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MTGCardScanner {

    public static void main(String[] args) {
        try {
            // 1. Capture image from webcam
            Webcam webcam = Webcam.getDefault();
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();

            System.out.println("Capturing image...");
            BufferedImage image = webcam.getImage();
            File file = new File("src/main/data/img/capture.png");
            ImageIO.write(image, "PNG", file);
            webcam.close();

            // 2. Run OCR with Tesseract (make sure tessdata is installed)
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Users\\Daniel Zink\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");
            tesseract.setLanguage("eng+deu");

            System.out.println("Running OCR...");
            String text = tesseract.doOCR(file);
            System.out.println("OCR Result:\n" + text);

            // Assume card name is the first line of text
            String cardName = text.split("\n")[0].trim();
            System.out.println("Detected Card Name: " + cardName);

            // 3. Query Scryfall API
            //String apiUrl = "https://api.scryfall.com/cards/named?fuzzy=" + cardName.replace(" ", "+");
            String apiUrl = "https://api.scryfall.com/cards/named?fuzzy=Demütiger+Naturkundler";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String cardJson = response.body();
                System.out.println("Card details retrieved!");

                // 4. Save JSON result
                try (FileWriter writer = new FileWriter("src/main/data/card.json")) {
                    writer.write(cardJson);
                }
                System.out.println("Saved card details to card.json");
            } else {
                System.err.println("Failed to retrieve card from Scryfall. Status: " + response.statusCode());
            }

        } catch (IOException | TesseractException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

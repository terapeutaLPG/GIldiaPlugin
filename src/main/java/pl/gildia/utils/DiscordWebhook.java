package pl.gildia.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.bukkit.configuration.file.FileConfiguration;

import pl.gildia.GildiaPlugin;

public class DiscordWebhook {

    private final GildiaPlugin plugin;
    private String webhookUrl;
    private String username;
    private String avatarUrl;
    private boolean enabled;

    public DiscordWebhook(GildiaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("discord.webhook.enabled", false);
        this.webhookUrl = config.getString("discord.webhook.url", "");
        this.username = config.getString("discord.webhook.username", "GildiaPlugin Logger");
        this.avatarUrl = config.getString("discord.webhook.avatar_url", "");
    }

    public void sendAdminDeleteGuildLog(String adminName, String guildName, String guildTag, String reason) {
        if (!enabled || webhookUrl.isEmpty() || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL")) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String embed = String.format(
                "{"
                + "\"username\": \"%s\","
                + "\"avatar_url\": \"%s\","
                + "\"embeds\": [{"
                + "\"title\": \"🔨 Gildia usunięta przez administratora\","
                + "\"color\": 15158332," // Czerwony kolor
                + "\"fields\": ["
                + "{"
                + "\"name\": \"Administrator\","
                + "\"value\": \"%s\","
                + "\"inline\": true"
                + "},"
                + "{"
                + "\"name\": \"Gildia\","
                + "\"value\": \"[%s] %s\","
                + "\"inline\": true"
                + "},"
                + "{"
                + "\"name\": \"Powód\","
                + "\"value\": \"%s\","
                + "\"inline\": false"
                + "},"
                + "{"
                + "\"name\": \"Data i czas\","
                + "\"value\": \"%s\","
                + "\"inline\": false"
                + "}"
                + "],"
                + "\"footer\": {"
                + "\"text\": \"GildiaPlugin Logger\""
                + "}"
                + "}]"
                + "}",
                username, avatarUrl, adminName, guildTag, guildName, reason, timestamp
        );

        // Wysyłanie webhook'a w osobnym wątku
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                sendWebhook(embed);
            } catch (IOException e) {
                plugin.getLogger().warning("Nie udało się wysłać webhook'a Discord: " + e.getMessage());
            }
        });
    }

    private void sendWebhook(String jsonPayload) throws IOException {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "GildiaPlugin/1.0");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Discord webhook returned HTTP " + responseCode);
        }

        connection.disconnect();
    }
}

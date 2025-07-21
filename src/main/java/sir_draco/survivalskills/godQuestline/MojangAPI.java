package sir_draco.survivalskills.godQuestline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class MojangAPI {

    public static JsonObject getPlayerProfile(UUID uuid) throws IOException, InterruptedException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    public static String[] getSkinData(UUID uuid) throws IOException, InterruptedException {
        JsonObject profile = getPlayerProfile(uuid);
        JsonObject properties = profile.getAsJsonArray("properties").get(0).getAsJsonObject();
        String value = properties.get("value").getAsString();
        String signature = properties.get("signature").getAsString();

        return new String[]{signature, value};
    }
}

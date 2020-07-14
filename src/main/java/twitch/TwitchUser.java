package twitch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import twitch.exception.InvalidAccount;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class TwitchUser {
    private final String token;
    private final boolean valid;

    private String clientID;
    private String userID;
    private String login;


    /**
     * Constructor of Twitch.TwitchUser class
     *
     * @param token
     * @param ignoreInvalid true - ignore invalid accounts
     */
    public TwitchUser(String token, boolean ignoreInvalid) throws InvalidAccount {
        this.token = token;
        this.valid = getTokenInformation(token);
        if (!ignoreInvalid && !this.valid) {
            throw new InvalidAccount("Invalid token : ", token);
        }
    }

    public TwitchUser(String token) throws InvalidAccount {
        this.token = token;
        this.valid = getTokenInformation(token);
        if (!this.valid) {
            throw new InvalidAccount("Invalid token : ", token);
        }
    }

    /**
     * The Method that receives user's clientID and userID
     *
     * @param token
     * @return true - successful, false - failed
     */
    private boolean getTokenInformation(String token) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://id.twitch.tv/oauth2/validate"))
                .setHeader("Authorization", " Bearer " + token)
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response).body());
        try {
            this.clientID = (String) jsonObject.get("client_id");
            this.userID = (String) jsonObject.get("user_id");
            this.login = (String) jsonObject.get("login");
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    /**
     * Method gets amount of followed.
     *
     * @return - amount of followed.
     */
    public int getFollowed() {
        if (!valid) return -1;
        JSONObject jsonObject = getFollowedJSON();
        int result;
        try {
            result = jsonObject.getInt("total");
        } catch (JSONException e) {
            System.out.println("Could not find key\"total\"");
            return -1;
        }
        return result;
    }

    /**
     * Get JSON of followed API result.
     *
     * @return JSONObject after API request.
     */
    private JSONObject getFollowedJSON() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitch.tv/helix/users/follows?from_id=" + userID))
                .setHeader("Authorization", " Bearer " + token)
                .setHeader("Client-ID", clientID)
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new JSONObject(Objects.requireNonNull(response).body());
    }

    /**
     * Check if TwitchUser can follow
     *
     * @return true - if can follow
     */
    public boolean canFollow() {
        return getFollowed() < 2000;
    }

    /**
     * Check if user is followed to "channelID"
     * @param channelID - channel to check
     * @return true - if followed
     */
    public boolean isFollowedTo(String channelID) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitch.tv/kraken/users/" + getUserID() + "/follows/channels/" + channelID + "/follows/channels/"))
                .setHeader("Authorization", " OAuth " + getToken())
                .setHeader("Client-ID", getClientID())
                .setHeader("Accept", "application/vnd.twitchtv.v5+json")
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(response).body().contains("is not following");
    }

    /**
     * Unfollow from all users
     */
    public void cleanAll() {
        if (!valid) return;
        try {
            JSONObject jsonObject = getFollowedJSON();
            int total = jsonObject.getInt("total");
            if (total == 0) return;
            JSONArray data = jsonObject.getJSONArray("data");
            int i = 0;
            for (int k = 0; k < total; k++) {
                if (i == data.length() - 1) {
                    data = getFollowedJSON().getJSONArray("data");
                    i = 0;
                }
                String userID = data.getJSONObject(i).getString("to_id");
                if (unfollow(userID)) {
                    System.out.println(k + "/" + total);
                } else {
                    System.out.println("failed to unfollowed");
                    break;
                }
                i++;
            }
        } catch (JSONException e) {
            System.out.println("Can't parse JSON");
        }
    }

    /**
     * Unfollow from specific amount of users
     *
     * @param amount - amount to unfollow
     */
    public void clean(int amount) {
        if (!valid) return;
        try {
            JSONObject jsonObject = getFollowedJSON();
            int total = jsonObject.getInt("total");
            if (total == 0) return;
            JSONArray data = jsonObject.getJSONArray("data");
            int i = 0;
            for (int k = 0; k < amount; k++) {
                if (i == data.length() - 1) {
                    data = getFollowedJSON().getJSONArray("data");
                    i = 0;
                }
                String userID = data.getJSONObject(i).getString("to_id");
                if (unfollow(userID)) {
                    System.out.println(k + "/" + total);
                } else {
                    System.out.println("Failed to unfollowed");
                    break;
                }
                i++;
            }
        } catch (JSONException e) {
            System.out.println("Can't parse JSON");
        }
    }

    /**
     * Unfollow from specific user
     *
     * @param channelID - channelID of this user
     * @return true - successful
     */
    public boolean unfollow(String channelID) {
        if (!valid) return false;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create("https://api.twitch.tv/kraken/users/" + userID + "/follows/channels/" + channelID))
                .setHeader("Authorization", " OAuth " + token)
                .setHeader("Client-ID", clientID)
                .setHeader("Accept", "application/vnd.twitchtv.v5+json")
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(response).body().isEmpty();
    }


    public String getLogin() {
        return login;
    }

    public String getToken() {
        return token;
    }

    public String getClientID() {
        return clientID;
    }

    public String getUserID() {
        return userID;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "TwitchUser{" +
                "token='" + token + '\'' +
                ", valid=" + valid +
                ", clientID='" + clientID + '\'' +
                ", userID='" + userID + '\'' +
                ", login='" + login + '\'' +
                '}';
    }
}

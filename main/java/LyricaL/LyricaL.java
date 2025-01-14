package LyricaL;

import java.io.*;
import java.net.URI;
import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;
import io.github.cdimascio.dotenv.Dotenv;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;
import org.apache.hc.core5.http.ParseException;

public class LyricaL {
    private static final String TOKEN_FILE = "spotify_tokens.txt";

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("LyricaL");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Dotenv dotenv = Dotenv.load();
        String clientId = dotenv.get("CLIENT_ID");
        String clientSecret = dotenv.get("CLIENT_SECRET");
        String redirectUriString = dotenv.get("REDIRECT_URI");
        URI redirectUri = URI.create(redirectUriString);

        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();

        // Load tokens from file if available
        try {
            String[] tokens = loadTokens();
            if (tokens != null) {
                spotifyApi.setAccessToken(tokens[0]);
                spotifyApi.setRefreshToken(tokens[1]);
                // Refresh the access token if needed
                refreshAccessToken(spotifyApi);
            } else {
                authenticateUser(spotifyApi);
            }

            // Start a thread to check currently playing track
            Thread thread = new Thread(() -> getCurrentlyPlayingTrack(spotifyApi));
            thread.start();

        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void authenticateUser(SpotifyApi spotifyApi) throws IOException, SpotifyWebApiException, ParseException {
        String authorizationUrl = spotifyApi.authorizationCodeUri()
                .scope("user-read-currently-playing")
                .build()
                .execute()
                .toString();

        System.out.println("Visit this URL to authorize: " + authorizationUrl);
        String code = JOptionPane.showInputDialog("Enter the authorization code: ");

        var authorizationCodeCredentials = spotifyApi.authorizationCode(code)
                .build()
                .execute();

        spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
        spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

        // Save tokens to file
        saveTokens(authorizationCodeCredentials.getAccessToken(), authorizationCodeCredentials.getRefreshToken());
    }

    private static void refreshAccessToken(SpotifyApi spotifyApi) throws IOException, SpotifyWebApiException, ParseException {
        if (spotifyApi.getRefreshToken() != null) {
            var refreshTokenCredentials = spotifyApi.authorizationCodeRefresh()
                    .build()
                    .execute();

            spotifyApi.setAccessToken(refreshTokenCredentials.getAccessToken());
            // Save updated access token
            saveTokens(refreshTokenCredentials.getAccessToken(), spotifyApi.getRefreshToken());
        }
    }

    private static void getCurrentlyPlayingTrack(SpotifyApi spotifyApi) {
        try {
            CurrentlyPlaying currentlyPlaying = spotifyApi.getUsersCurrentlyPlayingTrack()
                    .build()
                    .execute();

            if (currentlyPlaying.getItem() instanceof Track) {
                Track track = (Track) currentlyPlaying.getItem();
                String artistName = track.getArtists()[0].getName();
                String trackName = track.getName();
                System.out.println("Currently playing: " + trackName + " by " + artistName);
                JOptionPane.showMessageDialog(null, "Currently playing: " + trackName + " by " + artistName);
            } else {
                System.out.println("Currently playing item is not a track.");
                JOptionPane.showMessageDialog(null, "Currently playing item is not a track.");
            }
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void saveTokens(String accessToken, String refreshToken) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE))) {
            writer.write(accessToken + "\n");
            writer.write(refreshToken + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] loadTokens() {
        File file = new File(TOKEN_FILE);
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String accessToken = reader.readLine();
            String refreshToken = reader.readLine();
            return new String[]{accessToken, refreshToken};
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

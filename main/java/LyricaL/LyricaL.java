package LyricaL;

import java.net.URI;
import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;
import io.github.cdimascio.dotenv.Dotenv;
import se.michaelthelin.spotify.SpotifyApi;
//import se.michaelthelin.spotify.model_objects.specification.CurrentlyPlaying;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;


import org.apache.hc.core5.http.ParseException;
import java.io.IOException;

public class LyricaL {
    public static void main(String[] args) {

        FlatDarkLaf.setup();
        JFrame frame = new JFrame("LyricaL");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Load environment variables
        Dotenv dotenv = Dotenv.load();
        String clientId = dotenv.get("CLIENT_ID");
        String clientSecret = dotenv.get("CLIENT_SECRET");
        String redirectUriString = dotenv.get("REDIRECT_URI");
        URI redirectUri = URI.create(redirectUriString);

        // Initialize Spotify API
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();

        System.out.println("Client ID: " + clientId);
        System.out.println("Redirect URI: " + redirectUri);

        try {
            // Generate the authorization URL
            String authorizationUrl = spotifyApi.authorizationCodeUri()
                    .scope("user-read-currently-playing")
                    .build()
                    .execute().toString();

            System.out.println("Visit this URL to authorize: " + authorizationUrl);

            // Ask the user to input the authorization code
            String code = JOptionPane.showInputDialog("Enter the authorization code: ");

            // Exchange the code for an access token
            String accessToken = spotifyApi.authorizationCode(code)
                    .build()
                    .execute()
                    .getAccessToken();

            // Set the access token
            spotifyApi.setAccessToken(accessToken);

            // Retrieve and display the currently playing song
            getCurrentlyPlayingTrack(spotifyApi);

        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
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
}

package main.java.LyricaL;
import java.net.URI;
import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;
import io.github.cdimascio.dotenv.Dotenv;
import se.michaelthelin.spotify.SpotifyApi;

public class LyricaL {
    public static void main(String[] args) {
        
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("Test");
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

        String authorizationUrl = spotifyApi.authorizationCodeUri()
                .scope("user-read-private,playlist-read-private")
                .build()
                .toString();

        System.out.println("Visit this URL to authorize: " + authorizationUrl);
    }
}
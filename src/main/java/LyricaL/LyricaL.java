package LyricaL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.hc.core5.http.ParseException;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.cdimascio.dotenv.Dotenv;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;

class Event {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private boolean isSet = false;

    // Set the event
    public void set() {
        lock.lock();
        try {
            isSet = true;
            condition.signalAll(); // Notify all waiting threads
        } finally {
            lock.unlock();
        }
    }

    // Clear the event
    public void clear() {
        lock.lock();
        try {
            isSet = false;
        } finally {
            lock.unlock();
        }
    }

    // Wait until the event is set
    public void waitEvent() throws InterruptedException {
        lock.lock();
        try {
            while (!isSet) {
                condition.await(); // Wait until signaled
            }
        } finally {
            lock.unlock();
        }
    }

    // Check if the event is set
    public boolean isSet() {
        lock.lock();
        try {
            return isSet;
        } finally {
            lock.unlock();
        }
    }
}
public class LyricaL {
    private static final String TOKEN_FILE = "spotify_tokens.txt";
    public static String track_id, artist, song_title, line = "";
    public static int current_progress = 0;
    public static Event event = new Event();
    public static void main(String[] args) {
        //String track_id, artist,song_title,line,status = "";
        //int current_progress = 0;
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
            Thread thread = new Thread(() -> monitor_song(spotifyApi));
            thread.start();

        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }
    private static void monitor_song(SpotifyApi spotifyApi) {
        String current_track_id = null;
        while(true){
            try {
                getCurrentlyPlayingTrack(spotifyApi);
                if(track_id != current_track_id){
                    current_track_id = track_id;
                    event.set();
                }
                Thread.sleep(10000);
            } catch (Exception e) {
                System.out.println("There was an error in getting song information.");
            }
            
        }
        

    }

    private static void update_display() {

    }

    private static void fetch_lyrics() {

    }

    private static void update_overlay_text() {

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
                String trackID = track.getId();
                int progress_ms = currentlyPlaying.getProgress_ms();
                int progress_sec = Math.floorDiv(progress_ms, 1000);
                int progress_min = Math.floorDiv(progress_sec,60);
                progress_sec %= 60;
                artist = artistName;
                song_title = trackName;
                track_id = trackID;
                current_progress = progress_min*60+progress_sec;
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

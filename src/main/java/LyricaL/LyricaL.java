package LyricaL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import java.awt.*;

import javax.swing.JLabel;
//import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
//import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

//import org.apache.hc.client5.http.impl.TunnelRefusedException;

import org.apache.hc.core5.http.ParseException;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.cdimascio.dotenv.Dotenv;
import kotlin.jvm.Throws;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;

import py4j.ClientServer;
import py4j.Py4JException;
import py4j.GatewayServer;

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
    public static Event song_change_event = new Event();
    public static Event line_set_event = new Event();
    public static Event lyrics_fetch_event = new Event();
    public static ClientServer clientServer = new ClientServer(null);
    public static final Synced_Lyrics fetcher = (Synced_Lyrics) clientServer.getPythonServerEntryPoint(new Class[] { Synced_Lyrics.class });

    public static void main(String[] args) throws InterruptedException {
        //String track_id, artist,song_title,line,status = "";
        //int current_progress = 0;
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("LyricaL");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        JLabel textArea = new JLabel();
        //frame.add(textArea);
        //textArea.setEditable(false);
        textArea.setFont(new Font("Univers",Font.BOLD,12));
        //textArea.setLineWrap(true);
        //textArea.setWrapStyleWord(true);
        textArea.setHorizontalAlignment(SwingConstants.CENTER);
        textArea.setVerticalAlignment(SwingConstants.CENTER);
        //JScrollPane scrollPane = new JScrollPane(textArea);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e){
                int frameWidth = frame.getWidth();
                int frameHeight = frame.getHeight();

                int newFontSize = Math.min(frameWidth/20,frameHeight/20);
                textArea.setFont(new Font("Univers",Font.BOLD,newFontSize));

            }
        });
        
        frame.add(textArea);
        //frame.getContentPane().add(scrollPane, BorderLayout.CENTER); // Add scrollPane to center
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
            Thread thread = new Thread(() -> monitor_song(spotifyApi,textArea));
            thread.start();
            Thread thread2 = new Thread(() -> update_display());
            thread2.start();
            ProcessBuilder processBuilder = new ProcessBuilder("py","lyrics_server.py");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String linez;
            while ((linez = reader.readLine()) != null) {
                System.out.println(linez);
            }
            //process.waitFor();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }
    private static void monitor_song(SpotifyApi spotifyApi, JLabel textArea) {
        String current_track_id = null;
        while(true){
            try {
                getCurrentlyPlayingTrack(spotifyApi, textArea);
                if(track_id !=null && !track_id.equals("None") && !track_id.equals(current_track_id)){
                    current_track_id = track_id;
                    song_change_event.set();
                }
                Thread.sleep(10000);
            } catch (Exception e) {
                System.out.println("There was an error in getting song information.");
                e.printStackTrace();
            }
            
        }
        

    }

    private static void update_display() {
        while(true){
            try{
                if(lyrics_fetch_event.isSet()){
                    continue;
                }
                fetch_lyrics();
                if(track_id.equals("None")){
                
                } 
                else {
                    update_overlay_text();
                }
            } catch(Exception e){

            }
        }
    }
    public interface Synced_Lyrics{
        String lyrics_search(String track_name, String artist);
    }
    private static void fetch_lyrics() throws InterruptedException {
        if(!song_change_event.isSet() || track_id.equals("None")){
            return;
        }
        //song_change_event.waitEvent();
        song_change_event.clear();
        lyrics_fetch_event.set();
        System.out.println("HELLO I CHANGED SONGS YYIPEEEEEEE");
        
        try{
            String lyrics = fetcher.lyrics_search(song_title,artist);
            System.out.println(lyrics);
            
        }catch (Py4JException e){
            e.printStackTrace();
        } finally {
            lyrics_fetch_event.clear();
        }
        
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

    private static void getCurrentlyPlayingTrack(SpotifyApi spotifyApi, JLabel textArea) {
        try {
            CurrentlyPlaying currentlyPlaying = spotifyApi.getUsersCurrentlyPlayingTrack()
                    .build()
                    .execute();
            
            if (currentlyPlaying!=null &&currentlyPlaying.getItem() instanceof Track) {
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
                //JOptionPane.showMessageDialog(null, "Currently playing: " + trackName + " by " + artistName);
                textArea.setText("Currently playing: " + trackName + " by " + artistName);
            } else {
                System.out.println("Currently playing item is not a track.");
                artist = "None";
                song_title = "None";
                track_id = "None";
                current_progress = 0;
                //JOptionPane.showMessageDialog(null, "Currently playing item is not a track.");
                textArea.setText("Currently playing item is not a track.");
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

package LyricaL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Iterator;
import java.awt.*;
import javax.swing.JMenu;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
//import javax.swing.border.Border;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.*;

import org.apache.hc.core5.http.ParseException;

import com.formdev.flatlaf.FlatDarkLaf;

import io.github.cdimascio.dotenv.Dotenv;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
//import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import py4j.ClientServer;
//import py4j.Py4JException;
import jep.MainInterpreter;
import jep.Interpreter;
import jep.SharedInterpreter;
import java.awt.event.*;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;

import LyricaL.gui.GUI;
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
    static{
        System.setProperty("java.library.path","C:/Users/fhu86/AppData/Local/Programs/Python/Python312/Lib/site-packages/jep");
        MainInterpreter.setJepLibraryPath("C:/Users/fhu86/AppData/Local/Programs/Python/Python312/Lib/site-packages/jep/jep.dll");
    }
    static{
        System.setProperty("java.library.path","C:/Users/jason/AppData/Local/Programs/Python/Python311/Lib/site-packages/jep");
        MainInterpreter.setJepLibraryPath("C:/Users/jason/AppData/Local/Programs/Python/Python311/Lib/site-packages/jep/jep.dll");
    }
    public static GUI guiyay;
    private static final String TOKEN_FILE = "spotify_tokens.txt";
    public static String track_id, artist, song_title, line,linetwo = "";
    public static int current_progress = 0;
    public static Event song_change_event = new Event();
    public static Event line_set_event = new Event();
    public static Event lyrics_fetch_event = new Event();
    //public static ClientServer clientServer = new ClientServer(null);
    //public static final Synced_Lyrics fetcher = (Synced_Lyrics) clientServer.getPythonServerEntryPoint(new Class[] { Synced_Lyrics.class });
    public static class TimeStampedLine{
        double timestamp;
        String lyricsa;
        public TimeStampedLine(double timestamp,String lyricsa){
            this.timestamp = timestamp;
            this.lyricsa = lyricsa;
        }
        @Override
        public String toString() {
            return "Timestamp: " + timestamp + " | Lyrics: " + lyricsa;
        }
    }
    public static ArrayList<TimeStampedLine> lines = new ArrayList<TimeStampedLine>();
    public static GUI getGUI() {
        return guiyay;
    }
    public static void main(String[] args) throws InterruptedException {
        //String track_id, artist,song_title,line,status = "";
        //int current_progress = 0;
        guiyay = new GUI();
        guiyay.Init_GUI();
        
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
            Thread thread = new Thread(() -> monitor_song(spotifyApi,guiyay));
            thread.start();
            Thread thread2 = new Thread(() -> update_display());
            thread2.start();
            Thread thread3 = new Thread(() -> {
                try {
                    main_loop(guiyay);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            });
            thread3.start();
            /*ProcessBuilder processBuilder = new ProcessBuilder("py","lyrics_server.py");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String linez;
            while ((linez = reader.readLine()) != null) {
                System.out.println(linez);
            }
            //process.waitFor();*/
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }
    private static void main_loop(GUI aGUI) throws InterruptedException {
        while(true){
            line_set_event.waitEvent();
            aGUI.setTextArea(line);
            aGUI.setSecondText(linetwo);
            //System.out.println(current_progress);
            line_set_event.clear();

        }
    }
    private static void monitor_song(SpotifyApi spotifyApi, GUI aGUI) {
        String current_track_id = null;
        while(true){
            try {
                getCurrentlyPlayingTrack(spotifyApi, aGUI);
                if(track_id !=null && !track_id.equals("None") && !track_id.equals(current_track_id)){
                    current_track_id = track_id;
                    song_change_event.set();
                }
                Thread.sleep(100);
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
        lines.clear();
        System.out.println("HELLO I CHANGED SONGS YYIPEEEEEEE");
        
        try(Interpreter interp = new SharedInterpreter()){
            //String lyrics = fetcher.lyrics_search(song_title,artist);
            interp.set("song_title",song_title);
            interp.set("artist",artist);
            interp.exec("import syncedlyrics");
            interp.exec("lyrics = syncedlyrics.search(f'{song_title} {artist}',synced_only=True)");
            //lyrics = 
            String lyrics = interp.getValue("lyrics",String.class);
            
            //System.out.println(lyrics);
            //System.out.println();
            
            String linePattern = "\\[(\\d{2}:\\d{2}\\.\\d{2})](.*)";
            Pattern pattern = Pattern.compile(linePattern);
            Matcher matcher = pattern.matcher(lyrics);
            System.out.println(lyrics);
            // Iterate through each match
            while (matcher.find()) {
                String mainTimestamp = matcher.group(1);
                String rawLine = matcher.group(2);  // The line with words between timestamps

                // Extract words from the line (after the < > tags)
                String wordPattern = "<\\d{2}:\\d{2}\\.\\d{2}>\\s*([\\p{L}\\p{N}',?!.]+)";;
                Pattern wordPatternObj = Pattern.compile(wordPattern);
                Matcher wordMatcher = wordPatternObj.matcher(rawLine);
                StringBuilder sb = new StringBuilder();

                // Collect all words in the line
                while (wordMatcher.find()) {
                    sb.append(wordMatcher.group(1)).append(" ");
                }

                String words;
                if (sb.length()>0){
                    words= sb.toString().trim();
                }else{
                    words=rawLine;
                }
                
                // Convert main timestamp to seconds
                String[] parts = mainTimestamp.split(":");
                double minutes = Double.parseDouble(parts[0]);
                double seconds = Double.parseDouble(parts[1]);
                double timestampInSeconds= Math.round((minutes * 60 + seconds)*100.0)/100.0;
                //double timestampInSeconds = convertTimestampToSeconds(mainTimestamp);

                // Add the timestamped lyrics to the list
                
                lines.add(new TimeStampedLine(timestampInSeconds, words));
            }
            /*for (TimeStampedLine linez : lines) {
                System.out.println(linez);
            }*/
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            lyrics_fetch_event.clear();
        }
        
    }

    private static void update_overlay_text() {
        TimeStampedLine nearestLine = null;
        TimeStampedLine secondNearest = null;
        
        for(int i=0;i<lines.size()-1;++i){
            if(lines.get(i).timestamp <= current_progress+.6){
                if(nearestLine == null || lines.get(i).timestamp > nearestLine.timestamp){
                    nearestLine = lines.get(i);
                    secondNearest = lines.get(i+1);
                }
            }
        }

        if(nearestLine!=null && secondNearest!=null){
            line= nearestLine.lyricsa;
            linetwo = secondNearest.lyricsa;
        }else{
            line = lines.isEmpty() ? "" : lines.get(0).lyricsa;
            linetwo = "";
        }
        line_set_event.set();
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

    private static void getCurrentlyPlayingTrack(SpotifyApi spotifyApi, GUI aGUI) {
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
                aGUI.setTextArea("Currently playing: " + trackName + " by " + artistName);
            } else {
                System.out.println("Currently playing item is not a track.");
                artist = "None";
                song_title = "None";
                track_id = "None";
                current_progress = 0;
                //JOptionPane.showMessageDialog(null, "Currently playing item is not a track.");
                aGUI.setTextArea("Currently playing item is not a track.");
                aGUI.setSecondText("");
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

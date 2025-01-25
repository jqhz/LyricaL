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

import py4j.ClientServer;
import py4j.Py4JException;

import java.awt.event.*;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;

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
    public static String track_id, artist, song_title, line,linetwo = "";
    public static int current_progress = 0;
    public static Event song_change_event = new Event();
    public static Event line_set_event = new Event();
    public static Event lyrics_fetch_event = new Event();
    public static ClientServer clientServer = new ClientServer(null);
    public static final Synced_Lyrics fetcher = (Synced_Lyrics) clientServer.getPythonServerEntryPoint(new Class[] { Synced_Lyrics.class });
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
    public static void main(String[] args) throws InterruptedException {
        //String track_id, artist,song_title,line,status = "";
        //int current_progress = 0;
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("LyricaL");
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        frame.setOpacity(0.8f);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setAlwaysOnTop(true);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Options",new JPanel());
        tabbedPane.addTab("Themes",new JPanel());
        tabbedPane.addTab("Keybinds",new JPanel());
        tabbedPane.addTab("Language",new JPanel());
        String[] columnNames = {"Action","Hotkey"};
        Object[][] data = {
            {"Toggle Window UI",""},
            {"Toggle window visibility",""},
            {"Lock",""},
            {"Toggle Transparency",""}
        };
        DefaultTableModel model = new DefaultTableModel(data,columnNames);
        JTable table = new JTable(model);
        
        JPanel contentPanel = new JPanel();
        //contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
        JPanel titleBar = new JPanel();
        titleBar.setBackground(Color.DARK_GRAY);
        //titleBar.setOpaque(false);
        titleBar.setLayout(new FlowLayout(FlowLayout.RIGHT,5,5));
        URL minimizeIconURL = LyricaL.class.getResource("/minimize_icon.png");
        URL closeIconURL = LyricaL.class.getResource("/exit_icon.png");
        URL settingsIconURL = LyricaL.class.getResource("/settings_icon.png");
        //System.out.println(minimizeIconURL);
        //System.out.println(closeIconURL);
        
        if (minimizeIconURL == null || closeIconURL == null) {
            System.err.println("Resource files not found. Ensure minimize_icon.png and exit_icon.png are in the correct directory.");
            System.exit(1);
        }
        ImageIcon minimizeIcon = new ImageIcon(minimizeIconURL);
        ImageIcon closeIcon = new ImageIcon(closeIconURL);
        ImageIcon settingsIcon = new ImageIcon(settingsIconURL);
        JLabel minimizeLabel = new JLabel(minimizeIcon);
        minimizeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                frame.setState(Frame.ICONIFIED); // Minimize the frame
            }
        });

        // Create the close button
        
        JLabel closeLabel = new JLabel(closeIcon);
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(0); // Close the application
            }
        });
        JLabel settingsLabel = new JLabel(settingsIcon);
        settingsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JFrame settingsFrame = new JFrame("Settings");
        settingsFrame.setSize(400,300);
        settingsFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        settingsFrame.setLayout(new BorderLayout());
        settingsFrame.setLocationRelativeTo(null);
        settingsFrame.add(tabbedPane);
        settingsFrame.setResizable(false);
        settingsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //frame.setState(Frame.ICONIFIED); // Minimize the frame
                boolean isVisible = settingsFrame.isVisible();
                settingsFrame.setVisible(!isVisible);
            }
        });
        titleBar.add(settingsLabel);
        titleBar.add(minimizeLabel);
        titleBar.add(closeLabel);
        
        JLabel textArea = new JLabel();
        //frame.add(textArea);
        //textArea.setEditable(false);
        textArea.setFont(new Font("Univers",Font.BOLD,16));
        textArea.setHorizontalAlignment(SwingConstants.CENTER);
        //textArea.setVerticalAlignment(SwingConstants.CENTER);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(textArea);
        contentPanel.add(Box.createVerticalStrut(50));

        JLabel secondText = new JLabel();
        secondText.setHorizontalAlignment(SwingConstants.CENTER);
        secondText.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondText.setFont(new Font("Univers",Font.BOLD,16));

        contentPanel.add(secondText);
        contentPanel.add(Box.createVerticalGlue());
        //frame.add(content)
        //JScrollPane scrollPane = new JScrollPane(textArea);
        /*frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e){
                int frameWidth = frame.getWidth();
                int frameHeight = frame.getHeight();

                int newFontSize = Math.min(frameWidth/20,frameHeight/20);
                textArea.setFont(new Font("Univers",Font.BOLD,newFontSize));

            }
        });*/
        Point dragPoint = new Point();
        frame.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragPoint.setLocation(e.getPoint());
                //contentPanel.setOpaque(true);
            }
        });

        frame.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragPoint != null) {
                    Point currentScreenLocation = e.getLocationOnScreen();
                    frame.setLocation(currentScreenLocation.x - dragPoint.x,
                            currentScreenLocation.y - dragPoint.y);
                }
                //contentPanel.setOpaque(false);
            }
        });
        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });
        frame.add(titleBar,BorderLayout.NORTH);

        //frame.add(textArea);
        frame.add(contentPanel,BorderLayout.CENTER);
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
            Thread thread = new Thread(() -> monitor_song(spotifyApi,textArea,secondText));
            thread.start();
            Thread thread2 = new Thread(() -> update_display());
            thread2.start();
            Thread thread3 = new Thread(() -> {
                try {
                    main_loop(textArea,secondText);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            });
            thread3.start();
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
    private static void main_loop(JLabel textArea,JLabel secondText) throws InterruptedException {
        while(true){
            line_set_event.waitEvent();
            textArea.setText(line);
            secondText.setText(linetwo);
            line_set_event.clear();

        }
    }
    private static void monitor_song(SpotifyApi spotifyApi, JLabel textArea, JLabel secondText) {
        String current_track_id = null;
        while(true){
            try {
                getCurrentlyPlayingTrack(spotifyApi, textArea, secondText);
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
        
        try{
            String lyrics = fetcher.lyrics_search(song_title,artist);
            //System.out.println(lyrics);
            //System.out.println();
            
            String linePattern = "\\[(\\d{2}:\\d{2}\\.\\d{2})](.*)";
            Pattern pattern = Pattern.compile(linePattern);
            Matcher matcher = pattern.matcher(lyrics);

            // Iterate through each match
            while (matcher.find()) {
                String mainTimestamp = matcher.group(1);
                String rawLine = matcher.group(2);  // The line with words between timestamps

                // Extract words from the line (after the < > tags)
                String wordPattern = "<\\d{2}:\\d{2}\\.\\d{2}>\\s*([\\w',?!.]+)";
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
        }catch (Py4JException e){
            e.printStackTrace();
        } finally {
            lyrics_fetch_event.clear();
        }
        
    }

    private static void update_overlay_text() {
        TimeStampedLine nearestLine = null;
        TimeStampedLine secondNearest = null;
        //Iterator<TimeStampedLine> it = lines.iterator();
        /*while (it.hasNext()) {
            TimeStampedLine i = it.next();
            if(i.timestamp <= current_progress+.6){
                if(nearestLine == null || i.timestamp > nearestLine.timestamp){
                    nearestLine = i;
                    secondNearest= it.next();
                }
            }
        }*/
        /*for(TimeStampedLine tsl : lines){
            if(tsl.timestamp <= current_progress+.6){
                if(nearestLine == null || tsl.timestamp > nearestLine.timestamp){
                    nearestLine = tsl;
                }
            }
        }*/
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

    private static void getCurrentlyPlayingTrack(SpotifyApi spotifyApi, JLabel textArea, JLabel secondText) {
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
                secondText.setText("");
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

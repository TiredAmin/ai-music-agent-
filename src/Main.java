/*
Hello , here's how the code works . the user has to input his/her song name , the code then uses Deezers api
to gather the songs info such as its artist , album , duration etc . then it asks the user whether he/she
wants the lyrics , after the user inputs yes the code gets its lyrics from lyrics ovhs api , after that
it asks if the user wants the lyrics to be analyzed , after another yes the code uses the keyword_extraction model
from Eden ai to analyze the lyrics . unfortunately the emotion_detection model from eden ai wasnt free , i also
tried over 20 ai models and that is the ONLY goddamn free ai model api that i could find that works .
i should also mention that since my free credit is only 1$ (and now probably even less) please input a song that has few lyrics
thank you :)
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {
    private static final String deezer_api = "https://api.deezer.com/search?q=";
    private static final String ovh_api = "https://api.lyrics.ovh/v1/";
    private static final String edenai_endpoint = "https://api.edenai.run/v2/text/keyword_extraction";
    private static final String eden_access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNzM0ZGEzZGItMTNlNC00ZGMwLTkyNGItMTU1YTg5MTdmYjAzIiwidHlwZSI6ImFwaV90b2tlbiJ9.oXCDezNykar1GxVWvjyinetiIxN38858mS1PpIuDobc";
    // the token has less than 1$ in credit
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the song name: ");
        String songName = scanner.nextLine();
        try {
            fetchSongInfo(songName, scanner);
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
    private static void fetchSongInfo(String songName, Scanner scanner) throws IOException {
        String encodedSongName = URLEncoder.encode(songName, "UTF-8");
        String apiUrl = deezer_api + encodedSongName;
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            getsonginfo(response.toString(), scanner);
        } else {
            System.out.println("Failed to fetch data from Deezer API. Response code: " + responseCode);
        }
    }
    private static void getsonginfo(String jsonResponse, Scanner scanner) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray dataArray = jsonObject.getJSONArray("data");
        if (dataArray.length() == 0) {
            System.out.println("No songs found matching your query.");
            return;
        }
        JSONObject song = dataArray.getJSONObject(0);
        String title = song.getString("title");
        String artist = song.getJSONObject("artist").getString("name");
        String album = song.getJSONObject("album").getString("title");
        int duration = song.getInt("duration");
        String previewUrl = song.getString("preview");
        String link = song.getString("link");
        System.out.println("\n--- Song Info ---");
        System.out.println("Song Title: " + title);
        System.out.println("Artist: " + artist);
        System.out.println("Album: " + album);
        System.out.println("Duration: " + duration(duration));
        System.out.println("Preview URL: " + previewUrl);
        System.out.println("Deezer Link: " + link);
        System.out.print("\nWould you like to see the lyrics? (yes/no): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        if (answer.equals("yes")) {
            String lyrics = getlyrics(artist, title);
            if (lyrics != null) {
                System.out.println("\n--- Lyrics ---\n" + lyrics);

                System.out.print("\nWould you like to get the lyrics analysis ? (yes/no): ");
                String analyzeAnswer = scanner.nextLine().trim().toLowerCase();
                if (analyzeAnswer.equals("yes")) {
                    getanalysis(lyrics);
                }
            }
        }
    }
    private static String getlyrics(String artist, String title) {
        try {
            String encodedArtist = URLEncoder.encode(artist, "UTF-8");
            String encodedTitle = URLEncoder.encode(title, "UTF-8");
            String lyricsUrl = ovh_api + encodedArtist + "/" + encodedTitle;
            HttpURLConnection connection = (HttpURLConnection) new URL(lyricsUrl).openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject jsonObject = new JSONObject(response.toString());
                if (jsonObject.has("lyrics")) {
                    return jsonObject.getString("lyrics");
                } else {
                    System.out.println("Lyrics not found.");
                }
            } else {
                System.out.println("Failed to fetch lyrics. Response code: " + responseCode);
            }
        } catch (IOException e) {
            System.out.println("An error occurred while fetching lyrics: " + e.getMessage());
        }
        return null;
    }
    private static void getanalysis(String lyrics) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("providers", "amazon,microsoft");
            payload.put("language", "en");
            payload.put("text", lyrics);
            byte[] postData = payload.toString().getBytes("UTF-8");
            URL url = new URL(edenai_endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + eden_access_token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData);
                os.flush();
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseSB = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseSB.append(line);
                }
                reader.close();
                JSONObject responseJson = new JSONObject(responseSB.toString());
                if (responseJson.has("amazon")) {
                    JSONObject amazonResult = responseJson.getJSONObject("amazon");
                    if (amazonResult.has("items")) {
                        JSONArray items = amazonResult.getJSONArray("items");
                        System.out.println("\n--- Keyword Extraction (amazon) ---");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject keywordObj = items.getJSONObject(i);
                            double importance = keywordObj.getDouble("importance");
                            String keyword = keywordObj.getString("keyword");
                            // Print without curly braces or quotes
                            System.out.printf("Importance: %.2f, Keyword: %s%n", importance, keyword);
                        }
                    } else {
                        System.out.println("No keyword items found in the amazon response.");
                    }
                } else {
                    System.out.println("The Eden API response did not include an 'amazon' key. Full response:");
                    System.out.println(responseJson.toString(2));
                }
            } else {
                System.out.println("Failed to extract keywords with Eden API. Response code: " + responseCode);
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                System.out.println("Error details: " + errorResponse);
            }
        } catch (IOException e) {
            System.out.println("An error occurred while extracting keywords: " + e.getMessage());
        }
    }

    private static String duration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
}

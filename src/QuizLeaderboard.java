import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class QuizLeaderboard {

    static final String BASE = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    static final String REG = "RA2311003010438";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new LinkedHashMap<>();

        for (int i = 0; i <= 9; i++) {
            String url = BASE + "/quiz/messages?regNo=" + REG + "&poll=" + i;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            System.out.println("Poll " + i + " → " + res.body());
            parseAndMerge(res.body(), seen, scores);

            if (i < 9) Thread.sleep(5000);
        }

        List<Map.Entry<String, Integer>> board = new ArrayList<>(scores.entrySet());
        board.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder lb = new StringBuilder("[");
        for (int i = 0; i < board.size(); i++) {
            lb.append("{\"participant\":\"").append(board.get(i).getKey())
              .append("\",\"totalScore\":").append(board.get(i).getValue()).append("}");
            if (i < board.size() - 1) lb.append(",");
        }
        lb.append("]");

        String body = "{\"regNo\":\"" + REG + "\",\"leaderboard\":" + lb + "}";
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/quiz/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> submitRes = client.send(post, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nSubmit response → " + submitRes.body());
    }

    static void parseAndMerge(String json, Set<String> seen, Map<String, Integer> scores) {
        // pull out events array content
        int start = json.indexOf("\"events\"");
        if (start == -1) return;
        int arrStart = json.indexOf("[", start);
        int arrEnd = json.lastIndexOf("]");
        if (arrStart == -1 || arrEnd == -1) return;

        String events = json.substring(arrStart + 1, arrEnd);
        // split on }, {
        String[] items = events.split("\\},\\s*\\{");

        for (String item : items) {
            String roundId = extract(item, "roundId");
            String participant = extract(item, "participant");
            String scoreStr = extract(item, "score");
            if (roundId == null || participant == null || scoreStr == null) continue;

            String key = roundId + "|" + participant;
            if (seen.contains(key)) {
                System.out.println("  [dup] skipping " + key);
                continue;
            }
            seen.add(key);
            int score = Integer.parseInt(scoreStr.trim());
            scores.merge(participant, score, Integer::sum);
        }
    }

    static String extract(String s, String field) {
        String search = "\"" + field + "\"";
        int i = s.indexOf(search);
        if (i == -1) return null;
        int colon = s.indexOf(":", i);
        if (colon == -1) return null;
        String after = s.substring(colon + 1).trim();
        if (after.startsWith("\"")) {
            int end = after.indexOf("\"", 1);
            return after.substring(1, end);
        } else {
            int end = 0;
            while (end < after.length() && (Character.isDigit(after.charAt(end)) || after.charAt(end) == '-')) end++;
            return after.substring(0, end);
        }
    }
}

package org.comparison.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import java.io.FileWriter;
import java.io.IOException;


public class LLMComparison {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String geminiApiKey = dotenv.get("GEMINI_API_KEY");
        String groqApiKey = dotenv.get("GROQ_API_KEY"); // Changed from OpenAI

        String prompt = "Explain quantum computing to a 10-year-old";

        String geminiResponse = callGeminiAPI(geminiApiKey, prompt);
        String groqResponse = callGroqAPI(groqApiKey, prompt); // Changed from OpenAI

        saveComparisonReport(geminiResponse, groqResponse);
    }

    private static String callGeminiAPI(String apiKey, String prompt) {
        try {
            String jsonRequest = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + prompt + "\" } ] } ] }";

            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey)
                    .post(RequestBody.create(jsonRequest, MediaType.parse("application/json")))
                    .build();

            Response response = client.newCall(request).execute();
            String body = response.body().string();

            JsonNode root = mapper.readTree(body);

            if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                JsonNode firstCandidate = root.get("candidates").get(0);
                if (firstCandidate.has("content") &&
                        firstCandidate.get("content").has("parts") &&
                        firstCandidate.get("content").get("parts").size() > 0) {
                    return firstCandidate.get("content").get("parts").get(0).path("text").asText();
                }
            }

            System.err.println("[Gemini Warning] Unexpected response: " + body);
            return "Gemini returned an unexpected response format.";

        } catch (IOException e) {
            e.printStackTrace();
            return "Error calling Gemini API.";
        }
    }

    private static String callGroqAPI(String apiKey, String prompt) {
        try {
            String jsonRequest = "{ \"model\": \"llama-3.3-70b-versatile\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}] }";


            Request request = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonRequest, MediaType.parse("application/json")))
                    .build();

            Response response = client.newCall(request).execute();
            String body = response.body().string();

            JsonNode root = mapper.readTree(body);

            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode messageNode = root.get("choices").get(0).path("message");
                if (messageNode.has("content")) {
                    return messageNode.get("content").asText();
                }
            }

            System.err.println("[Groq Warning] Unexpected response: " + body);
            return "Groq returned an unexpected response format.";

        } catch (IOException e) {
            e.printStackTrace();
            return "Error calling Groq API.";
        }
    }

    private static void saveComparisonReport(String geminiOutput, String groqOutput) {
        try (FileWriter writer = new FileWriter("llm_comparison_report.md")) {
            writer.write("# LLM Comparison Report\n\n");
            writer.write("## Prompt\n");
            writer.write("Explain quantum computing to a 10-year-old\n");
            writer.write("## Gemini Response\n");
            writer.write(geminiOutput + "\n\n");
            writer.write("## Groq Response\n");
            writer.write(groqOutput + "\n");
            System.out.println("✅ Comparison report saved to llm_comparison_report.md");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

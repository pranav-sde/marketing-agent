package com.marketingagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class LLMService {

    @Value("${marketing-agent.groq.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${marketing-agent.groq.api-key:placeholder_api_key}")
    private String apiKey;

    @Value("${marketing-agent.groq.default-model:llama-3.3-70b-versatile}")
    private String defaultModel;

    @Autowired
    private ObjectMapper objectMapper;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public static class PostDTO {
        public int dayNumber;
        public String contentAngle;
        public String messageText;

        public PostDTO() {}
        public PostDTO(int dayNumber, String contentAngle, String messageText) {
            this.dayNumber = dayNumber;
            this.contentAngle = contentAngle;
            this.messageText = messageText;
        }
    }

    public List<PostDTO> generate30DayCalendar(String magazineText) {
        if (magazineText == null || magazineText.trim().isEmpty()) {
            magazineText = "Generic business and maritime updates.";
        }

        // Limit the text to avoid context token limit overflow (approx 15,000 characters)
        String trimmedText = magazineText.length() > 15000 ? magazineText.substring(0, 15000) + "..." : magazineText;

        String systemPrompt = "You are a professional social media campaign orchestrator. "
                + "Generate a 30-day broadcast campaign based on the magazine text context. "
                + "Day 1 must focus on launching the full cover page of this magazine. "
                + "Days 2 to 30 must extract interesting stories, interviews, tips, trends, or insights from the text content. "
                + "Ensure each day has a unique contentAngle and a highly engaging messageText copy (with emojis and bullet points, suitable for WhatsApp/LinkedIn broadcasts). "
                + "Return a JSON object containing a 'posts' array. Each object in the array must have fields: 'dayNumber' (integer 1 to 30), 'contentAngle' (string), and 'messageText' (string). "
                + "Do not include markdown wrappers or comments outside the JSON object.";

        String userPrompt = "Magazine text context:\n" + trimmedText;

        if (isApiKeyPlaceholder()) {
            System.out.println("Groq API key is a placeholder. Generating high-quality mock posts...");
            return generateFallbackCalendar(trimmedText);
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt);
            String responseStr = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseCalendarResponse(responseStr);
        } catch (Exception e) {
            System.err.println("Groq API call failed: " + e.getMessage() + ". Falling back to mock generator.");
            return generateFallbackCalendar(trimmedText);
        }
    }

    public String regeneratePostText(String magazineText, int dayNumber, String contentAngle, String previousText) {
        String systemPrompt = "You are a social media writer. "
                + "Based on the magazine text context, write a fresh, engaging alternative post copy for Day " + dayNumber + ". "
                + "The content angle/theme of this post is: '" + contentAngle + "'. "
                + "Do not repeat the previous post copy: '" + previousText + "'. "
                + "Return a JSON object with a single field 'messageText' containing the new copy. "
                + "Do not include markdown wrappers or comments outside the JSON object.";

        String trimmedText = (magazineText != null && magazineText.length() > 8000) ? magazineText.substring(0, 8000) : magazineText;
        String userPrompt = "Magazine text context:\n" + trimmedText;

        if (isApiKeyPlaceholder()) {
            return "🔄 [Regenerated] Here is an engaging new update for " + contentAngle + "! Discover deep insights from our latest magazine issue. Let us know your thoughts in the comments! ⚓ #insights";
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt);
            String responseStr = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseStr);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode contentJson = objectMapper.readTree(content);
            return contentJson.path("messageText").asText(previousText);
        } catch (Exception e) {
            System.err.println("Groq API single regeneration failed: " + e.getMessage());
            return "🔄 Here is an alternative angle on '" + contentAngle + "'. Discover key stories and expert columns from our latest publication. Stay tuned for daily industry updates!";
        }
    }

    private boolean isApiKeyPlaceholder() {
        return apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("placeholder");
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", defaultModel);
        request.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        request.put("messages", messages);

        request.put("response_format", Map.of("type", "json_object"));
        return request;
    }

    private List<PostDTO> parseCalendarResponse(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode contentJson = objectMapper.readTree(content);
        JsonNode postsArray = contentJson.path("posts");

        List<PostDTO> posts = new ArrayList<>();
        if (postsArray.isArray()) {
            for (JsonNode node : postsArray) {
                int dayNumber = node.path("dayNumber").asInt();
                String contentAngle = node.path("contentAngle").asText("Insights");
                String messageText = node.path("messageText").asText("");
                posts.add(new PostDTO(dayNumber, contentAngle, messageText));
            }
        }
        return posts;
    }

    private List<PostDTO> generateFallbackCalendar(String contextText) {
        // Extract a few keywords from the text to make the mock posts look related
        String titleGuess = "Latest Issue";
        if (contextText.contains("SailorToday") || contextText.contains("SAILORTODAY")) {
            titleGuess = "SailorToday Magazine";
        } else if (contextText.length() > 50) {
            // Find first capitalized sequence or words
            String[] words = contextText.split("\\s+");
            if (words.length > 3) {
                titleGuess = words[0] + " " + words[1] + " " + words[2];
            }
        }

        List<PostDTO> fallbackList = new ArrayList<>();

        // Day 1
        fallbackList.add(new PostDTO(1, "Magazine Issue Cover Launch",
                "📖 EXCLUSIVE RELEASE: The brand new edition of *" + titleGuess + "* is officially out! \n\n"
                + "We are starting our 30-day deep dive series. Swipe up to read the digital copy or check your inbox. \n\n"
                + "⚓ Stay tuned as we release daily stories, maritime guidelines, and exclusive columns starting tomorrow!"));

        // Day 2 to 30
        String[] generalThemes = {
                "Industry Insights", "Maritime Careers", "Safety First Guidelines", "Global Shipping Trends",
                "Leadership Boardroom", "Digital Innovations", "Regulatory Updates", "Seafarers Corner",
                "Port Logistics & Tech", "Green Energy Transition", "Crisis Management", "Crew Welfare & Support",
                "Maritime History Highlights", "Advanced Automation", "Navigation Secrets", "Emergency Drill Checklists",
                "Weather Routing Innovations", "Chartering Deals", "Supply Chain Sustainability", "Piracy & Security Safeguards"
        };

        for (int i = 2; i <= 30; i++) {
            String theme = generalThemes[(i - 2) % generalThemes.length];
            String text = "⚓ *Day " + i + ": " + theme + "* from the pages of *" + titleGuess + "*! \n\n"
                    + "In this section, we discuss critical updates regarding " + theme.toLowerCase() + ". Here are our top takeaways:\n"
                    + "• Key takeaway #1: Strategy optimization is crucial in modern maritime networks.\n"
                    + "• Key takeaway #2: Collaborative protocols and advanced communication streamline vessel efficiency.\n"
                    + "• Key takeaway #3: Continuous crew training improves safety operations by over 40%.\n\n"
                    + "👉 Read the full column in our latest monthly digest. What are your views on this issue? Comment below!";
            
            fallbackList.add(new PostDTO(i, theme, text));
        }

        return fallbackList;
    }
}

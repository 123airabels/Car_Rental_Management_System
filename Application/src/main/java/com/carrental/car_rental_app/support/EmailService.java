package com.carrental.car_rental_app.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends the account-confirmation email through the Brevo email API (free: 300 emails/day, no domain needed).
 * It is sent in the background, so sign-up does not wait for the email. With no BREVO_API_KEY set, nothing is sent
 * and the confirmation link is printed in the application log instead.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey, from, fromName, baseUrl, frontendUrl;

    public EmailService(@Value("${app.mail.brevo-api-key:}") String apiKey,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:Byahe Rentals}") String fromName,
                        @Value("${app.mail.brevo-url:https://api.brevo.com/v3}") String baseUrl,
                        @Value("${app.frontend.base-url}") String frontendUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from == null ? "" : from.trim();
        this.fromName = fromName; this.baseUrl = baseUrl;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public boolean isConfigured() { return !apiKey.isEmpty() && !from.isEmpty(); }

    /** Runs once at startup and prints whether email will work, so a wrong setup is obvious straight away. */
    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        if (apiKey.isEmpty() || from.isEmpty()) {
            log.warn("MAIL: BREVO_API_KEY and/or MAIL_FROM is not set, so confirmation emails are NOT sent. The confirmation link is printed here instead.");
            return;
        }
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/account"))
                    .timeout(Duration.ofSeconds(10)).header("api-key", apiKey).header("accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 200) log.info("MAIL: Brevo API key is valid. Confirmation emails will be sent from {}.", from);
            else log.error("MAIL: Brevo rejected the API key (HTTP {}). Create a new key in Brevo under SMTP & API > API Keys and set BREVO_API_KEY.", r.statusCode());
        } catch (Exception e) {
            log.error("MAIL: could not reach Brevo ({}). Check your internet connection.", e.toString());
        }
    }

    /** @return true if the email was handed to the background sender; false if email is not set up (the link is logged instead). */
    public boolean sendVerification(String toEmail, String name, String token) {
        String link = frontendUrl + "/verify.html?token=" + token;
        if (!isConfigured()) {
            log.warn("[EMAIL NOT CONFIGURED] Set BREVO_API_KEY and MAIL_FROM to send real emails. Confirmation link for {}: {}", toEmail, link);
            return false;
        }
        String safeName = HtmlUtils.htmlEscape(name);
        String html = "<p>Hi " + safeName + ",</p><p>Thanks for signing up with Byahe Rentals. Confirm your email address to activate your account:</p>"
                + "<p><a href=\"" + link + "\" style=\"background:#14213D;color:#fff;padding:10px 18px;border-radius:4px;text-decoration:none;font-weight:600\">Confirm my email</a></p>"
                + "<p>Or paste this link into your browser:<br>" + link + "</p>"
                + "<p style=\"color:#666;font-size:12px\">This link expires soon. If you did not create this account, you can ignore this email.</p>";
        String text = "Hi " + name + ",\n\nThanks for signing up with Byahe Rentals. Confirm your email address to activate your account:\n\n"
                + link + "\n\nThis link expires soon. If you did not create this account, you can ignore this email.";
        String body = "{\"sender\":{\"name\":" + js(fromName) + ",\"email\":" + js(from) + "},"
                + "\"to\":[{\"email\":" + js(toEmail) + ",\"name\":" + js(name) + "}],"
                + "\"subject\":" + js("Confirm your Byahe Rentals account") + ","
                + "\"htmlContent\":" + js(html) + ",\"textContent\":" + js(text) + "}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/smtp/email")).timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey).header("content-type", "application/json").header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((r, err) -> {
            if (err != null) { failed(toEmail, link, err.toString()); return; }
            if (r.statusCode() / 100 == 2) log.info("MAIL: confirmation email sent to {}", toEmail);
            else failed(toEmail, link, "HTTP " + r.statusCode() + " " + r.body()
                    + (r.statusCode() == 401 ? " (wrong BREVO_API_KEY)" : r.statusCode() == 400 ? " (is MAIL_FROM a verified sender in Brevo?)" : ""));
        });
        return true;
    }

    private void failed(String toEmail, String link, String why) {
        log.error("MAIL: could not send the confirmation email to {}: {}", toEmail, why);
        log.warn("Because sending failed, here is the confirmation link for {} (local testing only): {}", toEmail, link);
    }

    /** Minimal JSON string escaping, so no extra library is needed. */
    private static String js(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }
}

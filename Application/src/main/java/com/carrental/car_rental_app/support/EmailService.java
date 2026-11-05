package com.carrental.car_rental_app.support;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Collections;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends the account-confirmation and password-reset emails. Two ways, chosen by what is configured:
 *  1. Gmail (or any SMTP server): set MAIL_USERNAME and MAIL_PASSWORD (a Gmail App Password). Used first when both are set.
 *  2. The Brevo email API: set BREVO_API_KEY and MAIL_FROM.
 * It is sent in the background, so sign-up does not wait for the email. With neither set, nothing is sent
 * and the link is printed in the application log instead.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey, from, fromName, baseUrl, frontendUrl;
    private final JavaMailSender smtp;      // null unless a username and password are set
    private final String smtpUser;

    public EmailService(ObjectProvider<JavaMailSender> smtpSender,
                        @Value("${spring.mail.username:}") String smtpUser,
                        @Value("${spring.mail.password:}") String smtpPassword,
                        @Value("${app.mail.brevo-api-key:}") String apiKey,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:Byahe Rentals}") String fromName,
                        @Value("${app.mail.brevo-url:https://api.brevo.com/v3}") String baseUrl,
                        @Value("${app.frontend.base-url}") String frontendUrl) {
        this.smtpUser = smtpUser == null ? "" : smtpUser.trim();
        this.smtp = !this.smtpUser.isEmpty() && smtpPassword != null && !smtpPassword.isBlank() ? smtpSender.getIfAvailable() : null;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from == null ? "" : from.trim();
        this.fromName = fromName; this.baseUrl = baseUrl;
        String url = frontendUrl == null ? "" : frontendUrl.trim();
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        // A link that says localhost / 127.0.0.1 only opens on the computer running the system. When that is what is configured,
        // swap in this computer's network (LAN) address so the link also works on a phone or another computer on the same network.
        // For access from outside the network, set FRONTEND_URL to a public address (a tunnel such as ngrok, or your domain).
        if (url.matches("(?i)^https?://(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0)(:\\d+)?(/.*)?$")) {
            String lan = lanAddress();
            if (lan != null) url = url.replaceFirst("(?i)(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0)", lan);
        }
        this.frontendUrl = url;
    }

    /** The first non-loopback IPv4 address of this computer (for example 192.168.1.20), or null if there is none. */
    private static String lanAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
            }
        } catch (Exception ignored) { }
        return null;
    }

    public boolean isConfigured() { return smtp != null || (!apiKey.isEmpty() && !from.isEmpty()); }

    /** Gmail only sends as the logged-in account, so the From address is the SMTP username unless MAIL_FROM is a real alias of it. */
    private String smtpFrom() { return from.isEmpty() || from.toLowerCase().endsWith("@gmail.com") && !from.equalsIgnoreCase(smtpUser) ? smtpUser : from; }

    /** Runs once at startup and prints whether email will work, so a wrong setup is obvious straight away. */
    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        if (smtp != null) {
            try {
                if (smtp instanceof JavaMailSenderImpl impl) impl.testConnection();
                log.info("MAIL: connected to the SMTP server. Emails will be sent from {}. Links in emails open {}", smtpFrom(), frontendUrl);
            } catch (Exception e) {
                log.error("MAIL: could not log in to the SMTP server ({}). For Gmail use an App Password (Google Account > Security > App passwords), not your normal password, and turn on 2-Step Verification first.", e.toString());
            }
            return;
        }
        if (apiKey.isEmpty() || from.isEmpty()) {
            log.warn("MAIL: no email is set up, so confirmation emails are NOT sent. Set MAIL_USERNAME and MAIL_PASSWORD (Gmail App Password), or BREVO_API_KEY and MAIL_FROM. The confirmation link is printed here instead.");
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
        String html = "<p>Hi " + HtmlUtils.htmlEscape(name) + ",</p><p>Thanks for signing up with Byahe Rentals. Confirm your email address to activate your account:</p>"
                + button(link, "Confirm my email")
                + "<p>Or paste this link into your browser:<br>" + link + "</p>"
                + "<p style=\"color:#666;font-size:12px\">This link expires soon. If you did not create this account, you can ignore this email.</p>";
        String text = "Hi " + name + ",\n\nThanks for signing up with Byahe Rentals. Confirm your email address to activate your account:\n\n"
                + link + "\n\nThis link expires soon. If you did not create this account, you can ignore this email.";
        return deliver(toEmail, name, "Confirm your Byahe Rentals account", html, text, link, "confirmation");
    }

    /** Same delivery rules as {@link #sendVerification}; the link opens reset-password.html. */
    public boolean sendPasswordReset(String toEmail, String name, String token) {
        String link = frontendUrl + "/reset-password.html?token=" + token;
        String html = "<p>Hi " + HtmlUtils.htmlEscape(name) + ",</p><p>We received a request to reset your Byahe Rentals password. Choose a new one here:</p>"
                + button(link, "Reset my password")
                + "<p>Or paste this link into your browser:<br>" + link + "</p>"
                + "<p style=\"color:#666;font-size:12px\">This link works once and expires in about an hour. If you did not ask for this, you can ignore this email; your password has not changed.</p>";
        String text = "Hi " + name + ",\n\nWe received a request to reset your Byahe Rentals password. Choose a new one here:\n\n"
                + link + "\n\nThis link works once and expires in about an hour. If you did not ask for this, ignore this email; your password has not changed.";
        return deliver(toEmail, name, "Reset your Byahe Rentals password", html, text, link, "password-reset");
    }

    private static String button(String link, String label) {
        return "<p><a href=\"" + link + "\" style=\"background:#14213D;color:#fff;padding:10px 18px;border-radius:4px;text-decoration:none;font-weight:600\">" + label + "</a></p>";
    }

    private boolean deliver(String toEmail, String name, String subject, String html, String text, String link, String what) {
        if (!isConfigured()) {
            log.warn("[EMAIL NOT CONFIGURED] Set MAIL_USERNAME and MAIL_PASSWORD (Gmail App Password), or BREVO_API_KEY and MAIL_FROM, to send real emails. The {} link for {}: {}", what, toEmail, link);
            return false;
        }
        if (smtp != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    MimeMessage m = smtp.createMimeMessage();
                    MimeMessageHelper h = new MimeMessageHelper(m, true, "UTF-8");
                    h.setFrom(smtpFrom(), fromName);
                    h.setTo(toEmail);
                    h.setSubject(subject);
                    h.setText(text, html);
                    smtp.send(m);
                    log.info("MAIL: {} email sent to {}", what, toEmail);
                } catch (Exception e) {
                    failed(what, toEmail, link, e.toString());
                }
            });
            return true;
        }
        String body = "{\"sender\":{\"name\":" + js(fromName) + ",\"email\":" + js(from) + "},"
                + "\"to\":[{\"email\":" + js(toEmail) + ",\"name\":" + js(name) + "}],"
                + "\"subject\":" + js(subject) + ","
                + "\"htmlContent\":" + js(html) + ",\"textContent\":" + js(text) + "}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/smtp/email")).timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey).header("content-type", "application/json").header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((r, err) -> {
            if (err != null) { failed(what, toEmail, link, err.toString()); return; }
            if (r.statusCode() / 100 == 2) log.info("MAIL: {} email sent to {}", what, toEmail);
            else failed(what, toEmail, link, "HTTP " + r.statusCode() + " " + r.body()
                    + (r.statusCode() == 401 ? " (wrong BREVO_API_KEY)" : r.statusCode() == 400 ? " (is MAIL_FROM a verified sender in Brevo?)" : ""));
        });
        return true;
    }

    private void failed(String what, String toEmail, String link, String why) {
        log.error("MAIL: could not send the {} email to {}: {}", what, toEmail, why);
        log.warn("Because sending failed, here is the {} link for {} (local testing only): {}", what, toEmail, link);
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

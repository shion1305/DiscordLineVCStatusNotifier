package com.shion1305.discord.lineNotifier;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@WebServlet(urlPatterns = "/lineBot")
public class LineEndpoint extends HttpServlet {
    Logger logger = Logger.getLogger("LineBotEndpoint");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (InputStream stream = req.getInputStream()) {
            logger.info("Endpoint Received Information: " + new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        resp.setStatus(200);
    }
}

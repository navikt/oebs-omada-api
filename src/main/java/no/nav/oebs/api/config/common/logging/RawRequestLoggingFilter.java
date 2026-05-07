package no.nav.oebs.api.config.common.logging;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Provider
@Priority(Priorities.USER)
public class RawRequestLoggingFilter implements ContainerRequestFilter {

    private static final int MAX_BODY_LENGTH = 10_000;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String method = ctx.getMethod();
        if (!hasBody(method) || !ctx.hasEntity()) return;

        byte[] body = ctx.getEntityStream().readAllBytes();

        String bodyStr = new String(body, StandardCharsets.UTF_8);
        if (bodyStr.length() > MAX_BODY_LENGTH) {
            bodyStr = bodyStr.substring(0, MAX_BODY_LENGTH) + "... [avkortet]";
        }

        String safeMethod = sanitizeForLog(method);
        String safePath = sanitizeForLog(ctx.getUriInfo().getRequestUri().getPath());
        String safeBody = sanitizeForLog(bodyStr);

        log.info("[RåRequest] {} {} råBody={}",
                safeMethod,
                safePath,
                safeBody);

        // Sett streamen tilbake så SCIMple kan lese den
        ctx.setEntityStream(new ByteArrayInputStream(body));
    }

    private boolean hasBody(String method) {
        return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method);
    }

    private String sanitizeForLog(String value) {
        if (value == null) return null;
        return value
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replaceAll("\\p{Cntrl}", "_");
    }
}


package com.soulpal.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogController {

    @PostMapping("/error")
    public ResponseEntity<Void> receiveError(@RequestBody Map<String, Object> body) {
        String message  = str(body, "message");
        String source   = str(body, "source");
        String stack    = str(body, "stack");
        String route    = str(body, "route");
        String ua       = str(body, "userAgent");

        log.error("[FRONTEND] {} | route={} | source={} | ua={}\n{}",
                message, route, source, ua, stack);

        return ResponseEntity.noContent().build();
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}

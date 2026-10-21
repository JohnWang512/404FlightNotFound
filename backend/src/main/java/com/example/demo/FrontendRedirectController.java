package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/")
public class FrontendRedirectController {

    /**
     * When running in dev mode, users may hit the backend port (8080) while expecting
     * the frontend (5173) to serve the SPA. Redirect to the Vite dev server.
     */
    @GetMapping("api-explorer")
    public ResponseEntity<Void> redirectToFrontend() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("http://localhost:5173/api-explorer"))
                .build();
    }
}

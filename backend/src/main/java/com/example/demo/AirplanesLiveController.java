package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/live")
public class AirplanesLiveController {

    @Autowired
    private AirplanesLiveService airplanesLiveService;

    // Get aircraft by registration (tail number)
    @GetMapping("/reg/{reg}")
    public ResponseEntity<String> getByReg(@PathVariable String reg) {
        return airplanesLiveService.getByReg(reg);
    }

    // Get aircraft by ICAO hex identifier
    @GetMapping("/hex/{hex}")
    public ResponseEntity<String> getByHex(@PathVariable String hex) {
        return airplanesLiveService.getByHex(hex);
    }

    // Get aircraft by callsign
    @GetMapping("/callsign/{callsign}")
    public ResponseEntity<String> getByCallsign(@PathVariable String callsign) {
        return airplanesLiveService.getByCallsign(callsign);
    }

    // Get aircraft near a geographic point
    @GetMapping("/point/{lat}/{lon}/{radius}")
    public ResponseEntity<String> getByPoint(
            @PathVariable double lat,
            @PathVariable double lon,
            @PathVariable int radius) {
        return airplanesLiveService.getByPoint(lat, lon, radius);
    }

    // Get military aircraft
    @GetMapping("/mil")
    public ResponseEntity<String> getMilitary() {
        return airplanesLiveService.getMilitary();
    }

    // Get FAA LADD aircraft
    @GetMapping("/ladd")
    public ResponseEntity<String> getLadd() {
        return airplanesLiveService.getLadd();
    }

    // Get PIA aircraft
    @GetMapping("/pia")
    public ResponseEntity<String> getPia() {
        return airplanesLiveService.getPia();
    }
}

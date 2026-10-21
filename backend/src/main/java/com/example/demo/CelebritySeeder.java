package com.example.demo;

import com.example.demo.entity.Aircraft;
import com.example.demo.entity.Celebrity;
import com.example.demo.repository.AircraftRepository;
import com.example.demo.repository.CelebrityRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CelebritySeeder implements ApplicationRunner {

    private final CelebrityRepository celebrityRepository;
    private final AircraftRepository aircraftRepository;

    public CelebritySeeder(CelebrityRepository celebrityRepository, AircraftRepository aircraftRepository) {
        this.celebrityRepository = celebrityRepository;
        this.aircraftRepository = aircraftRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (celebrityRepository.count() > 0) {
            return;
        }

        Object[][] celebData = {
            {"Taylor Swift",     "Music"},
            {"Elon Musk",        "Tech"},
            {"Jeff Bezos",       "Tech"},
            {"Oprah Winfrey",    "Media"},
            {"Kim Kardashian",   "Entertainment"},
            {"Drake",            "Music"},
            {"Jay-Z",            "Music"},
            {"Bill Gates",       "Tech"},
            {"Mark Zuckerberg",  "Tech"},
            {"LeBron James",     "Sports"},
            {"Floyd Mayweather", "Sports"},
            {"Kylie Jenner",     "Entertainment"},
            {"John Travolta",    "Hollywood"},
            {"Larry Ellison",    "Tech"},
            {"Steven Spielberg", "Hollywood"},
        };

        // tail number, model, index into celebData
        Object[][] aircraftData = {
            {"N898TS", "Dassault Falcon 7X",           0},
            {"N621MM", "Bombardier Challenger 650",     0},
            {"N628TS", "Gulfstream G650ER",             1},
            {"N292JB", "Gulfstream G650ER",             2},
            {"N540OW", "Bombardier Global Express XRS", 3},
            {"N618KK", "Bombardier Global 7500",        4},
            {"N767CJ", "Boeing 767-200",                5},
            {"N450JB", "Bombardier Challenger 850",     6},
            {"N887WM", "Bombardier Global Express",     7},
            {"N72MZ",  "Gulfstream G650ER",             8},
            {"N601LJ", "Gulfstream G280",               9},
            {"N750FM", "Gulfstream G550",               10},
            {"N831KJ", "Bombardier Global 6000",        11},
            {"N950JT", "Boeing 707-138",                12},
            {"N123LE", "Gulfstream V",                  13},
            {"N289SS", "Boeing Business Jet",           14},
        };

        Celebrity[] saved = new Celebrity[celebData.length];
        for (int i = 0; i < celebData.length; i++) {
            Celebrity c = new Celebrity();
            c.setName((String) celebData[i][0]);
            c.setIndustry((String) celebData[i][1]);
            saved[i] = celebrityRepository.save(c);
        }

        for (Object[] row : aircraftData) {
            String tailNumber = (String) row[0];
            if (aircraftRepository.findByTailNumber(tailNumber).isPresent()) {
                continue;
            }
            Aircraft a = new Aircraft();
            a.setTailNumber(tailNumber);
            a.setModel((String) row[1]);
            a.setOwnerId(saved[(int) row[2]].getId());
            a.setCategory("celebrity");
            aircraftRepository.save(a);
        }
    }
}

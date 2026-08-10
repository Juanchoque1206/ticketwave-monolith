package com.ticketwave.config;

import com.ticketwave.domain.*;
import com.ticketwave.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seed(UserRepository userRepository,
                                  EventRepository eventRepository,
                                  VenueRepository venueRepository,
                                  PromotionRepository promotionRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                seedUsers(userRepository, passwordEncoder);

                Venue arena = new Venue();
                arena.setName("National Stadium");
                arena.setCity("Lima");
                arena.setAddress("Av. San Luis");
                arena.setCapacity(50000);
                venueRepository.save(arena);

                Promotion promo = new Promotion();
                promo.setCode("WELCOME10");
                promo.setName("Welcome discount");
                promo.setType(PromotionType.PERCENTAGE);
                promo.setValue(new BigDecimal("10.00"));
                promo.setScope(PromotionScope.NATIONAL);
                promo.setMaxUsage(1000);
                promo.setUsedCount(0);
                promo.setValidFrom(LocalDateTime.now().minusDays(1));
                promo.setValidUntil(LocalDateTime.now().plusDays(90));
                promo.setActive(true);
                promotionRepository.save(promo);

                log.info("Seed data created. admin/admin1234, user/user1234");
            }

            if (eventRepository.count() == 0) {
                seedEvents(eventRepository, venueRepository);
            }
        };
    }

    private void seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setEmail("admin@ticketwave.com");
        admin.setPassword(passwordEncoder.encode("admin1234"));
        admin.setFullName("TicketWave Admin");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        AppUser user = new AppUser();
        user.setUsername("user");
        user.setEmail("user@ticketwave.com");
        user.setPassword(passwordEncoder.encode("user1234"));
        user.setFullName("Regular User");
        user.setCity("Lima");
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    private void seedEvents(EventRepository eventRepository, VenueRepository venueRepository) {
        Venue arena = venueRepository.findAll().stream().findFirst().orElseGet(() -> {
            Venue v = new Venue();
            v.setName("National Stadium");
            v.setCity("Lima");
            v.setAddress("Av. San Luis");
            v.setCapacity(50000);
            return venueRepository.save(v);
        });

        String[] artists = {"Coldplay", "Bad Bunny", "Karol G", "Soda Stereo", "Guns N' Roses", "Rihanna", "Dua Lipa", "Grupo 5", "Arctic Monkeys", "Adele", "K-pop Show", "Rauw Alejandro", "J Balvin", "Shakira", "The Weeknd", "Luis Miguel", "Foo Fighters", "Beyoncé", "Bruno Mars", "Daddy Yankee"};
        String[] cities = {"Lima", "Arequipa", "Cusco", "Trujillo", "Chiclayo", "Piura", "Iquitos", "Tacna", "Huancayo", "Puno"};
        String[] venues = {"National Stadium", "Arena Peru", "Expo Center", "Convention Hall", "Open Air Park", "Gran Teatro", "Plaza de Armas", "Coliseo Norte", "Estadio Monumental", "Auditorio Sur"};
        LocalDateTime baseDate = LocalDateTime.now().plusDays(10);
        for (int i = 0; i < 20; i++) {
            Event e = new Event();
            e.setName(artists[i] + " Live in " + cities[i % cities.length]);
            e.setArtist(artists[i]);
            e.setCity(cities[i % cities.length]);
            e.setVenue(venues[i % venues.length]);
            e.setVenueEntity(arena);
            e.setEventDate(baseDate.plusDays(i * 7L));
            e.setDescription("Concert " + (i + 1) + " - " + artists[i]);
            e.setBasePrice(new BigDecimal(50 + i * 10));
            e.setTotalCapacity(300 + i * 50);
            e.setReservedCount(0);
            e.setStatus(EventStatus.PUBLISHED);
            e.setCreatedAt(LocalDateTime.now());
            eventRepository.save(e);
        }
        log.info("Seeded {} events", 20);
    }
}

package com.zidio.keystone.config;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(
            AppUserRepository users,
            CustomerRepository customers,
            SiteRepository sites,
            PartRepository parts,
            PasswordEncoder enc) {

        return args -> {

            if (users.count() > 0) {
                return;
            }

            // Create demo customer
            Customer c = new Customer();
            c.setName("Meridian Demo Customer");
            c.setEmail("customer@meridian.com");
            c.setPhone("9876543210");
            customers.save(c);

            // Create demo site
            Site site = new Site();
            site.setName("Meridian Tower");
            site.setAddress("Bangalore, Karnataka");
            site.setCustomer(c);
            sites.save(site);

            // Create demo users
            for (Role r : Role.values()) {

                AppUser u = new AppUser();

                u.setEmail(r.name().toLowerCase() + "@keystone.local");

                u.setFullName(
                        r.name().substring(0, 1)
                                + r.name().substring(1).toLowerCase()
                                + " User"
                );

                u.setRole(r);
                u.setPasswordHash(enc.encode("Password@123"));

                if (r == Role.CUSTOMER) {
                    u.setCustomer(c);
                }

                users.save(u);
            }

            // Create demo parts
            for (int i = 1; i <= 5; i++) {

                Part p = new Part();

                p.setName("Demo Part " + i);
                p.setStock(20);

                // BigDecimal is used because Part.unitCost is BigDecimal
                p.setUnitCost(BigDecimal.valueOf(250L * i));

                parts.save(p);
            }
        };
    }
}
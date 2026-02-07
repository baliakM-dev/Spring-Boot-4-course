package com.springframework.spring7restmvc.bootstrap;

import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Customer;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final BeerRepository beerRepository;

    @Override
    public void run(String... args) throws Exception {
        loadBeerData();
        loadCustomerData();
    }

    private void loadBeerData() {
        beerRepository.save(Beer.builder()
                .beerName("Galaxy Cat")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356")
                .price(new BigDecimal("12.99"))
                .quantityOnHand(122)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        beerRepository.save(Beer.builder()
                .beerName("Pilsner")
                .beerStyle(BeerStyle.ALE)
                .upc("98765")
                .price(new BigDecimal("19.50"))
                .quantityOnHand(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        beerRepository.save(Beer.builder()
                .beerName("IPA")
                .beerStyle(BeerStyle.IPA)
                .upc("98765")
                .price(new BigDecimal("16.20"))
                .quantityOnHand(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void loadCustomerData() {
        customerRepository.save(Customer.builder()
                .name("Customer 1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        customerRepository.save(Customer.builder()
                .name("Customer 2")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        customerRepository.save(Customer.builder()
                .name("Customer 3")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

}

package com.springframework.spring7restmvc.bootstrap;

import org.apache.commons.lang3.StringUtils;
import com.springframework.spring7restmvc.dto.beer.BeerCSVRecord;
import com.springframework.spring7restmvc.entities.Beer;
import com.springframework.spring7restmvc.entities.BeerStyle;
import com.springframework.spring7restmvc.entities.Customer;
import com.springframework.spring7restmvc.repositories.BeerRepository;
import com.springframework.spring7restmvc.repositories.CustomerRepository;
import com.springframework.spring7restmvc.services.BeerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Profile("!test")
public class BootstrapData implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final BeerRepository beerRepository;
    private final BeerService beerService;

    @Value("${app.import.beers.enabled:false}")
    boolean enabled;

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) return;
        loadBeerData();
        loadCSVData();
        loadCustomerData();
    }

    private void loadCSVData() throws FileNotFoundException {
        if (beerRepository.count() < 10) {
        File file = ResourceUtils.getFile("classpath:csvdata/beers.csv");

            List<BeerCSVRecord> rec = beerService.importBeers(file);

            rec.forEach(beerCSVRecord -> {
                BeerStyle beerStyle = switch (beerCSVRecord.getStyle()) {
                    case "American Pale Lager" -> BeerStyle.LAGER;
                    case "American Pale Ale (APA)", "American Black Ale", "Belgian Dark Ale", "American Blonde Ale" ->
                            BeerStyle.ALE;
                    case "American IPA", "American Double / Imperial IPA", "Belgian IPA" -> BeerStyle.IPA;
                    case "American Porter" -> BeerStyle.PORTER;
                    case "Oatmeal Stout", "American Stout" -> BeerStyle.STOUT;
                    case "Saison / Farmhouse Ale" -> BeerStyle.SAISON;
                    case "Fruit / Vegetable Beer", "Winter Warmer", "Berliner Weissbier" -> BeerStyle.WHEAT;
                    case "English Pale Ale" -> BeerStyle.PALE_ALE;
                    default -> BeerStyle.PILSNER;
                };
                beerRepository.save(Beer.builder()
                                .beerName(StringUtils.abbreviate(beerCSVRecord.getBeer(), 50))
                                .beerStyle(beerStyle)
                                .price(BigDecimal.TEN)
                                .upc(String.valueOf(beerCSVRecord.getId()))
                                .quantityOnHand(beerCSVRecord.getCount())
                                .build());
            });

        }
    }


    private void loadBeerData() {
        beerRepository.save(Beer.builder()
                .beerName("Galaxy Cat 22")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356")
                .price(new BigDecimal("12.99"))
                .quantityOnHand(122)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        beerRepository.save(Beer.builder()
                .beerName("Pilsner 22")
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

package de.jgh.pricetrend.mailparser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelDetailPriceRepository extends JpaRepository<ModelDetailPrice, Long> {
    List<ModelDetailPrice> findByModel(String model);
}

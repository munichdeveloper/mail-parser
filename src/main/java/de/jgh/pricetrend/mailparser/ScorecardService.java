package de.jgh.pricetrend.mailparser;

import de.jgh.pricetrend.mailparser.model.ModelDetailPrice;
import de.jgh.pricetrend.mailparser.model.ScoreCard;
import de.jgh.pricetrend.mailparser.repo.ModelDetailPriceRepository;
import de.jgh.pricetrend.mailparser.repo.RawEntryRepository;
import de.jgh.pricetrend.mailparser.repo.ScoreCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

@Service
public class ScorecardService {

    @Autowired
    private PercentileCalcService percentileCalcService;

    @Autowired
    private ModelDetailPriceRepository modelDetailPriceRepository;

    @Autowired
    private RawEntryRepository rawEntryRepository;

    @Autowired
    private ScoreCardRepository scoreCardRepository;

    public Object calculateScoring() {
        return calculateScoringInternal(modelDetailPriceRepository.findAll());
    }

    public Object calculateScoringForModel(String model) {
        return calculateScoringInternal(modelDetailPriceRepository.findByModel(model));
    }

    private Object calculateScoringInternal(Iterable<ModelDetailPrice> all) {
        HashMap<String, Double> report = new HashMap<>();

        all.forEach(modelDetailPrice -> {
            Long id = modelDetailPrice.getId();
            String model = modelDetailPrice.getModel();

            Long laufleistung = modelDetailPrice.getLaufleistung();
            if (laufleistung != null) {
                LocalDate erstzulassungRaw = modelDetailPrice.getErstzulassung();
                if (erstzulassungRaw != null) {
                    LocalDateTime erstzulassung = LocalDateTime.of(erstzulassungRaw, LocalTime.MIN);
                    BigDecimal preis = modelDetailPrice.getPreis();
                    CalculatedPercentile mileagePercentile = percentileCalcService.mileagePercentile(model, String.valueOf(modelDetailPrice.getLaufleistung()));
                    CalculatedPercentile registrationPercentile = percentileCalcService.registrationPercentile(model, String.valueOf(modelDetailPrice.getErstzulassung()));
                    CalculatedPercentile pricePercentile = percentileCalcService.pricePercentile(model, String.valueOf(modelDetailPrice.getPreis()));

                    ScoreCard scoreCard = new ScoreCard(id, pricePercentile.getDoubleValue(), registrationPercentile.getDoubleValue(), mileagePercentile.getDoubleValue());
                    scoreCardRepository.save(scoreCard);
                }
            }
        });

        return report;
    }
}
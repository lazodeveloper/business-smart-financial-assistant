package com.bcp.ia.asistent.caller;

import com.bcp.ia.asistent.model.dto.ConsolidationOffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OfferMockService {

    public Flux<ConsolidationOffer> getMockOffers() {
        List<ConsolidationOffer> offers = List.of(
                createOffer("OF-CONSO-24M", List.of("card", "personal"), 50000, 19.9, 24, "No mora >30 días al momento de la solicitud"),
                createOffer("OF-CONSO-36M", List.of("card", "personal", "micro"), 75000, 17.5, 36, "Score > 650 y sin mora activa")
        );
        return Flux.fromIterable(offers);
    }

    private ConsolidationOffer createOffer(String id, List<String> types, double maxBalance, double rate, int term, String cond) {
        ConsolidationOffer offer = new ConsolidationOffer();
        offer.setOfferId(id);
        offer.setProductTypesEligible(types);
        offer.setMaxConsolidatedBalance(BigDecimal.valueOf(maxBalance));
        offer.setNewRatePct(BigDecimal.valueOf(rate));
        offer.setMaxTermMonths(term);
        offer.setConditions(cond);
        return offer;
    }
}

package de.jgh.pricetrend.mailparser;

import javax.persistence.*;

@Entity
public class DetailEntry {
    @EmbeddedId
    private DetailEntryId id;

    private Double price;
    private boolean processed;

    public DetailEntry() {
    }

    public DetailEntry(DetailEntryId id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public DetailEntryId getId() {
        return id;
    }

}

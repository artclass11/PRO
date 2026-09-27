package com.amormagics.dailyledger;

public final class Entry {
    public final long id;
    public String date;
    public String customer;
    public double carats;
    public double rate;

    public Entry(long id, String date, String customer, double carats, double rate) {
        this.id = id;
        this.date = date;
        this.customer = customer;
        this.carats = carats;
        this.rate = rate;
    }

    public double amount() {
        return carats * rate;
    }
}

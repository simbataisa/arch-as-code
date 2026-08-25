package com.techcombank.qe.sut.data;

/**
 * Fixed pool of invented organisation names used by {@link SyntheticDataSeeder}
 * to populate {@code account.party_name}.
 *
 * <p>Every entry here is a wholly invented organisation name -- never a real
 * company and never a person's name -- so the reference SUT's seeded dataset
 * can never be mistaken for, or accidentally contain, PII.
 */
public final class SyntheticNames {

    public static final String[] NAMES = {
        "Aurora Trading",
        "Beacon Holdings",
        "Cascade Freight",
        "Driftwood Logistics",
        "Ember & Finch Partners",
        "Fernbridge Capital",
        "Granite Harbor Foods",
        "Hollow Pine Ventures",
        "Ironvale Manufacturing",
        "Juniper Ridge Traders",
        "Kestrel Bay Shipping",
        "Larkspur Mercantile",
        "Meridian Timber Co",
        "Northwind Provisions",
        "Opal Crescent Group",
        "Pinehaven Distributors",
        "Quarrystone Exports",
        "Ridgeline Commodities",
        "Silverbrook Textiles",
        "Thistledown Grain Co",
    };

    private SyntheticNames() {
    }
}

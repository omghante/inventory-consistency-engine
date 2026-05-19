package com.cascade.delta;

/**
 * SourceType — Classification of inventory update sources.
 *
 * <p>Each source type has an inherent trust bonus reflecting its
 * typical reliability in production environments:</p>
 *
 * <ul>
 *   <li><b>POS_AUTOMATED</b> (0.95) — Barcode scanner at checkout, nearly certain</li>
 *   <li><b>WAREHOUSE_SCANNER</b> (0.90) — Automated shelf/warehouse counter</li>
 *   <li><b>ERP_SYSTEM</b> (0.85) — Enterprise software synchronization</li>
 *   <li><b>MANUAL_ENTRY</b> (0.60) — Human typing a number, error-prone</li>
 *   <li><b>UNKNOWN</b> (0.40) — Source unknown, suspicious</li>
 * </ul>
 */
public enum SourceType {
    POS_AUTOMATED(0.95),
    WAREHOUSE_SCANNER(0.90),
    ERP_SYSTEM(0.85),
    MANUAL_ENTRY(0.60),
    UNKNOWN(0.40);

    private final double trustBonus;

    SourceType(double trustBonus) {
        this.trustBonus = trustBonus;
    }

    public double getTrustBonus() {
        return trustBonus;
    }
}

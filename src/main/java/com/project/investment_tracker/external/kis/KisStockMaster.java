package com.project.investment_tracker.external.kis;

public record KisStockMaster(String stockName, String stockSymbol, Market market) {
    public enum Market {
        KOSPI("kospi_code.mst", 227),
        KOSDAQ("kosdaq_code.mst", 221);

        private final String fileName;
        private final int detailLength;

        Market(String fileName, int detailLength) {
            this.fileName = fileName;
            this.detailLength = detailLength;
        }

        String fileName() {
            return fileName;
        }

        int detailLength() {
            return detailLength;
        }
    }
}

package com.project.investment_tracker.external.kis;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class KisStockMasterParser {
    private static final Charset CP949 = Charset.forName("MS949");
    private static final int NAME_OFFSET = 21;
    private static final int BASIC_LENGTH = 61;
    private static final int MAX_MASTER_BYTES = 8 * 1024 * 1024;

    public List<KisStockMaster> parse(byte[] archive, KisStockMaster.Market market) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(market.fileName())) {
                    byte[] master = zip.readNBytes(MAX_MASTER_BYTES + 1);
                    if (master.length > MAX_MASTER_BYTES) {
                        throw new IOException("마스터파일 크기 제한을 초과했습니다.");
                    }
                    return parseRows(master, market);
                }
            }
            throw new IOException("ZIP에 필요한 마스터파일이 없습니다.");
        } catch (IOException exception) {
            throw new IllegalStateException(market + " 종목 마스터파일을 해석하지 못했습니다.", exception);
        }
    }

    private List<KisStockMaster> parseRows(byte[] master, KisStockMaster.Market market) throws IOException {
        List<KisStockMaster> stocks = new ArrayList<>();
        // 고정 길이는 문자가 아닌 바이트 기준이다. 줄바꿈을 제거한 뒤 이름만 CP949로 해석한다.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(master), StandardCharsets.ISO_8859_1))) {
            String row;
            int lineNumber = 0;
            while ((row = reader.readLine()) != null) {
                lineNumber++;
                if (row.isBlank()) {
                    continue;
                }
                if (row.length() != BASIC_LENGTH + market.detailLength()) {
                    throw new IOException("마스터파일 행 길이가 올바르지 않습니다: " + lineNumber);
                }
                String symbol = row.substring(0, 9).trim();
                // 펀드 등 9자리 상품코드는 현재 주식 검색 대상에서 제외한다.
                if (!symbol.matches("[0-9A-Z]{6}")) {
                    continue;
                }
                byte[] nameBytes = row.substring(NAME_OFFSET, BASIC_LENGTH)
                        .getBytes(StandardCharsets.ISO_8859_1);
                String name = CP949.newDecoder().decode(ByteBuffer.wrap(nameBytes)).toString().strip();
                if (name.isEmpty()) {
                    throw new IOException("종목명이 비어 있습니다: " + lineNumber);
                }
                stocks.add(new KisStockMaster(name, symbol, market));
            }
        }
        if (stocks.isEmpty()) {
            throw new IOException("검색 가능한 종목이 없습니다.");
        }
        return List.copyOf(stocks);
    }
}

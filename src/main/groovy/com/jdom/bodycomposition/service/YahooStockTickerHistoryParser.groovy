package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.bodycomposition.domain.YahooStockTickerData
import com.jdom.util.MathUtil
import com.jdom.util.TimeUtil

/**
 * Created by djohnson on 12/6/14.
 */
class YahooStockTickerHistoryParser {

    final YahooStockTicker ticker
    final String data

    YahooStockTickerHistoryParser(YahooStockTicker ticker, String data) {
        this.ticker = ticker
        this.data = data
    }

    List<YahooStockTickerData> parse() {
        List<YahooStockTickerData> entries = []

        String[] lines = data.split('\n')
        for (int i = lines.length - 1; i > 0; i--) {
            def entry = new YahooStockTickerData(ticker: ticker)

            def line = lines[i]
            String[] fields = line.split(',')

            int idx = 0
            entry.date = TimeUtil.dateFromDashString(fields[idx++])
            entry.open = MathUtil.toMoney(fields[idx++])
            entry.high = MathUtil.toMoney(fields[idx++])
            entry.low = MathUtil.toMoney(fields[idx++])
            entry.close = MathUtil.toMoney(fields[idx++])
            entry.volume = Long.parseLong(fields[idx++])
            entry.adjustedClose = MathUtil.toMoney(fields[idx++])

            entries += entry
        }

        entries
    }
}

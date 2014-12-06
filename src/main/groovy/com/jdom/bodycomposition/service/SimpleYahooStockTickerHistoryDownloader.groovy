package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker

/**
 * Created by djohnson on 12/5/14.
 */
class SimpleYahooStockTickerHistoryDownloader implements YahooStockTickerHistoryDownloader {

    private static final String BASE_URL = 'http://ichart.finance.yahoo.com/table.csv?s='

    @Override
    String download(YahooStockTicker ticker) {
        return download(ticker, null, null)
    }

    @Override
    String download(YahooStockTicker ticker, Date start, Date end) {
        def urlString = new StringBuilder(BASE_URL).append(ticker.ticker)
        if (start != null) {
            Calendar cal = Calendar.getInstance()
            cal.setTime(start)

            urlString.append('&a=').append(cal.get(Calendar.MONTH))
            urlString.append('&b=').append(cal.get(Calendar.DAY_OF_MONTH))
            urlString.append('&c=').append(cal.get(Calendar.YEAR))
        }

        if (end != null) {
            Calendar cal = Calendar.getInstance()
            cal.setTime(end)

            urlString.append('&d=').append(cal.get(Calendar.MONTH))
            urlString.append('&e=').append(cal.get(Calendar.DAY_OF_MONTH))
            urlString.append('&f=').append(cal.get(Calendar.YEAR))
            urlString.append('&g=').append('d')
        }

        def bytes = new ByteArrayOutputStream()
        def out = new BufferedOutputStream(bytes)
        out << new URL(urlString.toString()).openStream()
        out.close()

        return new String(bytes.toByteArray())
    }
}

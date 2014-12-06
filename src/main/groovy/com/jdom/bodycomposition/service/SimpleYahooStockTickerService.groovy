package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.transaction.Transactional
/**
 * Created by djohnson on 11/14/14.
 */
@Service
@Transactional
class SimpleYahooStockTickerService implements YahooStockTickerService {

    @Autowired YahooStockTickerDao yahooStockTickerDao

    @Override
    List<YahooStockTicker> getTickers() {
        return yahooStockTickerDao.findAll()
    }
}

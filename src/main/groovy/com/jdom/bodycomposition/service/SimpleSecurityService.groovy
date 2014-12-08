package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.util.TimeUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

import javax.transaction.Transactional
/**
 * Created by djohnson on 11/14/14.
 */
@Service
@Transactional
class SimpleSecurityService implements SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSecurityService)

    @Autowired
    StockDao stockDao

    @Autowired
    DailySecurityDataDao dailySecurityDataDao

    @Autowired
    DailySecurityDataDownloader historyDownloader

    @Override
    List<Stock> getStocks() {
        return stockDao.findAll()
    }

    @Override
    void updateHistoryData(BaseSecurity security) throws FileNotFoundException {
        log.info("Updating history data for ticker ${security.symbol}")

        Date start = null

        Pageable latest = new PageRequest(0, 1, Sort.Direction.DESC, 'date')
        Page<DailySecurityData> page = dailySecurityDataDao.findBySecurityOrderByDateDesc(security, latest)

        if (page.hasContent()) {
            Date latestDate = TimeUtil.dateAtStartOfDay(page.getContent()[0].date)
            Date startOfToday = TimeUtil.dateAtStartOfDay(TimeUtil.newDate())

            if (!latestDate.before(startOfToday)) {
                return
            }

            Calendar cal = Calendar.getInstance()
            cal.setTime(latestDate)
            cal.add(Calendar.DATE, 1)
            start = cal.getTime()
        }

        String historyData = historyDownloader.download(security, start, null)
        def parsedData = new YahooDailySecurityDataParser(security, historyData).parse()
        log.info("Parsed ${parsedData.size()} entities for ticker ${security.symbol}")

        for (DailySecurityData data : parsedData) {
            stockDao.save(data)
        }
        log.info("Inserted ${parsedData.size()} entities for ticker ${security.symbol}")
    }

    @Override
    AlgorithmScenario profileAlgorithm(final AlgorithmScenario scenario) {
        Portfolio portfolio = scenario.initialPortfolio
        Algorithm algorithm = scenario.algorithm

        List<Stock> tickers = getStocks()
        for (Iterator<Stock> iter = tickers.iterator(); iter.hasNext();) {
            if (!algorithm.includeSecurity(iter.next())) {
                iter.remove()
            }
        }

        for (Stock ticker : tickers) {
            def dayEntries = dailySecurityDataDao.findBySecurityAndDateBetween(ticker, scenario.startDate,
                    scenario.endDate)

            for (DailySecurityData dayEntry : dayEntries) {
                List<PortfolioTransaction> actions = algorithm.actionsForDay(portfolio, dayEntry)
                if (!actions.empty) {
                    actions.each { transaction ->

                        portfolio = transaction.apply(portfolio)
                        scenario.transactions += transaction
                    }
                }
            }
        }

        scenario.resultPortfolio = portfolioValue(portfolio, scenario.endDate)

        return scenario
    }

    @Override
    PortfolioValue portfolioValue(final Portfolio portfolio, final Date date) {
        Set<PositionValue> positionValues = new HashSet<>()
        for (Position position : portfolio.positions) {
            def dailyValue = dailySecurityDataDao.findBySecurityAndDate(position.security, date)

            positionValues.add(new PositionValue(position, date, dailyValue.close))
        }
        return new PortfolioValue(portfolio, date, positionValues)
    }
}

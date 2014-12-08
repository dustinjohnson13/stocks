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

    static final int MAX_DAILY_SECURITY_DATAS_PER_PAGE = 1000

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
        log.info("Updating history data for security ${security.symbol}")

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
        log.info("Parsed ${parsedData.size()} entities for security ${security.symbol}")

        for (DailySecurityData data : parsedData) {
            stockDao.save(data)
        }
        log.info("Inserted ${parsedData.size()} entities for security ${security.symbol}")
    }

    @Override
    AlgorithmScenario profileAlgorithm(final AlgorithmScenario scenario) {

        long start = TimeUtil.currentTimeMillis()

        Portfolio initialPortfolio = scenario.initialPortfolio
        Algorithm algorithm = scenario.algorithm

        List<Stock> securities = getStocks()
        for (Iterator<Stock> iter = securities.iterator(); iter.hasNext();) {
            if (!algorithm.includeSecurity(iter.next())) {
                iter.remove()
            }
        }

        Portfolio portfolio = initialPortfolio
        def startDate = scenario.startDate
        def endDate = scenario.endDate

        Pageable ascendingByDate = new PageRequest(0, MAX_DAILY_SECURITY_DATAS_PER_PAGE, Sort.Direction.ASC, 'date')
        Page<DailySecurityData> page = dailySecurityDataDao.findBySecurityInAndDateBetweenOrderByDateAsc(securities, startDate, endDate, ascendingByDate)

        while (page.hasContent()) {

            for (DailySecurityData dayEntry : page.getContent()) {

                List<PortfolioTransaction> actions = algorithm.actionsForDay(portfolio, dayEntry)
                if (actions != null && !actions.empty) {
                    actions.each { transaction ->

                        portfolio = transaction.apply(portfolio)
                        scenario.transactions += transaction
                    }
                }
            }

            if (page.last) {
                break
            } else if (page.hasNext()) {
                page = dailySecurityDataDao.findBySecurityInAndDateBetweenOrderByDateAsc(securities, startDate, endDate, page.nextPageable())
            }
        }

        def resultPortfolio = portfolioValue(portfolio, endDate)
        scenario.resultPortfolio = resultPortfolio
        scenario.valueChangePercent = resultPortfolio.percentChangeFrom(portfolioValue(initialPortfolio, startDate))
        scenario.duration = TimeUtil.currentTimeMillis() - start

        return scenario
    }

    @Override
    PortfolioValue portfolioValue(final Portfolio portfolio, final Date date) {
        Set<PositionValue> positionValues = new HashSet<>()
        for (Position position : portfolio.positions) {
            Pageable latestUpToDate = new PageRequest(0, 1, Sort.Direction.DESC, 'date')
            Page<DailySecurityData> page = dailySecurityDataDao.findBySecurityAndDateBetweenOrderByDateDesc(
                  position.security, new Date(0), date, latestUpToDate)

            if (page.hasContent()) {
                positionValues.add(new PositionValue(position, date, page.getContent()[0].close))
            } else {
                log.error(String.format("Unable to find daily security data to calculate the market " +
                      "value of security [%s] for date [%s]!", position.security.symbol, TimeUtil.dashString(date)))
            }
        }
        return new PortfolioValue(portfolio, date, positionValues)
    }

}
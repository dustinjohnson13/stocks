package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.bodycomposition.domain.market.MarketEngine
import com.jdom.bodycomposition.domain.market.MarketEngines
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

import static com.jdom.util.TimeUtil.dashString
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
    DailySecurityMetricsDao dailySecurityMetricsDao

    @Autowired
    DailySecurityDataDownloader historyDownloader

    @Override
    List<Stock> getStocks() {
        return stockDao.findAll()
    }

    @Override
    DailySecurityData save(final DailySecurityData dailySecurityData) {
        def savedDailySecurityData = dailySecurityDataDao.save(dailySecurityData)
        def dailySecurityMetrics = calculateDailyMetrics(savedDailySecurityData)
        dailySecurityMetricsDao.save(dailySecurityMetrics)
        return savedDailySecurityData
    }

    private DailySecurityMetrics calculateDailyMetrics(final DailySecurityData dailySecurityData) {
        def dailySecurityDataDate = dailySecurityData.date
        def cal = Calendar.getInstance()
        cal.setTime(dailySecurityDataDate)
        cal.add(Calendar.WEEK_OF_YEAR, -52)
        def fiftyTwoWeeksAgo = cal.getTime()

        log.info("Searching for the 52 week high and low for security [${dailySecurityData.security.symbol}] " +
              "using date range ${dashString(dailySecurityDataDate)} to ${dashString(fiftyTwoWeeksAgo)}.")

        Page<DailySecurityData> highPage = dailySecurityDataDao.findBySecurityAndDateBetweenOrderByHighDesc(
              dailySecurityData.security, fiftyTwoWeeksAgo, dailySecurityDataDate, new PageRequest(0, 1))

        def fiftyTwoWeekHigh;
        if (highPage.hasContent()) {
            fiftyTwoWeekHigh = highPage.getContent()[0]
        } else {
            log.error("No results for the 52 week high value!  Defaulting to use today, but that should have been found " +
                  "in the query!")

            fiftyTwoWeekHigh = dailySecurityData
        }

        Page<DailySecurityData> lowPage = dailySecurityDataDao.findBySecurityAndDateBetweenOrderByLowAsc(
              dailySecurityData.security, fiftyTwoWeeksAgo, dailySecurityDataDate, new PageRequest(0, 1))
        def fiftyTwoWeekLow;
        if (lowPage.hasContent()) {
            fiftyTwoWeekLow = lowPage.getContent()[0]
        } else {
            log.error("No results for the 52 week low value!  Defaulting to use today, but that should have been found " +
                  "in the query!")

            fiftyTwoWeekLow = dailySecurityData

        }
        return new DailySecurityMetrics(date: dailySecurityDataDate,
              fiftyTwoWeekHigh: fiftyTwoWeekHigh, fiftyTwoWeekLow: fiftyTwoWeekLow)
    }

    @Override
    DailySecurityMetrics findDailySecurityMetricsBySecurityAndDate(final BaseSecurity security, final Date date) {
        return dailySecurityMetricsDao.findByDateAndFiftyTwoWeekHighSecurity(date, security)
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
            save(data)
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
        MarketEngine marketEngine = MarketEngines.create(startDate, endDate, dailySecurityDataDao, portfolio)

        Pageable ascendingByDate = new PageRequest(0, MAX_DAILY_SECURITY_DATAS_PER_PAGE, Sort.Direction.ASC, 'date')
        Page<DailySecurityData> page = dailySecurityDataDao.findBySecurityInAndDateBetweenOrderByDateAsc(securities, startDate, endDate, ascendingByDate)

        Date date = startDate
        marketEngine.processDay() // The first market day must be over in this scenario
        while (page.hasContent()) {

            for (DailySecurityData dayEntry : page.getContent()) {
                while (dayEntry.date.after(date)) {
                    date = TimeUtil.oneDayLater(date)
                    marketEngine.processDay()
                }
                algorithm.actionsForDay(marketEngine, dayEntry)
            }


            if (page.last) {
                break
            } else if (page.hasNext()) {
                page = dailySecurityDataDao.findBySecurityInAndDateBetweenOrderByDateAsc(securities, startDate, endDate, page.nextPageable())
            }
        }
        marketEngine.processDay()

        portfolio = marketEngine.portfolio
        scenario.transactions = marketEngine.transactions
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

    @Override
    BaseSecurity findSecurityBySymbol(final String symbol) {
        return stockDao.findBySymbol(symbol)
    }

}

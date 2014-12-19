package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.util.MathUtil
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

        Page<DailySecurityData> last200 = dailySecurityDataDao.findBySecurityOrderByDateDesc(
              dailySecurityData.security, new PageRequest(0, 200))
        List<DailySecurityData> content = last200.hasContent() ?  new ArrayList<DailySecurityData>(last200.getContent()) : new ArrayList<DailySecurityData>();
        content.add(dailySecurityData)

        def movingAverages = [(5):null, (10):null, (20):null, (50):null, (100):null, (200):null]


        long[] dailyCloses = new long[content.size()]
        if (dailyCloses.length > 4) {
            content.eachWithIndex { DailySecurityData entry, int i ->
                dailyCloses[dailyCloses.length - (i + 1)] = entry.close
            }

            movingAverages.keySet().each { movingAveragePeriod ->
                if (dailyCloses.length < movingAveragePeriod) {
                    return
                }

                Long movingAverage = MathUtil.simpleMovingAverage(dailyCloses, movingAveragePeriod)[dailyCloses.length - 1]
                movingAverages.put(movingAveragePeriod, movingAverage)
            }
        }

        return new DailySecurityMetrics(date: dailySecurityDataDate,
              fiftyTwoWeekHigh: fiftyTwoWeekHigh, fiftyTwoWeekLow: fiftyTwoWeekLow,
        fiveDayMovingAverage: movingAverages.get(5), tenDayMovingAverage: movingAverages.get(10),
        twentyDayMovingAverage: movingAverages.get(20), fiftyDayMovingAverage: movingAverages.get(50),
        hundredDayMovingAverage: movingAverages.get(100), twoHundredDayMovingAverage: movingAverages.get(200))
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

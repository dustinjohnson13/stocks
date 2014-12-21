package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.util.BollingerBands
import com.jdom.util.MACD
import com.jdom.util.MathUtil
import com.jdom.util.Stochastic
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

    private static final Date EARLIEST_ALLOWED_DATE = TimeUtil.dateFromDashString('1960-01-01')

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
        def cal = Calendar.getInstance()
        cal.setTime(dailySecurityData.date)
        cal.add(Calendar.WEEK_OF_YEAR, -52)
        def fiftyTwoWeeksAgo = cal.getTime()

        Page<DailySecurityData> last52WeeksWorthOfEntries = dailySecurityDataDao.findBySecurityAndDateBetweenOrderByDateDesc(
                dailySecurityData.security, fiftyTwoWeeksAgo, dailySecurityData.getDate(), new PageRequest(0, 365))

        if (!last52WeeksWorthOfEntries.hasContent()) {
            throw new IllegalStateException("Why wasn't there content to calculate metrics?  We just inserted a day's entry!")
        }

        List<DailySecurityData> content = new ArrayList<>(last52WeeksWorthOfEntries.getContent())
        Collections.reverse(content)

        return calculateDailyMetrics(dailySecurityData, content)
    }

    private DailySecurityMetrics calculateDailyMetrics(final DailySecurityData dailySecurityData, List<DailySecurityData> entriesUpTo52WeeksAgo) {

        def fiftyTwoWeekHigh = dailySecurityData
        def fiftyTwoWeekLow = dailySecurityData
        for (DailySecurityData entry : entriesUpTo52WeeksAgo) {
            if (entry.high > fiftyTwoWeekHigh.high) {
                fiftyTwoWeekHigh = entry
            }
            if (entry.low < fiftyTwoWeekLow.low) {
                fiftyTwoWeekLow = entry
            }
        }

//        log.info "Using earliest date: ${entriesUpTo52WeeksAgo.get(0).getDate()}"
//        log.info "Using latest date: ${entriesUpTo52WeeksAgo.get(entriesUpTo52WeeksAgo.size() - 1).getDate()}"
//        log.info "Using entry date: ${dailySecurityData.getDate()}"


        def startIndex = Math.max(0, entriesUpTo52WeeksAgo.size() - 200)
        List<DailySecurityData> newest200Entries = entriesUpTo52WeeksAgo.subList(startIndex, entriesUpTo52WeeksAgo.size())
        assert newest200Entries.size() < 201

        def smas = [(5): null, (10): null, (20): null, (50): null, (100): null, (200): null]
        def emas = [(5): null, (10): null, (20): null, (50): null, (100): null, (200): null]
        MACD macd = null
        Stochastic fast = null
        Stochastic slow = null
        Integer relativeStrengthIndex = null
        Integer williamsR = null
        BollingerBands bbands = null
        Integer commodityChannelIndex = null

        long[] dailyCloses = new long[newest200Entries.size()]
        long[] dailyHighs = new long[newest200Entries.size()]
        long[] dailyLows = new long[newest200Entries.size()]
        if (dailyCloses.length > 4) {
            newest200Entries.eachWithIndex { DailySecurityData entry, int i ->
                dailyCloses[i] = entry.close
                dailyHighs[i] = entry.high
                dailyLows[i] = entry.low
            }

            smas.keySet().each { movingAveragePeriod ->
                if (dailyCloses.length < movingAveragePeriod) {
                    return
                }

                Long movingAverage = MathUtil.simpleMovingAverage(dailyCloses, movingAveragePeriod)[dailyCloses.length - 1]
                smas.put(movingAveragePeriod, movingAverage)
                Long ema = MathUtil.exponentialMovingAverage(dailyCloses, movingAveragePeriod)[dailyCloses.length - 1]
                emas.put(movingAveragePeriod, ema)
            }

            if (dailyCloses.length > MACD.TOO_FEW_DAYS_FOR_WINDOW) {
                macd = MathUtil.macd(dailyCloses, 12, 26, 9)
            }
            if (dailyCloses.length > Stochastic.TOO_FEW_DAYS_FOR_WINDOW_FOR_FAST) {
                fast = MathUtil.fastStochastic(dailyHighs, dailyLows, dailyCloses, 14, 3)

                if (dailyCloses.length > Stochastic.TOO_FEW_DAYS_FOR_WINDOW_FOR_SLOW) {
                    slow = MathUtil.slowStochastic(dailyHighs, dailyLows, dailyCloses, 14, 3, 3)
                }
            }

            if (dailyCloses.length > 10) {
                relativeStrengthIndex = MathUtil.relativeStrengthIndex(dailyCloses, 10)
            }
            if (dailyCloses.length > 19) {
                williamsR = MathUtil.williamsR(dailyHighs, dailyLows, dailyCloses, 14)
                bbands = MathUtil.bollingerBands(dailyCloses, 20)
                commodityChannelIndex = MathUtil.commodityChannelIndex(dailyHighs, dailyLows, dailyCloses, 20)
            }
        }

        def metrics = new DailySecurityMetrics(date: dailySecurityData.date,
              fiftyTwoWeekHigh: fiftyTwoWeekHigh, fiftyTwoWeekLow: fiftyTwoWeekLow,
              fiveDaySimpleMovingAverage: smas.get(5), tenDaySimpleMovingAverage: smas.get(10),
              twentyDaySimpleMovingAverage: smas.get(20), fiftyDaySimpleMovingAverage: smas.get(50),
              hundredDaySimpleMovingAverage: smas.get(100), twoHundredDaySimpleMovingAverage: smas.get(200),
              fiveDayExponentialMovingAverage: emas.get(5), tenDayExponentialMovingAverage: emas.get(10),
              twentyDayExponentialMovingAverage: emas.get(20), fiftyDayExponentialMovingAverage: emas.get(50),
              hundredDayExponentialMovingAverage: emas.get(100), twoHundredDayExponentialMovingAverage: emas.get(200),
              relativeStrengthIndex: relativeStrengthIndex, williamsR: williamsR,
              commodityChannelIndex: commodityChannelIndex
        )

        if (macd) {
            metrics.macd = macd.value
            metrics.macdSignal = macd.signal
        }
        if (fast) {
            metrics.fastStochasticOscillatorK = fast.k
            metrics.fastStochasticOscillatorD = fast.d
        }
        if (slow) {
            metrics.slowStochasticOscillatorK = slow.k
            metrics.slowStochasticOscillatorD = slow.d
        }
        if (bbands) {
            metrics.bollingerBandsLower = bbands.lower
            metrics.bollingerBandsUpper = bbands.upper
        }

        return metrics
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
    void recalculateDailyMetrics(BaseSecurity security) {
        log.info("Recalculating daily metrics for security ${security.symbol}")

        List<DailySecurityData> securityDatas = new ArrayList<>(dailySecurityDataDao.findBySecurity(security))
        Collections.sort(securityDatas, new Comparator<DailySecurityData>() {
            @Override
            int compare(final DailySecurityData o1, final DailySecurityData o2) {
                return o1.getDate().compareTo(o2.getDate())
            }
        })

        log.info "Calculating metrics for ${securityDatas.size()} daily security entries."
        List<DailySecurityMetrics> metrics = new ArrayList<>(securityDatas.size())
        for (int i = 0; i < securityDatas.size(); i++) {
            if ( i % 1000 == 0) {
                log.info "  -- ${i}"
            }

            def dailySecurityData = securityDatas.get(i)

            def cal = Calendar.getInstance()
            cal.setTime(dailySecurityData.date)
            cal.add(Calendar.WEEK_OF_YEAR, -52)
            def fiftyTwoWeeksAgo = cal.getTime()

            int startIndex = i
            for (int j = startIndex; j > -1; j--) {
                def earlier = securityDatas.get(j)
                if (earlier.date.before(fiftyTwoWeeksAgo)) {
                    break
                }
                startIndex = j
            }

            List<DailySecurityData> latest52WeeksOfEntries = securityDatas.subList(startIndex, i+1)

            metrics.add(calculateDailyMetrics(dailySecurityData, latest52WeeksOfEntries))
        }
        dailySecurityMetricsDao.save(metrics)
        log.info("Inserted ${metrics.size()} metrics entries for security ${security.symbol}")
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

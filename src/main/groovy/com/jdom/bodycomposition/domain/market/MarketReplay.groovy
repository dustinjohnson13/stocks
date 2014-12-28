package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.impl.BuyRandomSellHigher
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.broker.Brokers
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.DailySecurityMetricsDao
import com.jdom.bodycomposition.service.SecurityService
import com.jdom.util.TimeUtil
import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue

import static com.jdom.util.MathUtil.formatPercentage

/**
 * Created by djohnson on 12/6/14.
 */
@ToString
class MarketReplay implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(MarketReplay)

    private static final int threadPoolSize = 6

    AlgorithmFactory algorithmFactory = BuyRandomSellHigher.factory

    Portfolio initialPortfolio

    long commissionCost

    long duration

    long averagePercentChange

    long maxPercentChange = Long.MIN_VALUE

    long minPercentChange = Long.MAX_VALUE

    void replay(final startDate,
                final endDate,
                final DailySecurityDataDao dailySecurityDataDao,
                final DailySecurityMetricsDao dailySecurityMetricsDao,
                final SecurityService securityService,
                final int instancesOfAlgorithms) {

        Map<String, Class<? extends Algorithm>> algorithms = [:]

        List<AlgorithmConfiguration> configurations = []
        instancesOfAlgorithms.times {
            def configuration = new AlgorithmConfiguration(
                    algorithm: algorithmFactory.createInstance(it))
            configurations.add(configuration)
        }

        log.info "Running scenario ${TimeUtil.dashString(startDate)} - ${TimeUtil.dashString(endDate)}"

        long start = TimeUtil.currentTimeMillis()

        MarketEngine marketEngine = createMarketEngine(dailySecurityDataDao)

        configurations.each {
            it.broker = Brokers.create(marketEngine, initialPortfolio, commissionCost)
        }

        final ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
        Date currentDate = startDate

        BlockingQueue<Object> dailyData = new LinkedBlockingQueue<>()
        pool.submit(queueDailySecurityData(startDate, endDate, dailySecurityDataDao, dailySecurityMetricsDao, dailyData))

        def initialPortfolioValue = null
        Map<Integer, DailySecurityData> dailyDataBySecurityId = [:]
        while (!endDate.before(currentDate)) {

            long startTime = TimeUtil.currentTimeMillis()

            List<DailySecurityData> dailySecurityDatas = dailyData.take()

            dailyDataBySecurityId = createMapOfDailySecurityDataBySecurityId(dailySecurityDatas)

            if (currentDate == startDate) {
                initialPortfolioValue = securityService.portfolioValue(configurations[0].broker.portfolio, currentDate, dailyDataBySecurityId)
            }

            marketEngine.processDay(currentDate, dailyDataBySecurityId)

            def dailySecurityMetrics = dailyData.take()

            List<Future<?>> futures = new ArrayList<Future<?>>(instancesOfAlgorithms)
            if (!dailySecurityDatas.empty) {
                configurations.each {
                    Future<?> future = pool.submit(new Runnable() {
                        @Override
                        void run() {
                            def portfolioValue = securityService.portfolioValue(it.broker.portfolio, currentDate, dailyDataBySecurityId)
                            it.algorithm.actionsForDay(it.broker, portfolioValue, new ArrayList<DailySecurityData>(dailySecurityDatas), new ArrayList<DailySecurityData>(dailySecurityMetrics), new Date(currentDate.getTime()))
                        }
                    })
                    futures.add(future)
                }
            }

            futures.each {
                it.get()
            }

            currentDate = TimeUtil.oneDayLater(currentDate)

            log.info "Time taken: ${TimeUtil.currentTimeMillis() - startTime}"
        }

        duration = TimeUtil.currentTimeMillis() - start
        long percentChanges = 0l
        configurations.each {
            def resultPortfolio = securityService.portfolioValue(it.broker.portfolio, endDate, dailyDataBySecurityId)
            long valueChangePercent = resultPortfolio.percentChangeFrom(initialPortfolioValue)
            log.info "Percent change: ${formatPercentage(valueChangePercent)}"
            percentChanges += valueChangePercent

            if (valueChangePercent > maxPercentChange) {
                maxPercentChange = valueChangePercent
            } else if (valueChangePercent < minPercentChange) {
                minPercentChange = valueChangePercent
            }
        }
        averagePercentChange = (long) (percentChanges / instancesOfAlgorithms)

        log.info """
            Average percent change: ${formatPercentage(averagePercentChange)}
            Maximum percent change: ${formatPercentage(maxPercentChange)}
            Minimum percent change: ${formatPercentage(minPercentChange)}"""
    }

    private Runnable queueDailySecurityData(Date startDate,
                                            Date endDate,
                                            DailySecurityDataDao dailySecurityDataDao,
                                            DailySecurityMetricsDao dailySecurityMetricsDao,
                                            BlockingQueue<Object> dailyData) {
        return new Runnable() {
            @Override
            void run() {
                Date dataDate = startDate
                while (!endDate.before(dataDate)) {
                    def dailySecurityDatas = dailySecurityDataDao.findByDate(dataDate)
                    dailyData.add(dailySecurityDatas)

                    def dailySecurityMetrics = dailySecurityMetricsDao.findByDate(dataDate)
                    dailyData.add(dailySecurityMetrics)

                    dataDate = TimeUtil.oneDayLater(dataDate)
                }
            }
        }
    }

    protected MarketEngine createMarketEngine(final DailySecurityDataDao dailySecurityDataDao) {
        return MarketEngines.create(dailySecurityDataDao)
    }

    static class AlgorithmConfiguration {
        Broker broker
        Algorithm algorithm
    }

    static Map<Integer, DailySecurityData> createMapOfDailySecurityDataBySecurityId(
            final List<DailySecurityData> dailySecurityDatas) {
        def map = [:]
        dailySecurityDatas.each {
            map[it.security.id] = it
        }

        return map
    }
}

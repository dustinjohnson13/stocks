package com.jdom.bodycomposition.domain

import groovy.transform.ToString

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

/**
 * Created by djohnson on 12/9/14.
 */
@Entity
@Table(name = 'security_daily_metrics')
@ToString
class DailySecurityMetrics implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id

    @Column
    Date date

    @OneToOne
    @JoinColumn(name = 'fifty_two_week_high')
    DailySecurityData fiftyTwoWeekHigh

    @OneToOne
    @JoinColumn(name = 'fifty_two_week_low')
    DailySecurityData fiftyTwoWeekLow

    @Column(name = 'five_day_simple_moving_average')
    Long fiveDaySimpleMovingAverage

    @Column(name = 'ten_day_simple_moving_average')
    Long tenDaySimpleMovingAverage

    @Column(name = 'twenty_day_simple_moving_average')
    Long twentyDaySimpleMovingAverage

    @Column(name = 'fifty_day_simple_moving_average')
    Long fiftyDaySimpleMovingAverage

    @Column(name = 'hundred_day_simple_moving_average')
    Long hundredDaySimpleMovingAverage

    @Column(name = 'two_hundred_day_simple_moving_average')
    Long twoHundredDaySimpleMovingAverage

    @Column(name = 'five_day_exponential_moving_average')
    Long fiveDayExponentialMovingAverage

    @Column(name = 'ten_day_exponential_moving_average')
    Long tenDayExponentialMovingAverage

    @Column(name = 'twenty_day_exponential_moving_average')
    Long twentyDayExponentialMovingAverage

    @Column(name = 'fifty_day_exponential_moving_average')
    Long fiftyDayExponentialMovingAverage

    @Column(name = 'hundred_day_exponential_moving_average')
    Long hundredDayExponentialMovingAverage

    @Column(name = 'two_hundred_day_exponential_moving_average')
    Long twoHundredDayExponentialMovingAverage

    @Column
    Long macd

    @Column(name = 'macd_signal')
    Long macdSignal

    @Column(name = 'fast_stochastic_oscillator_k')
    Integer fastStochasticOscillatorK

    @Column(name = 'fast_stochastic_oscillator_d')
    Integer fastStochasticOscillatorD

    @Column(name = 'slow_stochastic_oscillator_k')
    Integer slowStochasticOscillatorK

    @Column(name = 'slow_stochastic_oscillator_d')
    Integer slowStochasticOscillatorD

    @Column(name = 'relative_strength_index')
    Integer relativeStrengthIndex

    @Column(name = 'williams_r')
    Integer williamsR

    @Column(name = 'bollinger_bands_upper')
    Integer bollingerBandsUpper

    @Column(name = 'commodity_channel_index')
    Integer commodityChannelIndex

    @Column(name = 'bollinger_bands_lower')
    Integer bollingerBandsLower

    // TODO: Should the stochastics contain the decimal portion?
    // TODO: Would the KDJ Indicator be valuable?
    // TODO: Would the BIAS Ratio be valuable?
    // TODO: Would the Volume Moving Average (VMA) be valuable?
}

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

    @Column(name = 'five_day_moving_average')
    Long fiveDayMovingAverage

    @Column(name = 'ten_day_moving_average')
    Long tenDayMovingAverage

    @Column(name = 'twenty_day_moving_average')
    Long twentyDayMovingAverage

    @Column(name = 'fifty_day_moving_average')
    Long fiftyDayMovingAverage

    @Column(name = 'hundred_day_moving_average')
    Long hundredDayMovingAverage

    @Column(name = 'two_hundred_day_moving_average')
    Long twoHundredDayMovingAverage
}

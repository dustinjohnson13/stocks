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

}

package com.jdom.bodycomposition.domain

import groovy.transform.ToString

import javax.persistence.*

/**
 * Created by djohnson on 11/15/14.
 */
@Entity
@Table(name = 'yahoo_stock_ticker_data')
@ToString
class DailySecurityData implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id

    @ManyToOne
    @JoinColumn(name = 'ticker_id') //TODO: Rename column in the database
    Stock security

    @Column
    Date date

    @Column
    Long open

    @Column
    Long close

    @Column
    Long high

    @Column
    Long low

    @Column
    Long volume

    @Column(name = 'adjusted_close')
    Long adjustedClose
}

package com.jdom.bodycomposition.domain

import groovy.transform.ToString

import javax.persistence.*

/**
 * Created by djohnson on 11/15/14.
 */
@Entity
@Table(name = 'security_daily_data')
@ToString
class DailySecurityData implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id

    @ManyToOne
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

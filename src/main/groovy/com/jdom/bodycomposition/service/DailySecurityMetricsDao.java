package com.jdom.bodycomposition.service;

import com.jdom.bodycomposition.domain.BaseSecurity;
import com.jdom.bodycomposition.domain.DailySecurityMetrics;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * Created by djohnson on 11/15/14.
 */
@Repository
public interface DailySecurityMetricsDao extends PagingAndSortingRepository<DailySecurityMetrics, Long> {

   DailySecurityMetrics findByDateAndFiftyTwoWeekHighSecurity(final Date date, final BaseSecurity security);

}

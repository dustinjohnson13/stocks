package com.jdom.bodycomposition.service;

import com.jdom.bodycomposition.domain.BaseSecurity;
import com.jdom.bodycomposition.domain.Stock;
import com.jdom.bodycomposition.domain.DailySecurityData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Created by djohnson on 11/15/14.
 */
@Repository
public interface DailySecurityDataDao extends PagingAndSortingRepository<DailySecurityData, Long> {

   List<DailySecurityData> findBySecurity(BaseSecurity security);

   Page<DailySecurityData> findBySecurityOrderByDateDesc(BaseSecurity security, Pageable pageable);

   List<Stock> findBySecurityAndDateBetween(final Stock security, final Date start, final Date end);
}

package com.jdom.bodycomposition.service;

import com.jdom.bodycomposition.domain.YahooStockTicker;
import com.jdom.bodycomposition.domain.YahooStockTickerData;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by djohnson on 11/15/14.
 */
@Repository
public interface YahooStockTickerDataDao extends PagingAndSortingRepository<YahooStockTickerData, Long> {

   List<YahooStockTickerData> findByTicker(YahooStockTicker ticker);
}

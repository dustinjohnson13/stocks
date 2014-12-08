package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.algorithm.PositionValue;
import com.jdom.util.MathUtil;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by djohnson on 11/16/14.
 */
public class PositionsPanel extends Panel {

   public PositionsPanel(String id, String title, IModel<List<? extends PositionValue>> model) {
      super(id, model);

      final ListView<PositionValue> entries = new ListView<PositionValue>("position", model) {

         @Override
         protected void populateItem(ListItem<PositionValue> item) {
            PositionValue entry = item.getModelObject();
            item.add(new Label("symbol", entry.getSecurity().getSymbol()));
            item.add(new Label("shares", entry.getShares()));
            item.add(new Label("price", MathUtil.formatMoney(entry.getPrice())));
            item.add(new Label("cashValue", MathUtil.formatMoney(entry.marketValue())));
         }
      };
      add(entries);
   }
}

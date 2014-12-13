package com.jdom.bodycomposition.web;

import com.jdom.bodycomposition.domain.algorithm.PortfolioValue;
import com.jdom.bodycomposition.domain.algorithm.PositionValue;
import com.jdom.util.MathUtil;
import com.jdom.util.TimeUtil;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jdom.util.MathUtil.formatMoney;
import static com.jdom.util.MathUtil.formatPercentage;

/**
 * Created by djohnson on 12/12/14.
 */
public class PortfolioValuePanel extends Panel {
    public PortfolioValuePanel(String id, final IModel<PortfolioValue> model) {
        this(id, model, new Model<PortfolioValue>());
    }

    public PortfolioValuePanel(String id, final IModel<PortfolioValue> model, final IModel<PortfolioValue> initialPortfolioModel) {
        super(id, model);

        add(new Label("date", new Model<String>() {
            @Override
            public String getObject() {
                final PortfolioValue resultPortfolio = model.getObject();
                return (resultPortfolio == null) ? "" : TimeUtil.dashString(resultPortfolio.getDate());
            }

            @Override
            public void setObject(final String object) {
            }
        }));

        final IModel<List<? extends PositionValue>> positions = new LoadableDetachableModel<List<? extends PositionValue>>() {
            protected List<? extends PositionValue> load() {
                final PortfolioValue resultPortfolio = model.getObject();
                return (resultPortfolio == null) ? Collections.<PositionValue>emptyList() :
                        new ArrayList<>(resultPortfolio.getPositions());
            }
        };

        final PositionsPanel positionsPanel = new PositionsPanel("positions", "positions", positions);
        positionsPanel.setOutputMarkupId(true);
        add(positionsPanel);

        add(new Label("marketValue", new Model<String>() {
            @Override
            public String getObject() {

                final PortfolioValue resultPortfolio = model.getObject();
                if (resultPortfolio == null) {
                    return "";
                } else {
                    PortfolioValue initialPortfolioValue = initialPortfolioModel.getObject();

                    String currentValue = formatMoney(resultPortfolio.marketValue());
                    String overallReturn = (initialPortfolioValue == null) ? "" :
                            String.format("(%s)", formatPercentage(resultPortfolio.percentChangeFrom(initialPortfolioValue)));

                    return String.format("%s %s", currentValue, overallReturn);
                }
            }

            @Override
            public void setObject(final String object) {
            }
        }));

        add(new Label("resultCash", new Model<String>() {
            @Override
            public String getObject() {
                final PortfolioValue resultPortfolio = model.getObject();
                return resultPortfolio == null ? "" : MathUtil.formatMoney(resultPortfolio.getCash());
            }

            @Override
            public void setObject(final String object) {
            }
        }));
    }
}

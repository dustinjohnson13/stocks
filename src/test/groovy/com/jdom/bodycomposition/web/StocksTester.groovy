package com.jdom.bodycomposition.web
import com.jdom.util.MathUtil
import org.apache.wicket.Component
import org.apache.wicket.markup.html.form.Form
import org.apache.wicket.protocol.http.WebApplication
import org.apache.wicket.util.tester.BaseWicketTester
import org.apache.wicket.util.tester.FormTester
import org.apache.wicket.util.tester.WicketTester
/**
 * Created by djohnson on 12/7/14.
 */
class StocksTester extends WicketTester {

    StocksTester() {
    }

    StocksTester(WebApplication application) {
        super(application)
    }

    @Override
    StocksFormTester newFormTester(String path) {
        return newFormTester(path, true);
    }

    @Override
    StocksFormTester newFormTester(String path, boolean fillBlankString) {
        return new StocksFormTester(path, (Form<?>)getComponentFromLastRenderedPage(path), this,
              fillBlankString);
    }

    static class StocksFormTester extends FormTester {

        StocksFormTester(String path, Form<?> workingForm, BaseWicketTester wicketTester, boolean fillBlankString) {
            super(path, workingForm, wicketTester, fillBlankString)
        }

        FormTester setMoney(String formComponentId, String money) {
            Component component = super.getForm().get(formComponentId);
            return setMoney(component, money)
        }

        FormTester setMoney(Component formComponent, String money) {
            long asLong = (long) (MathUtil.toMoney(money) / 100L)
            return setValue(formComponent, Long.toString(asLong))
        }
    }
}

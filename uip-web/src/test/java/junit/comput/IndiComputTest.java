package junit.comput;

import com.sq.comput.domain.IndicatorInstance;
import com.sq.comput.domain.IndicatorTemp;
import com.sq.comput.service.IndiComputService;
import com.sq.comput.service.IndicatorTempService;
import com.sq.entity.search.MatchType;
import com.sq.entity.search.Searchable;
import com.sq.util.DateUtil;
import junit.base.TestCase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: shuiqing
 * Date: 2015/4/17
 * Time: 16:09
 * Email: shuiqing301@gmail.com
 * GitHub: https://github.com/ShuiQing301
 * Blog: http://shuiqing301.github.io
 * _
 * |_)._ _
 * | o| (_
 */
public class IndiComputTest extends TestCase {

    @Autowired
    private IndiComputService indiComputService;

    @Autowired
    private IndicatorTempService indicatorTempService;

    /*@Test
    public void testInterfaceDataGather () {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, 20);
        System.out.println("uuuuuuuuuuuuu" + DateUtil.formatCalendar(cal, DateUtil.DATE_FORMAT_Y_M_D));
        this.indiComputService.interfaceDataGather(cal);
    }

    @Test
    public void testReComput(){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, 20);
        List<IndicatorTemp> indicatorTempList = new ArrayList<IndicatorTemp>();
        Searchable searchable = Searchable.newSearchable().addSearchFilter("indicatorCode", MatchType.LIKE,"%PT%");
        indicatorTempList = indicatorTempService.findAll(searchable).getContent();
        this.indiComputService.reComputIndicator(cal,indicatorTempList);
    }*/

    @Test
    public void testCalculateDataGather () {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, 16);
        System.out.println("uuuuuuuuuuuuu" + DateUtil.formatCalendar(cal, DateUtil.DATE_FORMAT_Y_M_D));
        this.indiComputService.calculateDataGater(cal);
    }

    @Test
    public void batchSave(){
        for (int i=1;i<100;i++) {
            IndicatorInstance indicatorInstance = new IndicatorInstance();
            indicatorInstance.setDataSource(2);
            indicatorInstance.setDecimalNum(2);
            indicatorInstance.setFloatValue(23d);
            indicatorInstance.setCategoryId(201l);
            indicatorInstance.setCreateTime(Calendar.getInstance());
            indicatorInstance.setIndicatorTempId(275l);
            indicatorInstance.setStatDateNum(20150504);
            indicatorInstance.setValueType(1);
            indicatorInstance.setFetchCycle(1);
            indicatorInstance.setOperCalType(1);
            indicatorInstance.setIndicatorCode("asdasdsa");
            indicatorInstance.setIndicatorName("test");
            indicatorInstance.setUnit(5);
            this.indiComputService.save(indicatorInstance);
        }
    }
}
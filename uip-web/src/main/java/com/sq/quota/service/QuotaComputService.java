package com.sq.quota.service;

import com.sq.comput.component.ComputHelper;
import com.sq.comput.domain.IndicatorConsts;
import com.sq.entity.search.MatchType;
import com.sq.entity.search.Searchable;
import com.sq.entity.search.condition.OrCondition;
import com.sq.entity.search.condition.SearchFilterHelper;
import com.sq.inject.annotation.BaseComponent;
import com.sq.quota.component.QuotaComputHelper;
import com.sq.quota.domain.QuotaConsts;
import com.sq.quota.domain.QuotaInstance;
import com.sq.quota.domain.QuotaTemp;
import com.sq.quota.repository.QuotaInstanceRepository;
import com.sq.quota.repository.QuotaTempRepository;
import com.sq.quota.strategy.*;
import com.sq.service.BaseService;
import com.sq.util.DateUtil;
import net.sourceforge.jeval.EvaluationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 指标运算服务类.
 * User: shuiqing
 * Date: 2015/8/25
 * Time: 9:44
 * Email: shuiqing301@gmail.com
 * GitHub: https://github.com/shuiqing301
 * Blog: http://shuiqing301.github.io/
 * _
 * |_)._ _
 * | o| (_
 */
@Service
public class QuotaComputService extends BaseService<QuotaInstance,Long> {

    private static final Logger log = LoggerFactory.getLogger(QuotaComputService.class);

    @Autowired
    @BaseComponent
    private QuotaInstanceRepository quotaInstanceRepository;

    @Autowired
    private QuotaTempRepository quotaTempRepository;

    /** 指标模板缓存 */
    public static Map<String,QuotaTemp> quotaTempMapCache = new HashMap<String,QuotaTemp>();

    /**
     * 初始化操作
     *     1、缓存指标模板
     *     2、更新指标模板的指标模板基础表达式
     */
    public void reloadQuotaCalculateExp() {
        cacheQuotaTemp();
        /*updateQuotaNativeExp();*/
    }

    /**
     * 缓存指标模板
     */
    public void cacheQuotaTemp () {
        List<QuotaTemp> quotaTempList = quotaTempRepository.findAll();
        for (QuotaTemp quotaTemp:quotaTempList) {
            quotaTempMapCache.put(quotaTemp.getIndicatorCode(), quotaTemp);
        }
    }

    /**
     * 更新指标的基础表达式
     */
    public void updateQuotaNativeExp () {
        List<QuotaTemp> quotaTempList = new LinkedList<QuotaTemp>();
        for (Map.Entry<String, QuotaTemp> entry : quotaTempMapCache.entrySet()) {
            if (entry.getValue().getDataSource() != IndicatorConsts.DATASOURCE_CALCULATE) {
                continue;
            }
            String nativeExpression = generateNativeExpression(entry.getValue().getCalculateExpression());
            entry.getValue().setGernaterdNativeExpression(nativeExpression);
            quotaTempList.add(entry.getValue());
        }
        quotaTempRepository.save(quotaTempList);
    }

    /**
     * 接口数据汇集到系统的最小维度小时级
     * @param computCal 计算时间
     */
    public void interfaceDataGather (Calendar computCal) {
        ThreadPoolExecutor _instance = ComputHelper.initThreadPooSingleInstance();

        Searchable searchable = Searchable.newSearchable()
                .addSearchFilter(" DataSource", MatchType.EQ, IndicatorConsts.DATASOURCE_INTERFACE);
        Page<QuotaTemp> quotaTempPage = this.quotaTempRepository.findAll(searchable);
        List<QuotaTemp> quotaTempList = quotaTempPage.getContent();

        for (QuotaTemp quotaTemp:quotaTempList) {
            log.error(" QuotaTemp:->" + quotaTemp.getIndicatorName());
            sendCalculateComm(quotaTemp, computCal, new InterfaceQuotaStrategy());
        }
    }

    /**
     * 发送指标计算请求
     * @param quotaTemp 指标模板
     * @param computCal 计算时间点
     * @param iComputStrategy 计算策略
     */
    public synchronized void sendCalculateComm (QuotaTemp quotaTemp,
                                                         Calendar computCal, IQuotaComputStrategy iComputStrategy) {
        QuotaComputTask quotaComputTask = new QuotaComputTask();
        quotaComputTask.setComputCal(computCal);
        quotaComputTask.setAssignMillions(System.currentTimeMillis());
        quotaComputTask.setiQuotaComputStrategy(iComputStrategy);
        quotaComputTask.setQuotaTemp(quotaTemp);
        QuotaComputHelper.fetchThreadPooSingleInstance().submit(quotaComputTask);
    }

    /**
     * 生成最低级别的计算表达式
     * @param exp 计算表达式
     * @return 生成的指标表达式
     */
    public String generateNativeExpression (String exp) {
        List<String> variableList = ComputHelper.getVariableList(exp, ComputHelper.getEvaluatorInstance());

        boolean expStatusflag = true;//表达式状态，true表示已经是基础表达式，没有关联的计算指标了
        for (String variable:variableList) {
            //生成native expression时不校验关联指标编码所对应的指标模板的存在性
            //   判断表达式中的编码是否存在，留到具体的计算处统一校验
            QuotaTemp quotaTemp = quotaTempMapCache.get(variable);
            if (null == quotaTemp) {
                return null;//关联指标不存在，直接退出
            }
            if (quotaTemp.getDataSource() == QuotaConsts.DATASOURCE_CALCULATE) {
                String replaceString = quotaTemp.getCalculateExpression();
                String needReplaceString = EvaluationConstants.OPEN_VARIABLE + variable + EvaluationConstants.CLOSED_BRACE;
                exp = exp.replace(needReplaceString,replaceString);
                expStatusflag = false;
            }
        }

        if (expStatusflag) {
            return exp;
        } else {
            return generateNativeExpression(exp);
        }
    }

    /**
     * 计算指标重计算
     *     在指定计算日期至当前日期的时间范围内，对传入的指标的所有关联指标做重计算
     * @param computCal
     * @param quotaTempList
     */
    public void reComputQuota (Calendar computCal, List<QuotaTemp> quotaTempList) {

        List<QuotaTemp> associatedQuotaTempList = searchAssociatedQuotaTemp(quotaTempList);

        //删除计算指标的关联指标
        deleteNeedReComputIndicator(computCal,associatedQuotaTempList);
        for (QuotaTemp quotaTemp:associatedQuotaTempList) {
            System.out.println(quotaTemp.getIndicatorCode());
        }

        List<Calendar> calendarList = DateUtil.dayListSinceCal(computCal);
        QuotaComputHelper.computExecuteCounter.set(associatedQuotaTempList.size());
        for (Calendar reComputCal : calendarList) {
            while(QuotaComputHelper.computExecuteCounter.get() < associatedQuotaTempList.size()) {
                System.out.println("counter: " + QuotaComputHelper.computExecuteCounter.get());
                try {
                    Thread.sleep(1000l);
                } catch (InterruptedException e) {
                    log.error("sleep error.", e);
                }
            }
            QuotaComputHelper.computExecuteCounter.set(0);
            startComputFullDayQuota(reComputCal, associatedQuotaTempList);
        }
    }

    /**
     * 计算一天的指标
     *    设置超时时常，一旦计算超时还未计算完的指标结果都以NULL标示
     */
    public void startComputFullDayQuota (Calendar reComputCal, List<QuotaTemp> quotaTempList) {
        for (QuotaTemp quotaTemp:quotaTempList) {
            switch (quotaTemp.getCalType()) {
                case IndicatorConsts.CALTYPE_INVENTORY:
                    sendCalculateComm(quotaTemp, reComputCal, new InventoryQuotaStrategy());
                    break;
                case IndicatorConsts.CALTYPE_ORIGINAL:
                    sendCalculateComm(quotaTemp, reComputCal, new PrimaryQuotaStrategy());
                    break;
            }
        }
    }

    /**
     * 找出指标集合的关联指标
     */
    public List<QuotaTemp> searchAssociatedQuotaTemp (List<QuotaTemp> quotaTempList) {
        Assert.notEmpty(quotaTempList);
        Searchable searchable = Searchable.newSearchable()
                .addSearchFilter("dataSource", MatchType.EQ, IndicatorConsts.DATASOURCE_CALCULATE);
        OrCondition orCondition = new OrCondition();
        for (QuotaTemp quotaTemp:quotaTempList) {
            orCondition.add(
                    SearchFilterHelper.newCondition("gernaterdNativeExpression",
                            MatchType.LIKE, "%#{" + quotaTemp.getIndicatorCode() + "}%"));
        }
        searchable.addSearchFilter(orCondition);
        return quotaTempRepository.findAll(searchable).getContent();
    }

    /**
     * 删除需要重计算的指标以及关联指标实例
     */
    public void deleteNeedReComputIndicator(Calendar computCal, List<QuotaTemp> associatedQuotaTempList){
        Assert.notEmpty(associatedQuotaTempList, "deleteNeedReComputIndicator: 关联指标列表不能为空.");
        List<String> indicatorCodeList = new ArrayList<String>();
        for(QuotaTemp quotaTemp:associatedQuotaTempList) {
            indicatorCodeList.add(quotaTemp.getIndicatorCode());
        }

        int startComputDateNum = Integer.parseInt(DateUtil.formatCalendar(computCal, DateUtil.DATE_FORMAT_DAFAULT));
        Searchable deleteSearchable = Searchable.newSearchable()
                .addSearchFilter("statDateNum", MatchType.GTE, startComputDateNum)
                .addSearchFilter("dataSource", MatchType.EQ, IndicatorConsts.DATASOURCE_CALCULATE)
                .addSearchFilter("indicatorCode",MatchType.IN, indicatorCodeList);
        quotaInstanceRepository.delete(quotaInstanceRepository.findAll(deleteSearchable).getContent());
    }
}

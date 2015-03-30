package com.sq.protocol.opc.domain;

import com.sq.entity.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 实时测点.
 * User: shuiqing
 * Date: 2015/3/30
 * Time: 13:57
 * Email: shuiqing301@gmail.com
 * GitHub: https://github.com/ShuiQing301
 * Blog: http://shuiqing301.github.io/
 * _
 * |_)._ _
 * | o| (_
 */
@Entity
@Table(name = "T_MesuringPoint")
public class MesuringPoint extends BaseEntity<Long> {

    /**
     * 源code，DCS或者PLC系统上测点的编码
     */
    private String sourceCode;

    /**
     * 目标code, 接口对接方编码
     */
    private String targetCode;

    /**
     * 测点名称
     */
    private String pointName;

    /**
     * 测点计算类型，默认不需要计算
     */
    private MeaType meaType = MeaType.OriginalMea;

    /**
     * 计算表达式
     */
    private String calculateExp;

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public String getPointName() {
        return pointName;
    }

    public void setPointName(String pointName) {
        this.pointName = pointName;
    }

    public MeaType getMeaType() {
        return meaType;
    }

    public void setMeaType(MeaType meaType) {
        this.meaType = meaType;
    }

    public String getCalculateExp() {
        return calculateExp;
    }

    public void setCalculateExp(String calculateExp) {
        this.calculateExp = calculateExp;
    }
}

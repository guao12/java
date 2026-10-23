package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 言灵图鉴 新增 / 编辑 请求。
 * 字段含义与首页展示（原静态 SPIRIT_GALLERY）完全一致。
 *
 * @author fenglin
 */
public class SpiritSaveDTO {

    /** 路由 key：小写字母 / 数字 / 连字符，如 junyan */
    @NotBlank(message = "请填写言灵 key（路由地址，如 junyan）")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,49}$",
            message = "key 只能是小写字母、数字和连字符，且以字母或数字开头")
    private String key;

    /** 言灵名 */
    @NotBlank(message = "请填写言灵名")
    @Size(max = 50, message = "言灵名不能超过 50 个字符")
    private String name;

    /** 元素系别 */
    @NotBlank(message = "请填写元素系别")
    @Size(max = 20, message = "元素系别不能超过 20 个字符")
    private String element;

    /** 密级 */
    @NotBlank(message = "请选择密级")
    private String level;

    /** 言灵效果（一句话） */
    @NotBlank(message = "请填写言灵效果")
    @Size(max = 100, message = "言灵效果不能超过 100 个字符")
    private String effect;

    /** 详细介绍 */
    @NotBlank(message = "请填写详细介绍")
    @Size(max = 2000, message = "详细介绍不能超过 2000 个字符")
    private String desc;

    /** 典型持有者 */
    @Size(max = 100, message = "持有者信息不能超过 100 个字符")
    private String holder = "";

    /** 使用禁忌 / 备注 */
    @Size(max = 200, message = "备注不能超过 200 个字符")
    private String note = "";

    /** 展示排序，越小越靠前 */
    @Min(value = 0, message = "排序值不能小于 0")
    @Max(value = 9999, message = "排序值不能大于 9999")
    private Integer sort = 0;

    /** 状态：1=展示 0=隐藏 */
    @Min(0)
    @Max(1)
    private Integer status = 1;

    public SpiritSaveDTO() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 言灵图鉴实体，对应表 dr_spirit。
 *
 * <p>首页「言灵图鉴」区块与 /spirit/:key 详情页的数据源，
 * 由后台管理系统维护。字段含义与前端原静态 SPIRIT_GALLERY 一致。</p>
 *
 * <p>命名说明（避开 MySQL 保留字 + MyBatis-Plus 别名规则）：
 * key 是保留字，数据库列用 spirit_key、Java 属性用 spiritKey，JSON 仍输出为 key；
 * desc 同理，数据库列用 intro、Java 属性用 intro，JSON 输出为 desc。</p>
 *
 * @author fenglin
 */
@TableName("dr_spirit")
public class DrSpirit implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 路由 key（JSON 字段名 key），如 junyan */
    @TableField("spirit_key")
    private String spiritKey;

    /** 言灵名 */
    @TableField("name")
    private String name;

    /** 元素系别：火/冰/风/重力/时间/精神/暗影/雷/威压… */
    @TableField("element")
    private String element;

    /** 密级：内部/机密/高危/绝密 */
    @TableField("level")
    private String level;

    /** 言灵效果（一句话） */
    @TableField("effect")
    private String effect;

    /** 详细介绍（JSON 字段名 desc） */
    @TableField("intro")
    private String intro;

    /** 典型持有者 */
    @TableField("holder")
    private String holder;

    /** 使用禁忌 / 备注 */
    @TableField("note")
    private String note;

    /** 插画图片网址（http/https），非空时优先于本地 /img/spirit/<key>.webp */
    @TableField("image_url")
    private String imageUrl;

    /** 展示排序，越小越靠前 */
    @TableField("sort")
    private Integer sort;

    /** 状态：1=展示 0=隐藏 */
    @TableField("status")
    private Integer status;

    /** 逻辑删除标记：0=未删除 1=已删除 */
    @TableLogic
    @TableField(value = "deleted", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer deleted;

    /** 创建时间，数据库生成，Java 侧只读 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间，数据库维护，Java 侧只读 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    public DrSpirit() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("key")
    public String getSpiritKey() {
        return spiritKey;
    }

    public void setSpiritKey(String spiritKey) {
        this.spiritKey = spiritKey;
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

    @JsonProperty("desc")
    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "DrSpirit{id=" + id + ", key='" + spiritKey + "', name='" + name + "'}";
    }
}

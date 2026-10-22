package com.fenglin.springboottest.vo;

import java.util.List;

/**
 * 字典返回：在校身份 + 言灵，前后端共用同一套 code。
 * 前端 RegisterView.vue 使用：dict.identities / dict.spirits，每项 { code, name }。
 *
 * @author fenglin
 */
public class DictVO {

    private List<Item> identities;
    private List<Item> spirits;

    public DictVO() {
    }

    public DictVO(List<Item> identities, List<Item> spirits) {
        this.identities = identities;
        this.spirits = spirits;
    }

    public List<Item> getIdentities() {
        return identities;
    }

    public void setIdentities(List<Item> identities) {
        this.identities = identities;
    }

    public List<Item> getSpirits() {
        return spirits;
    }

    public void setSpirits(List<Item> spirits) {
        this.spirits = spirits;
    }

    /** 字典项 */
    public static class Item {
        private String code;
        private String name;

        public Item() {
        }

        public Item(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

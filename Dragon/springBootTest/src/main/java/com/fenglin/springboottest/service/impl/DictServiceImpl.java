package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.service.DictService;
import com.fenglin.springboottest.vo.DictVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典服务实现。字典项与前端 RegisterView.vue 的本地默认值保持一致，
 * 保证后端未启动时前端回退本地字典、后端启动后两边 code 完全相同。
 *
 * @author fenglin
 */
@Service
public class DictServiceImpl implements DictService {

    /** 在校身份 */
    private static final Map<String, String> IDENTITIES = new LinkedHashMap<>();

    /** 言灵 */
    private static final Map<String, String> SPIRITS = new LinkedHashMap<>();

    static {
        IDENTITIES.put("freshman", "新生 · 2026 级");
        IDENTITIES.put("student", "在校生");
        IDENTITIES.put("executor", "执行部预备成员");
        IDENTITIES.put("teacher", "教员 / 研究员");
        IDENTITIES.put("guest", "访客 · 临时通行");

        SPIRITS.put("none", "未觉醒 · 待血统鉴定");
        SPIRITS.put("junyan", "言灵 · 君焰");
        SPIRITS.put("bronze", "言灵 · 青铜御座");
        SPIRITS.put("shiling", "言灵 · 时零");
        SPIRITS.put("fengwang", "言灵 · 风王之瞳");
        SPIRITS.put("zhulong", "言灵 · 烛龙");
        SPIRITS.put("wangquan", "言灵 · 王权");
    }

    @Override
    public DictVO dictionaries() {
        return new DictVO(toItems(IDENTITIES), toItems(SPIRITS));
    }

    @Override
    public String identityName(String code) {
        if (code == null) {
            return "未登记";
        }
        return IDENTITIES.getOrDefault(code, code);
    }

    @Override
    public String spiritName(String code) {
        if (code == null) {
            return "未觉醒";
        }
        return SPIRITS.getOrDefault(code, code);
    }

    @Override
    public boolean isValidIdentity(String code) {
        return code != null && IDENTITIES.containsKey(code);
    }

    @Override
    public boolean isValidSpirit(String code) {
        return code != null && SPIRITS.containsKey(code);
    }

    @Override
    public Map<String, String> identities() {
        return IDENTITIES;
    }

    @Override
    public Map<String, String> spirits() {
        return SPIRITS;
    }

    private static List<DictVO.Item> toItems(Map<String, String> source) {
        List<DictVO.Item> items = new ArrayList<>(source.size());
        source.forEach((code, name) -> items.add(new DictVO.Item(code, name)));
        return items;
    }
}

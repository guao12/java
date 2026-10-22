package com.fenglin.springboottest.service;

import com.fenglin.springboottest.vo.DictVO;

import java.util.Map;

/**
 * 字典服务：在校身份 + 言灵，前后端共用同一套 code。
 *
 * @author fenglin
 */
public interface DictService {

    /** 字典接口返回（注册页下拉项） */
    DictVO dictionaries();

    /** 在校身份 code -> 中文名，未知 code 返回原值 */
    String identityName(String code);

    /** 言灵 code -> 中文名，未知 code 返回原值 */
    String spiritName(String code);

    /** 校验在校身份 code 是否合法 */
    boolean isValidIdentity(String code);

    /** 校验言灵 code 是否合法 */
    boolean isValidSpirit(String code);

    /** 只读视图，供内部遍历 */
    Map<String, String> identities();

    Map<String, String> spirits();
}

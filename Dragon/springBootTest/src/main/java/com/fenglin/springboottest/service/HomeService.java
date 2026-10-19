package com.fenglin.springboottest.service;

import com.fenglin.springboottest.vo.HomeOverviewVO;

/**
 * 首页展示服务：一次请求返回首页所需的全部数据。
 *
 * @author fenglin
 */
public interface HomeService {

    /** 首页概览：公告 + 统计 + 两级分类 + 精选 / 热门 / 最新 + 热词 */
    HomeOverviewVO overview();
}

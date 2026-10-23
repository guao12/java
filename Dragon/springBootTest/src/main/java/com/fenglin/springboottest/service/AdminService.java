package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.AdminLoginDTO;
import com.fenglin.springboottest.dto.SpiritSaveDTO;
import com.fenglin.springboottest.entity.DrAdmin;
import com.fenglin.springboottest.entity.DrSpirit;
import com.fenglin.springboottest.vo.AdminLoginVO;

import java.util.List;

/**
 * 后台管理系统服务：管理员登录 + 言灵图鉴数据管理。
 *
 * @author fenglin
 */
public interface AdminService {

    /** 管理员登录：账号密码校验 + 签发后台 JWT */
    AdminLoginVO login(AdminLoginDTO dto);

    /** 按主键取管理员（拦截器用） */
    DrAdmin getAdminById(Long id);

    /** 后台列表：全部言灵（含隐藏），按 sort 升序 */
    List<DrSpirit> listSpirits();

    /** 前台列表：仅 status=1 的言灵，首页言灵图鉴与详情页共用 */
    List<DrSpirit> publicSpirits();

    /** 新增言灵（key 重复时抛业务异常） */
    DrSpirit createSpirit(SpiritSaveDTO dto);

    /** 编辑言灵 */
    DrSpirit updateSpirit(Long id, SpiritSaveDTO dto);

    /** 删除言灵（逻辑删除） */
    void deleteSpirit(Long id);
}

package com.tanhua.server.controller;

import com.tanhua.common.utils.Cache;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
@RequestMapping("tanhua")
public class TodayBestController {

    @Autowired
    private TodayBestService todayBestService;

    /**
     * 查询今天佳人
     *
     * @param
     * @return
     */
    @GetMapping("todayBest")
    public ResponseEntity<TodayBest> queryTodayBast() {
        try {
            TodayBest todayBast = this.todayBestService.queryTodayBast();
            if (todayBast != null) {
                return ResponseEntity.ok(todayBast);
            }
        } catch (Exception e) {
            log.error("查询今日佳人失败  UserId=" + UserThreadLocal.get().getId(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    /**
     * 查询推荐用户列表
     * @param queryParam
     * @return
     */
    @GetMapping("recommendation")
    @Cache
    public ResponseEntity<PageResult> queryRecommendation(RecommendUserQueryParam queryParam){
        try {
            PageResult pageresult = this.todayBestService.queryRecommendation(queryParam);
            if (pageresult != null) {
                return ResponseEntity.ok(pageresult);
            }
        } catch (Exception e) {
            log.error("查询推荐用户列表失败  UserId=" +UserThreadLocal.get().getId() , e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}


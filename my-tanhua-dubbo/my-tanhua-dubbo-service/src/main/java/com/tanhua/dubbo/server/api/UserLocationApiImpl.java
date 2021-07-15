package com.tanhua.dubbo.server.api;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.api.UserLocationApi;
import com.tanhua.dubbo.server.pojo.UserLocation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.geo.GeoPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service(version = "1.0.0")
@Slf4j
public class UserLocationApiImpl implements UserLocationApi {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /**初始化索引库
     *
     */
    @PostConstruct
    public void initIndex(){
        //判断索引库是否存在，如果不存在，需要创建
        if(!this.elasticsearchTemplate.indexExists("tanhua")){
            this.elasticsearchTemplate.createIndex(UserLocation.class);
        }
        //判断表是否存在，如果不存在，需要创建
        if(!this.elasticsearchTemplate.typeExists("tanhua", "user_location")){
            this.elasticsearchTemplate.putMapping(UserLocation.class);
        }
    }

    @Override
    public Boolean updateUserLocation(Long userId, Double longitude, Double latitude, String address) {
        //查询个人的地理位置数据，如果不存在，需要新增，如果是存在数据，更新数据
        try {
            GetQuery getQuery = new GetQuery();
            getQuery.setId(String.valueOf(userId));
            UserLocation userLocation = this.elasticsearchTemplate.queryForObject(getQuery, UserLocation.class);
            if(ObjectUtil.isEmpty(userLocation)){
                //新增数据
                userLocation = new UserLocation();
                userLocation.setUserId(userId);
                userLocation.setAddress(address);
                userLocation.setCreated(System.currentTimeMillis());
                userLocation.setUpdated(userLocation.getCreated());
                userLocation.setLastUpdated(userLocation.getCreated());
                userLocation.setLocation(new GeoPoint(latitude, longitude));
                IndexQuery indexQuery = new IndexQueryBuilder().withObject(userLocation).build();
                //保存数据到ES中
                this.elasticsearchTemplate.index(indexQuery);
            }else {
                //更新数据
                //更新的字段
                Map<String,Object> map = new HashMap<>();
                map.put("location", new GeoPoint(latitude, longitude));
                map.put("updated", System.currentTimeMillis());
                map.put("lastUpdated", userLocation.getUpdated());
                map.put("address", address);
                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.doc(map);
                UpdateQuery updateQuery = new UpdateQueryBuilder()
                        .withId(String.valueOf(userId))
                        .withClass(UserLocation.class)
                        .withUpdateRequest(updateRequest).build();
                //更新数据
                this.elasticsearchTemplate.update(updateQuery);
            }
            return true;
        } catch (Exception e) {
            log.error("更新地理位置失败~ userId = " + userId + ", longitude = " + longitude + ", latitude = " + latitude + ", address = " + address, e);
        }
        return false;
    }
}
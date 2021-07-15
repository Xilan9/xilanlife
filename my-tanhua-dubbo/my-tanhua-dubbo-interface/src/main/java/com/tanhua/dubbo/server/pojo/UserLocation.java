package com.tanhua.dubbo.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.geo.GeoPoint;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "tanhua", type = "user_location", shards = 6, replicas = 2)
public class UserLocation {

    @Id
    private Long userId; //用户id
    @GeoPointField
    private GeoPoint location; //lon:经度 lat:纬度

    @Field(type = FieldType.Keyword)
    private String address; //位置描述

    @Field(type = FieldType.Long)
    private Long created; //创建时间

    @Field(type = FieldType.Long)
    private Long updated; //更新时间

    @Field(type = FieldType.Long)
    private Long lastUpdated; //上次更新时间
}
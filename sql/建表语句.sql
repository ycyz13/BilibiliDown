CREATE TABLE up (
    id bigint(20) AUTO_INCREMENT NOT NULL COMMENT '主键',
    biz_no varchar(32) NOT NULL COMMENT '编号',
    uid varchar(64) NOT NULL COMMENT 'up主用户id',
    up_name varchar(64) NOT NULL COMMENT 'up名称',
    handle_status int(3) NOT NULL COMMENT '处理状态。1：初始化，2：常规同步',
    last_sync_time datetime NOT NULL COMMENT '上次成功同步作品时间',
    be_like varchar(1024) NOT NULL COMMENT '像谁',
    update_time datetime NOT NULL COMMENT '更新记录时间',
    create_time datetime NOT NULL COMMENT '创建记录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_biz_no(biz_no),
    UNIQUE KEY uniq_uid(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='up主信息维护表'
// todo: 给up表增加自动update time

CREATE TABLE vedio (
    id bigint(20) AUTO_INCREMENT NOT NULL COMMENT '主键',
    up_uid varchar(64) NOT NULL COMMENT 'up主用户id',
    handle_status int NOT NULL COMMENT '处理状态。1：待同步，2：同步完毕',
    bv_id varchar(64) NOT NULL COMMENT 'bvId',
    bv_create_time datetime NOT NULL COMMENT '投稿时间',
    cid varchar(64) NOT NULL COMMENT 'cid',
    bv_page int NOT NULL COMMENT '投稿内的选集',
    av_title varchar(512) NOT NULL COMMENT 'av标题',
    title varchar(512) NOT NULL COMMENT '标题',
    file_name varchar(512) NOT NULL COMMENT '下载文件名称',
    store_path varchar(2048) NOT NULL COMMENT '文件存储路径',
    qn int NOT NULL COMMENT '视频清晰度',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新记录时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建记录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_bv_id_cid(bv_id, cid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投稿表'

ALTER TABLE up
    MODIFY COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE vedio
    MODIFY COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
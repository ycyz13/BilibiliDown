package ycyz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 投稿表
 * </p>
 *
 * @author ycyz
 * @since 2024-08-23
 */
@Getter
@Setter
public class Vedio implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * up主用户id
     */
    private String upUid;

    /**
     * 处理状态。1：待同步，2：同步完毕
     */
    private Integer handleStatus;

    /**
     * bvId
     */
    private String bvId;

    /**
     * cid
     */
    private String cid;

    /**
     * av标题
     */
    private String avTitle;

    /**
     * 标题
     */
    private String title;

    /**
     * 下载文件名称
     */
    private String fileName;

    /**
     * 文件存储路径
     */
    private String storePath;

    /**
     * 视频清晰度
     */
    private Integer qn;

    /**
     * 更新记录时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建记录时间
     */
    private LocalDateTime createTime;
}

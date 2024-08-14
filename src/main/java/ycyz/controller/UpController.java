package ycyz.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import ycyz.entity.Up;
import ycyz.service.IUpService;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * up主信息维护表 前端控制器
 * </p>
 *
 * @author ycyz
 * @since 2024-08-13
 */
@RestController
@RequestMapping("/up")
public class UpController {
    @Autowired
    private IUpService upService;

    @GetMapping(value = "")
    public List<Up> findAll() {
        return this.upService.list(new LambdaQueryWrapper<Up>().orderByAsc(Up::getId));
    }

    @PutMapping(value = "/{id}")
    public Up updateSyncTime(@PathVariable Long id, @RequestBody Up up) {
        up.setId(id);
        up.setUpdateTime(LocalDateTime.now());
        this.upService.updateById(up);
        return up;
    }

}

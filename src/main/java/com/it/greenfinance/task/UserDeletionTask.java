package com.it.greenfinance.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.it.greenfinance.pojo.User;
import com.it.greenfinance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.List;

/**
 * 用户注销相关的定时任务
 */
@Component
public class UserDeletionTask {
    
    @Autowired
    private UserService userService;
    
    /**
     * 每天凌晨2点执行，检查并处理超过7天冷静期的用户
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processUserDeletion() {
        // 查询所有处于注销冷静期的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deletion_status", 1);
        List<User> users = userService.list(queryWrapper);
        
        // 计算7天前的时间
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        
        // 遍历用户，处理超过7天的
        for (User user : users) {
            if (user.getDeletionTime() != null && user.getDeletionTime().before(cal.getTime())) {
                // 更新用户状态为已注销
                user.setDeletionStatus(2);
                userService.updateById(user);
                
                // 可以在这里添加其他清理逻辑，如删除关联数据等
                // cleanupUserData(user.getId());
            }
        }
    }
    
    /**
     * 每月1号凌晨3点执行，清理超过两年的数据
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void cleanUpOldData() {
        // 计算两年前的时间
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -2);
        
        // 在这里可以添加清理超过两年数据的逻辑
        // 例如：清理已注销超过两年的用户数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deletion_status", 2); // 已注销用户
        queryWrapper.lt("deletion_time", cal.getTime()); // 注销时间超过两年
        List<User> usersToDelete = userService.list(queryWrapper);
        
        // 物理删除这些用户数据（谨慎操作，确保符合法规要求）
        for (User user : usersToDelete) {
            // 删除用户相关数据
            cleanupUserData(user.getId());
            
            // 删除用户本身
            userService.removeById(user.getId());
        }
    }
    
    /**
     * 清理用户相关数据（可根据业务需求扩展）
     * @param userId 用户ID
     */
    private void cleanupUserData(Long userId) {
        // 在这里可以添加清理用户相关数据的逻辑
        // 例如：删除用户的账单、预算、分类等数据
        // 这里只是示例，具体实现根据业务需求确定
        /*
        // 删除用户的账单
        billService.remove(new QueryWrapper<Bill>().eq("user_id", userId));
        
        // 删除用户的预算
        budgetService.remove(new QueryWrapper<Budget>().eq("user_id", userId));
        
        // 删除用户的分类
        categoryService.remove(new QueryWrapper<Category>().eq("user_id", userId));
        
        // 删除用户的子分类
        subCategoryService.remove(new QueryWrapper<SubCategory>().eq("user_id", userId));
        
        // 删除用户的分类关键词
        categoryKeywordService.remove(new QueryWrapper<CategoryKeyword>().eq("user_id", userId));
        
        // 删除用户的预计支出
        expectedExpenseService.remove(new QueryWrapper<ExpectedExpense>().eq("user_id", userId));
        
        // 删除用户的系统配置
        systemConfigService.remove(new QueryWrapper<SystemConfig>().eq("user_id", userId));
        */
    }
}
package com.topik.topikai.service;

import com.topik.topikai.entity.Role;
import com.topik.topikai.entity.User;
import com.topik.topikai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UsageQuotaService {

    public static final int FREE_DAILY_GRADING = 3;
    public static final int FREE_WEEKLY_MINITEST = 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    public boolean isPremium(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.isPresent() && user.get().getRole() == Role.PREMIUM_USER;
    }

    public int countGradingToday(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_answer WHERE user_id = ? AND created_at = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, LocalDate.now());
        return count != null ? count : 0;
    }

    public boolean canGrade(Long userId) {
        if (isPremium(userId)) return true;
        return countGradingToday(userId) < FREE_DAILY_GRADING;
    }

    public int countMiniTestThisWeek(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM mini_test_log WHERE user_id = ? AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean canGenerateMiniTest(Long userId) {
        if (isPremium(userId)) return true;
        return countMiniTestThisWeek(userId) < FREE_WEEKLY_MINITEST;
    }

    public void logMiniTest(Long userId) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO mini_test_log (user_id, created_at) VALUES (?, CURDATE())",
                    userId
            );
        } catch (Exception e) {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS mini_test_log (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                            "user_id BIGINT NOT NULL, " +
                            "created_at DATE NOT NULL)"
            );
            jdbcTemplate.update(
                    "INSERT INTO mini_test_log (user_id, created_at) VALUES (?, CURDATE())",
                    userId
            );
        }
    }

    public Map<String, Object> getQuotaInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        boolean premium = isPremium(userId);
        int usedToday = countGradingToday(userId);
        int miniUsed = countMiniTestThisWeek(userId);

        info.put("isPremium", premium);
        info.put("gradingUsedToday", usedToday);
        info.put("gradingLimitDaily", premium ? -1 : FREE_DAILY_GRADING);
        info.put("canGrade", premium || usedToday < FREE_DAILY_GRADING);
        info.put("miniTestUsedWeek", miniUsed);
        info.put("miniTestLimitWeekly", premium ? -1 : FREE_WEEKLY_MINITEST);
        info.put("canMiniTest", premium || miniUsed < FREE_WEEKLY_MINITEST);
        return info;
    }
}

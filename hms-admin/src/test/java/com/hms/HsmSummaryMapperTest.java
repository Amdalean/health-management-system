package com.hms;

import java.util.List;

import com.hms.main.summary.domain.SummaryDetailVo;
import com.hms.main.summary.mapper.HsmSummaryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class HsmSummaryMapperTest
{
    @Autowired
    private HsmSummaryMapper hsmSummaryMapper;

    @Test
    public void testSelectLastSummaryDetailByType()
    {
        // 测试查询最新汇总明细按类型分组的方法
        Long userId = 101L; // 使用参考SQL中的用户ID
        List<SummaryDetailVo> result = hsmSummaryMapper.selectLastSummaryDetailByType(userId);

        System.out.println("查询结果数量: " + result.size());
        for (SummaryDetailVo vo : result) {
            System.out.println("类型: " + vo.getDictLabel() + ", 金额: " + vo.getMny());
        }

        // 断言结果不为空
        assert result != null;
    }
}
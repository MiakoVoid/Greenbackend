package com.it.greenfinance.service.impl;

import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.CategoryKeywordMapper;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.mapper.SystemConfigMapper;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.CategoryKeyword;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.greenfinance.pojo.vo.OcrBillParseResultVo;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.utils.QwenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private CategoryKeywordMapper categoryKeywordMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private SubCategoryMapper subCategoryMapper;
    @Mock
    private BillMapper billMapper;
    @Mock
    private QwenUtil qwenUtil;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private BillService billService;
    @Mock
    private ExpectedExpenseService expectedExpenseService;

    @InjectMocks
    private AIServiceImpl aiService;

    private List<Category> mockCategories;

    @BeforeEach
    void setUp() {
        // Setup default mock categories
        mockCategories = new ArrayList<>();
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("餐饮");
        mockCategories.add(cat);
        lenient().when(systemConfigMapper.getConfigValue(any(), any())).thenReturn("0");
        BillVo billVo = new BillVo();
        billVo.setId(1L);
        lenient().when(billService.createBill(any(BillBo.class))).thenReturn(billVo);
    }

    @Test
    void processText_ShouldPreloadCategoriesAndProcessSegments() {
        // Arrange
        Long userId = 1L;
        String text = "买菜花了20元";
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        // Act
        List<Object> result = aiService.processText(userId, text, null);

        // Assert
        assertNotNull(result);
        verify(categoryMapper, times(1)).selectList(null);
        verify(subCategoryMapper, times(1)).selectList(null);
    }

    @Test
    void processText_WithMultipleSegments_ShouldPreloadOnce() {
        // Arrange
        Long userId = 1L;
        String text = "买菜20元，打车30元"; // Comma separates segments
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        aiService.processText(userId, text, null);

        // Assert
        verify(categoryMapper, times(1)).selectList(null);
        verify(subCategoryMapper, times(1)).selectList(null);
    }

    @Test
    void processText_WithExplicitBilltime_ShouldUseProvidedTime() {
        // Arrange
        Long userId = 1L;
        String text = "billtime=2023-10-01 12:00:00, 买菜20元";
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Mock keyword match to ensure category assignment
        CategoryKeyword keyword = new CategoryKeyword();
        keyword.setCategoryId(1L);
        keyword.setKeyword("买菜");
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.singletonList(keyword));

        // Capture BillBo to check time
        ArgumentCaptor<BillBo> billBoCaptor = ArgumentCaptor.forClass(BillBo.class);
        
        // Act
        aiService.processText(userId, text, null);

        // Assert
        verify(billService, times(1)).createBill(billBoCaptor.capture());
        BillBo capturedBill = billBoCaptor.getValue();
        
        LocalDateTime expected = LocalDateTime.of(2023, 10, 1, 12, 0, 0);
        Date expectedDate = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());
        
        // Allow small difference due to ms precision conversion
        assertEquals(expectedDate.getTime() / 1000, capturedBill.getBillTime().getTime() / 1000);
    }

    @Test
    void processText_WithInvalidBilltime_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        String text = "billtime=invalid-date, 买菜20元";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            aiService.processText(userId, text, null);
        });
    }

    @Test
    void processText_WithExplicitParameterBilltime_ShouldUseParameter() {
        // Arrange
        Long userId = 1L;
        String text = "买菜20元";
        String billTimeParam = "2023-11-11 11:11:11";
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        CategoryKeyword keyword = new CategoryKeyword();
        keyword.setCategoryId(1L);
        keyword.setKeyword("买菜");
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.singletonList(keyword));
        
        ArgumentCaptor<BillBo> billBoCaptor = ArgumentCaptor.forClass(BillBo.class);

        // Act
        aiService.processText(userId, text, billTimeParam);

        // Assert
        verify(billService, times(1)).createBill(billBoCaptor.capture());
        BillBo capturedBill = billBoCaptor.getValue();
        
        LocalDateTime expected = LocalDateTime.of(2023, 11, 11, 11, 11, 11);
        Date expectedDate = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());
        
        assertEquals(expectedDate.getTime() / 1000, capturedBill.getBillTime().getTime() / 1000);
    }

    @Test
    void processText_WithParameterBilltimeOverridingTextBilltime() {
        // Arrange
        Long userId = 1L;
        String text = "billtime=2023-10-01 12:00:00, 买菜20元";
        String billTimeParam = "2023-11-11 11:11:11"; // Should override the one in text
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        CategoryKeyword keyword = new CategoryKeyword();
        keyword.setCategoryId(1L);
        keyword.setKeyword("买菜");
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.singletonList(keyword));
        
        ArgumentCaptor<BillBo> billBoCaptor = ArgumentCaptor.forClass(BillBo.class);

        // Act
        aiService.processText(userId, text, billTimeParam);

        // Assert
        verify(billService, times(1)).createBill(billBoCaptor.capture());
        BillBo capturedBill = billBoCaptor.getValue();
        
        // Should match parameter, not text
        LocalDateTime expected = LocalDateTime.of(2023, 11, 11, 11, 11, 11);
        Date expectedDate = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());
        
        assertEquals(expectedDate.getTime() / 1000, capturedBill.getBillTime().getTime() / 1000);
    }

    @Test
    void processText_WithNaturalLanguage_Yesterday() {
        // Arrange
        Long userId = 1L;
        String text = "昨天买菜20元";
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Mock keyword match
        CategoryKeyword keyword = new CategoryKeyword();
        keyword.setCategoryId(1L);
        keyword.setKeyword("买菜");
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.singletonList(keyword));

        ArgumentCaptor<BillBo> billBoCaptor = ArgumentCaptor.forClass(BillBo.class);

        // Act
        aiService.processText(userId, text, null);

        // Assert
        verify(billService, times(1)).createBill(billBoCaptor.capture());
        BillBo capturedBill = billBoCaptor.getValue();
        
        LocalDateTime expected = LocalDateTime.now().minusDays(1);
        Date billDate = capturedBill.getBillTime();
        LocalDateTime actual = LocalDateTime.ofInstant(billDate.toInstant(), ZoneId.systemDefault());
        
        assertEquals(expected.getDayOfYear(), actual.getDayOfYear());
    }

    @Test
    void processText_WithNaturalLanguage_LastNight() {
        // Arrange
        Long userId = 1L;
        String text = "昨晚打车30元";
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Mock keyword match
        CategoryKeyword keyword = new CategoryKeyword();
        keyword.setCategoryId(1L);
        keyword.setKeyword("打车");
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.singletonList(keyword));

        ArgumentCaptor<BillBo> billBoCaptor = ArgumentCaptor.forClass(BillBo.class);

        // Act
        aiService.processText(userId, text, null);

        // Assert
        verify(billService, times(1)).createBill(billBoCaptor.capture());
        BillBo capturedBill = billBoCaptor.getValue();
        
        LocalDateTime expected = LocalDateTime.now().minusDays(1).withHour(20).withMinute(0);
        Date billDate = capturedBill.getBillTime();
        LocalDateTime actual = LocalDateTime.ofInstant(billDate.toInstant(), ZoneId.systemDefault());
        
        assertEquals(expected.getDayOfYear(), actual.getDayOfYear());
        assertEquals(20, actual.getHour());
    }

    @Test
    void processText_WithNaturalLanguage_LastWeekMonday() {
        // Arrange
        Long userId = 1L;
        String text = "上周一买菜20元";
        
        when(categoryMapper.selectList(any())).thenReturn(mockCategories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        CategoryKeyword keyword = new CategoryKeyword();
        keyword.setCategoryId(1L);
        keyword.setKeyword("买菜");
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.singletonList(keyword));

        ArgumentCaptor<BillBo> billBoCaptor = ArgumentCaptor.forClass(BillBo.class);

        // Act
        aiService.processText(userId, text, null);

        // Assert
        verify(billService, times(1)).createBill(billBoCaptor.capture());
        BillBo capturedBill = billBoCaptor.getValue();
        
        // Calculate expected date carefully
        // minusWeeks(1) from now, then with(MONDAY)
        LocalDateTime expected = LocalDateTime.now().minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        
        Date billDate = capturedBill.getBillTime();
        LocalDateTime actual = LocalDateTime.ofInstant(billDate.toInstant(), ZoneId.systemDefault());
        
        assertEquals(expected.getDayOfYear(), actual.getDayOfYear());
    }

    @Test
    void processRawOcrText_ShouldNormalizeFields() {
        Long userId = 1L;
        String rawText = "微信支付 金额-12.3 2026-03-22 14:30:00";
        when(qwenUtil.parseRawOcrBillText(any())).thenReturn("{\"isBillScene\":true,\"sceneConfidence\":99,\"bills\":[{\"merchantName\":\"喜茶\",\"amount\":\"-12.3\",\"consumeTime\":\"2026-03-22 14:30:00\",\"billType\":\"退款\",\"paymentMethod\":\"微信支付\",\"orderNumber\":\"A001\"}]}");

        OcrBillParseResultVo result = aiService.processRawOcrText(userId, rawText);

        assertTrue(result.getBillScene());
        assertEquals(99, result.getSceneConfidence());
        assertEquals(1, result.getBills().size());
        assertEquals("-12.30", result.getBills().get(0).getAmount().toPlainString());
        assertEquals(1, result.getBills().get(0).getType());
        assertNotNull(result.getBills().get(0).getBillTime());
    }

    @Test
    void processRawOcrText_WhenTooLong_ShouldThrow4001() {
        Long userId = 2L;
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 0; i < 21000; i++) {
            textBuilder.append("a");
        }
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                aiService.processRawOcrText(userId, textBuilder.toString()));
        assertTrue(exception.getMessage().startsWith("[4001]"));
    }

    @Test
    void processRawOcrText_WhenAiResultInvalid_ShouldThrow5001() {
        Long userId = 3L;
        when(qwenUtil.parseRawOcrBillText(any())).thenReturn("not-json");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                aiService.processRawOcrText(userId, "测试文本"));
        assertTrue(exception.getMessage().startsWith("[5001]"));
    }
}

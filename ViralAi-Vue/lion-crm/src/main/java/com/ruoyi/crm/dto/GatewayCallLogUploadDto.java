package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.ruoyi.common.annotation.Excel;

import lombok.Getter;
import lombok.Setter;

/**
 * 网关原始流水上传导入对象
 *
 * <p>只用于手工上传的 Excel 解析，不落库：解析后由
 * {@code IGatewayCallLogService#uploadAndIngest} 补齐客户与模型编码，再走与网关上报
 * 相同的入库逻辑。</p>
 *
 * <p>客户不在文件里，由上传弹窗指定（一次上传归属一个客户，避免多客户混表时误归属）；
 * 模型编码可直接填 {@code product_code}，也可只填上游模型标识/变体，由
 * {@code crm_gateway_pull_map} 的 PRODUCT 规则解析。</p>
 *
 * <p>列顺序无关，按表头名匹配，多余列会被忽略。</p>
 *
 * @author ruoyi
 * @date 2026-11-15
 */
@Getter
@Setter
public class GatewayCallLogUploadDto
{
    /** 网关调用流水号，入库幂等键 */
    @Excel(name = "流水号", sort = 1, width = 34, prompt = "必填。渠道侧唯一流水号，重复上传会被跳过")
    private String callNo;

    /** 模型编码，直接指定时不再走模型映射 */
    @Excel(name = "模型编码", sort = 2, width = 22, prompt = "选填。填了就直接用该 product_code，优先级高于模型标识")
    private String productCode;

    /** 上游模型标识，按 PRODUCT 映射解析模型编码 */
    @Excel(name = "模型标识", sort = 3, width = 22, prompt = "选填。未填模型编码时按「流水映射配置」的模型规则解析")
    private String modelKey;

    /** 上游模型变体，如 720p|text_to_video */
    @Excel(name = "模型变体", sort = 4, width = 20, prompt = "选填。模型标识存在多档时用于精确匹配映射规则")
    private String modelVariant;

    /** 模型类型 LLM/Embedding/TTS/Image */
    @Excel(name = "模型类型", sort = 5, width = 12)
    private String modelType;

    /** 输入token */
    @Excel(name = "输入token", sort = 6, width = 14)
    private Long promptTokens;

    /** 输出token */
    @Excel(name = "输出token", sort = 7, width = 14)
    private Long completionTokens;

    /** 总token，留空时按输入+输出汇总 */
    @Excel(name = "总token", sort = 8, width = 14)
    private Long totalTokens;

    /** 刊例价费用 */
    @Excel(name = "刊例价费用", sort = 9, width = 18, scale = 8)
    private BigDecimal totalFees;

    /** 折后费用（渠道实际扣费口径） */
    @Excel(name = "折后费用", sort = 10, width = 18, scale = 8)
    private BigDecimal actualFees;

    /** 调用时间，计费归属时间 */
    @Excel(name = "调用时间", sort = 11, width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss",
            prompt = "必填。支持 yyyy-MM-dd HH:mm:ss / yyyy/MM/dd HH:mm:ss")
    private Date callTime;

    /**
     * 调用状态
     *
     * <p>用字符串接收而不是整数：导入时 Excel 里既可能填「成功/失败」，也可能填 1/0，
     * 由服务层统一归一，避免 {@code @Excel(readConverterExp)} 对纯数字不做反查导致丢值。</p>
     */
    @Excel(name = "调用状态", sort = 12, width = 12, prompt = "选填。成功/失败 或 1/0，留空按成功处理")
    private String callStatusText;

    /** 失败原因 */
    @Excel(name = "失败原因", sort = 13, width = 30)
    private String errMsg;
}

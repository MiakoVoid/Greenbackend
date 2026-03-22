# OCR账单解析接口文档

## 接口信息

- 路径：`POST /api/ai/ocr`
- 说明：Android端先完成图片OCR，再将原始文本上传到后端；后端调用内部AI服务完成账单场景识别与多账单结构化提取。
- 鉴权：`Authorization: Bearer <token>`
- Content-Type：`application/json`

## 请求参数

```json
{
  "ocrText": "微信支付\n商户：喜茶科技园店\n金额：-12.3\n时间：2026-03-22 14:30:00\n订单号：A001"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| ocrText | String | 是 | Android端OCR提取的原始文本（最大20kB） |

## 成功响应示例

```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": {
    "billScene": true,
    "sceneConfidence": 99,
    "bills": [
      {
        "merchant": "喜茶科技园店",
        "amount": -12.30,
        "billTime": "2026-03-22 14:30:00",
        "type": 1,
        "paymentMethod": "微信支付",
        "orderNumber": "A001"
      }
    ]
  }
}
```

## 字段规则

- 账单场景识别：仅当 `sceneConfidence >= 98` 且AI判断为账单时，`billScene=true`。
- 多账单支持：`bills` 为数组，可返回多条账单。
- 金额标准化：`amount` 统一保留两位小数，支持负数退款。
- 时间标准化：内部统一转换为 ISO-8601（UTC+8）后再映射为 `BillVo.billTime`。
- 账单类型：映射到 `BillVo.type`（1=支出，2=收入；退款场景按支出方向返回负金额）。

## 错误码

| 错误码 | 含义 | 触发条件 |
|---|---|---|
| 4001 | OCR文本超长 | `ocrText` 超过 20kB |
| 4002 | 调用频率超限 | 单用户每分钟调用超过30次 |
| 5001 | AI解析失败 | AI返回空结果或返回格式不符合约定 |

## 失败响应示例

```json
{
  "success": false,
  "code": 4002,
  "message": "调用过于频繁，请稍后重试",
  "data": null
}
```

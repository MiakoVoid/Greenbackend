# 绿芽记账（greenfinance）接口文档

输入使用统一实体类bo类，返回实体类vo类,数据库操作使用实体类。

## 一、基础元信息

| 字段     | 取值                                                                  | 说明                                                                 |
|--------|---------------------------------------------------------------------|--------------------------------------------------------------------|
| 项目名    | greenfinance（绿芽记账）                                                  | 个人财务管理安卓应用                                                         |
| 文档版本   | V1.0                                                                | 接口定义稳定版                                                            |
| 基础URL  | 生产：暂定；开发：`http://localhost:8080/api`                                | 接口请求根路径                                                            |
| 认证方式   | JWT Token，请求头携带 `Authorization: Bearer {token}`                     | 除"注册/登录"外所有接口必传                                                    |
| 统一响应格式 | JSON结构，包含`success`（布尔）、`code`（整数）、`message`（字符串）、`data`（对象/数组/Null） | 成功时`success=true`，失败时返回错误原因                                        |
| 核心存储特性 | 图片（头像/分类图标）存储于安卓本地，数据库存"相对路径/资源标识"                                  | 如"file:files/greenfinance/avatars/10001.png""res:ic_category_food" |

## 二、用户认证模块（Auth Module）

### 1. 用户注册
- **接口标识**：地址=`/auth/register`，方法=POST
- 描述：注册账号，只需要username和password,暂时不考虑email和phone的验证功能。密码6-20位，含字母+数字,可以有特殊符号。
- **请求参数（JSON）**：
  | 参数名     | 类型   | 必传 | 说明                          | 示例值               |
  |------------|--------|------|-------------------------------|----------------------|
  | username   | String | 是   | 3-20位，含字母/数字/下划线    | "zhangsan123"        |
  | password   | String | 是   | 6-20位，含字母+数字           | "Zhang@123456"       |
  | email      | String | 否   | 符合邮箱格式                   | "zhangsan@xxx.com"   |
  | phone      | String | 否   | 11位数字                      | "13800138000"        |
 - **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "注册成功",
    "data": {
      "userId": 10001,
      "username": "zhangsan123",
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
  ```

### 4. 认证与授权机制说明
- **认证令牌**：登录成功后返回 `token`，前端在后续请求头中设置 `Authorization: Bearer {token}`。
- **忽略认证路径**：`/auth/register`, `/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`, `/favicon.ico`, `/actuator/health` 等（见 `application.yml` 的 `jwt.ignore-paths`）。
- **令牌过期**：默认 `7` 天（`jwt.expiration`）。过期或非法令牌将返回 `401`。
- **安全建议**：不应通过查询参数传递令牌；避免在日志中打印令牌；移动端本地存储需加密与过期刷新。

### 5. 错误代码与处理
- `200` 成功
- `400` 参数错误（校验失败、格式不合法）
- `401` 未认证或令牌失效
- `403` 无权限
- `404` 资源不存在
- `409` 资源冲突（如分类重名）
- `429` 频率限制（详见速率限制）
- `500` 服务器内部错误
- 前端统一解析 `Result{success, code, message, data}`，失败时根据 `code` 做针对性提示与重试。

### 2. 用户登录
- **接口标识**：地址=`/auth/login`，方法=POST
- 描述：暂时只支持用户名+密码登录。
- **请求参数（JSON）**：
  | 参数名     | 类型   | 必传 | 说明                          | 示例值               |
  |------------|--------|------|-------------------------------|----------------------|
  | username   | String | 是   | 用户名                        | "zhangsan123"        |
  | password   | String | 是   | 登录密码                      | "Zhang@123456"       |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "登录成功",
    "data": {
      "userId": 10001,
      "username": "zhangsan123",
      "avatarPath": "files/greenfinance/avatars/10001.png",
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
  ```

### 3. 用户登出
- **接口标识**：地址=`/auth/logout`，方法=POST
- **请求参数**：仅需请求头`Authorization: Bearer {token}`
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "登出成功",
    "data": null
  }
  ```

## 三、用户信息模块（User Profile Module）

### 1. 获取用户信息
- **接口标识**：地址=`/user/profile`，方法=GET
- **请求参数**：请求头`Authorization: Bearer {token}`
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "success",
    "data": {
      "userId": 10001,
      "username": "zhangsan123",
      "email": "zhangsan@xxx.com",
      "phone": "13800138000",
      "avatarPath": "files/greenfinance/avatars/10001_1698000000.png",
      "createTime": "2023-10-23 14:30:00"
    }
  }
  ```

### 2. 更新用户信息
- **接口标识**：地址=`/user/profile`，方法=PUT
- **请求参数（JSON）**：
  | 参数名       | 类型   | 必传 | 说明                          | 示例值               |
  |--------------|--------|------|-------------------------------|----------------------|
  | email        | String | 否   | 新邮箱，需唯一且符合格式      | "new_zhangsan@xxx.com"|
  | phone        | String | 否   | 新手机号，11位数字            | "13900139000"        |
  | avatarPath   | String | 否   | 新头像本地相对路径（前端先存图）| "files/greenfinance/avatars/10001_1698100000.png" |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "用户信息更新成功",
    "data": null
  }
  ```

### 3. 修改密码
- **接口标识**：地址=`/user/password`，方法=PUT
- **请求参数（JSON）**：
  | 参数名       | 类型   | 必传 | 说明                          | 示例值               |
  |--------------|--------|------|-------------------------------|----------------------|
  | oldPassword  | String | 是   | 原密码                        | "Zhang@123456"       |
  | newPassword  | String | 是   | 新密码，6-20位含字母+数字     | "Zhang@654321"       |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "密码修改成功，请重新登录",
    "data": null
  }
  ```

## 四、分类管理模块（Category Module）

### 1. 获取所有分类（含子分类）
- **接口标识**：地址=`/categories`，方法=GET
- **请求参数**：请求头`Authorization: Bearer {token}`，查询参数type（可选，1=支出，2=收入）
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "name": "餐饮",
        "iconIdentifier": "res:ic_category_food",
        "type": 1,
        "sortOrder": 1,
        "subCategories": [
          {
            "id": 1,
            "categoryId": 1,
            "name": "快餐",
            "iconIdentifier": null,
            "sortOrder": 1
          },
          {
            "id": 2,
            "categoryId": 1,
            "name": "奶茶",
            "iconIdentifier": "file:files/greenfinance/icons/milk_tea.png",
            "sortOrder": 2
          }
        ]
      }
    ]
  }
  ```

### 2. 创建主分类
- **接口标识**：地址=`/categories`，方法=POST
- **请求参数（JSON）**：
  | 参数名         | 类型   | 必传 | 说明                          | 示例值               |
  |----------------|--------|------|-------------------------------|----------------------|
  | name           | String | 是   | 主分类名称，唯一              | "数码"               |
  | iconIdentifier | String | 是   | 图标标识（res:资源名/file:本地路径）| "res:ic_category_digital" |
  | type           | Integer| 是   | 1=支出，2=收入                | 1                    |
  | sortOrder      | Integer| 否   | 排序序号，默认0               | 3                    |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "主分类创建成功",
    "data": {
      "id": 9,
      "name": "数码",
      "iconIdentifier": "res:ic_category_digital",
      "type": 1,
      "sortOrder": 0
    }
  }
  ```

### 3. 更新主分类
- **接口标识**：地址=`/categories/{id}`，方法=PUT
- **请求参数（JSON）**：
  | 参数名         | 类型   | 必传 | 说明                          | 示例值               |
  |----------------|--------|------|-------------------------------|----------------------|
  | name           | String | 否   | 主分类名称，唯一              | "数码"               |
  | iconIdentifier | String | 否   | 图标标识（res:资源名/file:本地路径）| "res:ic_category_digital" |
  | type           | Integer| 否   | 1=支出，2=收入                | 1                    |
  | sortOrder      | Integer| 否   | 排序序号，默认0               | 3                    |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类更新成功",
    "data": {
      "id": 9,
      "name": "数码",
      "iconIdentifier": "res:ic_category_digital",
      "type": 1,
      "sortOrder": 3
    }
  }
  ```

### 4. 删除主分类
- **接口标识**：地址=`/categories/{id}`，方法=DELETE
- **请求参数**：路径参数id（主分类ID）
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类删除成功",
    "data": null
  }
  ```

### 5. 分类关键词管理（AI识别用）
- **接口标识**：地址=`/category-keywords`，方法=POST（新增）/GET（查询）/DELETE（删除）
- **新增关键词请求参数（JSON）**：
  | 参数名         | 类型   | 必传 | 说明                          | 示例值               |
  |----------------|--------|------|-------------------------------|----------------------|
  | categoryId     | Long   | 是   | 主分类ID                      | 1                    |
  | subCategoryId  | Long   | 否   | 子分类ID，可为null            | 1                    |
  | keyword        | String | 是   | 关键词（如商户名）            | "麦当劳"             |
- **响应示例（新增）**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "关键词添加成功",
    "data": {
      "id": 101,
      "keyword": "麦当劳",
      "categoryId": 1,
      "subCategoryId": 1
    }
  }
  ```

## 五、子分类管理模块（SubCategory Module）

### 1. 获取指定分类下的所有子分类
- **接口标识**：地址=`/sub-categories`，方法=GET
- **请求参数**：请求头`Authorization: Bearer {token}`，查询参数`categoryId`（主分类ID）
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "categoryId": 1,
        "name": "快餐",
        "iconIdentifier": null,
        "sortOrder": 1
      },
      {
        "id": 2,
        "categoryId": 1,
        "name": "奶茶",
        "iconIdentifier": "file:files/greenfinance/icons/milk_tea.png",
        "sortOrder": 2
      }
    ]
  }
  ```

### 2. 创建子分类
- **接口标识**：地址=`/sub-categories`，方法=POST
- **请求参数（JSON）**：
  | 参数名         | 类型   | 必传 | 说明                          | 示例值               |
  |----------------|--------|------|-------------------------------|----------------------|
  | categoryId     | Long   | 是   | 关联主分类ID                  | 9                    |
  | name           | String | 是   | 子分类名称，同一主分类下唯一  | "手机配件"           |
  | iconIdentifier | String | 否   | 图标标识，空则继承主分类      | "file:files/greenfinance/icons/phone_part.png" |
  | sortOrder      | Integer| 否   | 排序序号，默认0               | 1                    |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "子分类创建成功",
    "data": {
      "id": 21,
      "categoryId": 9,
      "name": "手机配件",
      "iconIdentifier": "file:files/greenfinance/icons/phone_part.png",
      "sortOrder": 1
    }
  }
  ```

### 3. 更新子分类
- **接口标识**：地址=`/sub-categories/{id}`，方法=PUT
- **请求参数（JSON）**：
  | 参数名         | 类型   | 必传 | 说明                          | 示例值               |
  |----------------|--------|------|-------------------------------|----------------------|
  | categoryId     | Long   | 否   | 关联主分类ID                  | 9                    |
  | name           | String | 否   | 子分类名称，同一主分类下唯一  | "手机配件"           |
  | iconIdentifier | String | 否   | 图标标识，空则继承主分类      | "file:files/greenfinance/icons/phone_part.png" |
  | sortOrder      | Integer| 否   | 排序序号，默认0               | 1                    |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "子分类更新成功",
    "data": {
      "id": 21,
      "categoryId": 9,
      "name": "手机配件",
      "iconIdentifier": "file:files/greenfinance/icons/phone_part.png",
      "sortOrder": 1
    }
  }
  ```

### 4. 删除子分类
- **接口标识**：地址=`/sub-categories/{id}`，方法=DELETE
- **请求参数**：路径参数id（子分类ID）
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "子分类删除成功",
    "data": null
  }
  ```

## 六、账单管理模块（Bill Module）

### 1. 创建账单
- **接口标识**：地址=`/bills/add`，方法=POST
- **请求参数（JSON）**：
  | 参数名            | 类型         | 必传 | 说明                         | 示例值                   |
  |----------------|------------|----|----------------------------|-----------------------|
  | originalAmount | BigDecimal | 是  | 原始金额（正数）                   | 50.00                 |
  | refundAmount   | BigDecimal | 否  | 退款金额，默认0，不超原始金额            | 10.00                 |
  | type           | Integer    | 是  | 1=支出，2=收入                  | 1                     |
  | categoryId     | Long       | 是  | 主分类ID（需存在且归属当前用户）          | 1                     |
  | subCategoryId  | Long       | 否  | 子分类ID，可为null（若传则需归属对应主分类）  | 1                     |
  | merchant       | String     | 否  | 商户名称（最长100字符）              | "麦当劳（科技园店）"           |
  | remark         | String     | 否  | 备注（最多500字符）                | "早餐：汉堡+可乐"            |
  | billTime       | String     | 是  | 账单时间，格式yyyy-MM-dd HH:mm:ss | "2023-10-25 08:30:00" |
  | paymentMethod  | String     | 否  | 支付方式（最长50字符）               | "微信支付"                |
  | orderNumber    | String     | 否  | 订单号（最长32字符）               | "202310250830001"         |

- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "账单创建成功",
  "data": {
    "id": 10001,
    "userId": 10001,
    "amount": 40.00,
    "originalAmount": 50.00,
    "refundAmount": 10.00,
    "type": 1,
    "categoryId": 1,
    "categoryName": "餐饮",
    "subCategoryId": 1,
    "subCategoryName": "快餐",
    "merchant": "麦当劳（科技园店）",
    "remark": "早餐：汉堡+可乐",
    "billTime": "2023-10-25 08:30:00",
    "paymentMethod": "微信支付",
    "orderNumber": "202310250830001",
    "createTime": "2023-10-25 08:35:00",
    "updateTime": "2023-10-25 08:35:00"
  }
}
```

### 2. 获取账单列表
- **接口标识**：地址=`/bills/list`，方法=GET
  - **请求参数**：
    | 参数名        | 类型      | 必传 | 说明                             | 示例值          |
    |------------|---------|----|--------------------------------|--------------|
    | page       | Integer | 否  | 页码，默认1                         | 1            |
    | size       | Integer | 否  | 每页条数，默认20                      | 20           |
    | startTime  | String  | 否  | 开始日期（格式yyyy-MM-dd） | "2023-10-01" |
    | endTime    | String  | 否  | 结束日期（格式yyyy-MM-dd） | "2023-10-31" |
    | categoryId | Long    | 否  | 主分类ID（筛选对应分类的账单）               | 1            | 
    | merchant   | String  | 否  | 商户名称（筛选对应商户的账单）模糊查询                 | "麦当劳（科技园店）" |
    | type       | Integer | 否  | 1=支出，2=收入（筛选对应类型的账单）           | 1            |

- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 10001,
        "userId": 10001,
        "amount": 40.00,
        "originalAmount": 50.00,
        "refundAmount": 10.00,
        "type": 1,
        "categoryId": 1,
        "categoryName": "餐饮",
        "subCategoryId": 1,
        "subCategoryName": "快餐",
        "merchant": "麦当劳（科技园店）",
        "remark": "早餐：汉堡+可乐",
        "billTime": "2023-10-25 08:30:00",
        "paymentMethod": "微信支付",
        "createTime": "2023-10-25 08:35:00",
        "updateTime": "2023-10-25 08:35:00"
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

### 3. 获取账单详情
- **接口标识**：地址=`/bills/{id}`，方法=GET
- **请求参数**：路径参数id（账单ID）
- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": {
    "id": 10001,
    "userId": 10001,
    "amount": 40.00,
    "originalAmount": 50.00,
    "refundAmount": 10.00,
    "type": 1,
    "categoryId": 1,
    "categoryName": "餐饮",
    "subCategoryId": 1,
    "subCategoryName": "快餐",
    "merchant": "麦当劳（科技园店）",
    "remark": "早餐：汉堡+可乐",
    "billTime": "2023-10-25 08:30:00",
    "paymentMethod": "微信支付",
    "createTime": "2023-10-25 08:35:00",
    "updateTime": "2023-10-25 08:35:00"
  }
}
```

### 4. 更新账单
- **接口标识**：地址=`/bills/{id}`，方法=PUT
- **请求参数**：
  - 路径参数：id（账单ID）
  - 请求体（JSON，参数规则同"创建账单"，所有参数非必传，仅传需修改的字段）

- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "账单更新成功",
  "data": {
    "id": 10001,
    "userId": 10001,
    "amount": 40.00,
    "originalAmount": 50.00,
    "refundAmount": 10.00,
    "type": 1,
    "categoryId": 1,
    "categoryName": "餐饮",
    "subCategoryId": 1,
    "subCategoryName": "快餐",
    "merchant": "麦当劳（科技园店）",
    "remark": "早餐：汉堡+可乐",
    "billTime": "2023-10-25 08:30:00",
    "paymentMethod": "微信支付",
    "orderNumber": "202310250830001",
    "createTime": "2023-10-25 08:35:00",
    "updateTime": "2023-10-25 08:35:00"
  }
}
```

### 5. 删除账单
- **接口标识**：地址=`/bills/{id}`，方法=DELETE
- **请求参数**：路径参数id（账单ID）
- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "账单删除成功",
  "data": null
}
```

## 七、智能记账模块（AI Billing Module）

### 1. OCR文本解析（截图记账）
- **接口标识**：地址=`/ai/ocr-parse`，方法=POST
- **请求参数（JSON）**：
  | 参数名       | 类型   | 必传 | 说明                          | 示例值               |
  |--------------|--------|------|-------------------------------|----------------------|
  | ocrText      | String | 是   | Google ML Kit识别的文本内容   | "微信支付 收款方：喜茶(科技园店) 金额：¥35.50 时间：2023-10-25 14:30:00" |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "OCR解析成功",
    "data": {
      "amount": 35.50,
      "type": 1,
      "categoryId": 1,
      "categoryName": "餐饮",
      "subCategoryId": 2,
      "subCategoryName": "奶茶",
      "merchant": "喜茶(科技园店)",
      "billTime": "2023-10-25 14:30:00",
      "paymentMethod": "微信支付"
    }
  }
  ```

## 七、AI与文本/图片记账模块（AI Module）

### 1. 文本记账
- **接口标识**：地址=`/ai/text-bookkeeping`，方法=POST
- **请求参数（JSON）**：
  | 参数名 | 类型 | 必传 | 说明 |
  |---|---|---|---|
  | text | String | 是 | 原始文本，如“古茗12r” |
  | recordType | Integer | 否 | 1=落库账单，2=预计支出；默认智能判定 |
  | billType | Integer | 否 | 1=支出，2=收入；可自动识别 |
  | categoryId/categoryName | Long/String | 否 | 传入则优先生效，允许自动创建 |
  | subCategoryId/subCategoryName | Long/String | 否 | 传入则优先生效，允许自动创建 |
  | merchant/paymentMethod/billTime/orderNumber/remark | 多种 | 否 | 手动覆盖解析结果 |
- **响应（JSON）**：
  - `preview`：解析预览结果（包含 `categoryId/name`、`subCategoryId/name`、`confidence`、`parseSource`）
  - `finalPayload`：最终落库载荷
  - `recordType`：1 或 2
  - `bill` 或 `expectedExpense`：落库结果
- **分类逻辑**：
  - 本地匹配优先：命中分类关键词或商户库直接返回；`confidence` 高（约 90）
  - 未命中→AI候选：召回 Top-N，选择在分类库中存在且分数最高者，置信度=分数×100
  - 原子写库：在事务内将 `text` 作为关键词写入分类库（去重），避免数据不一致

### 2. 图片记账
- **接口标识**：地址=`/ai/image-bookkeeping`，方法=POST
- **请求参数（JSON）**：`{ ocrText: String, ... }`
- **说明**：前端/客户端完成OCR后传入结构化文本，服务端解析并落库；同样支持本地优先+AI回退。

### 3. 速率限制与超时（AI调用）
- 默认频率：`5` 次/分钟（可通过 `ai.rate-limit.per-minute` 配置），超限返回 `429`。
- 超时机制：单次 AI 调用 10s 超时，超时回退本地结果并记录告警日志。
- 最佳实践：优先传入已知分类/子分类，减少AI调用；将常用商户加入关键词库提高本地命中率。

## 八、版本与变更记录（Changelog）
- `v1.0`：接口稳定版，包含用户、分类、子分类、账单等基础模块
- `v1.1`：新增 AI 文本/图片记账；引入本地优先+AI回退分类；支持 Top-N 候选与置信度；AI调用限流与超时；分类关键词原子入库

## 九、使用示例与最佳实践
- 文本记账：
  - 请求：`{"text":"古茗12r"}`
  - 建议：若希望落到“餐饮/奶茶”，可传 `{"text":"古茗12r","categoryName":"餐饮","subCategoryName":"奶茶"}` 提升准确性并自动创建缺失分类
- 错误处理：根据 `code` 与 `message` 友好提示；对 `429` 限流应退避重试
- 安全：令牌仅置于请求头，避免拼接到URL；敏感字段不入日志

## 十、速率限制与配额说明
- 通用接口：当前未启用速率限制（后续可按模块粒度引入）
- AI模块：启用每分钟配额，默认 `5` 次/分钟；可按用户维度扩展与差异化配置
- 监控与告警：服务端统计 AI 辅助次数与解析失败案例，定期输出周报日志便于优化

### 2. 文本描述解析（文本记账）
- **接口标识**：地址=`/ai/text-parse`，方法=POST
- **请求参数（JSON）**：
  | 参数名       | 类型   | 必传 | 说明                          | 示例值               |
  |--------------|--------|------|-------------------------------|----------------------|
  | text         | String | 是   | 用户输入的自然语言描述        | "今天下午2点半在肯德基花了45元吃炸鸡" |
- **响应示例**：
  ```json
  {
    "success": true,
    "code": 200,
    "message": "文本解析成功",
    "data": {
      "amount": 45.00,
      "type": 1,
      "categoryId": 1,
      "categoryName": "餐饮",
      "subCategoryId": 1,
      "subCategoryName": "快餐",
      "merchant": "肯德基",
      "billTime": "2023-10-25 14:30:00"
    }
  }
  ```

## 八、预计支出模块（Expected Expense Module）

### 1. 创建预计支出
- **接口标识**：地址=`/expected-expense`，方法=POST
- **请求参数（JSON）**：
  | 参数名            | 类型         | 必传 | 说明                         | 示例值                   |
  |----------------|------------|----|----------------------------|-----------------------|
  | amount         | BigDecimal | 是  | 预计金额                   | 500.00                 |
  | categoryId     | Long       | 是  | 主分类ID（需存在且归属当前用户）          | 1                     |
  | subCategoryId  | Long       | 否  | 子分类ID，可为null（若传则需归属对应主分类）  | 1                     |
  | remark         | String     | 否  | 备注（最多500字符）                | "本月房租"            |
  | dueDate        | String     | 是  | 预计支付日期，格式yyyy-MM-dd | "2023-10-25" |

- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "预计支出创建成功",
  "data": {
    "id": 10001,
    "userId": 10001,
    "amount": 500.00,
    "categoryId": 1,
    "categoryName": "生活",
    "subCategoryId": 1,
    "subCategoryName": "房租",
    "remark": "本月房租",
    "dueDate": "2023-10-25",
    "status": 1,
    "createTime": "2023-10-01 08:35:00",
    "updateTime": "2023-10-01 08:35:00"
  }
}
```

### 2. 获取预计支出列表
- **接口标识**：地址=`/expected-expense/list`，方法=GET
  - **请求参数**：
    | 参数名        | 类型      | 必传 | 说明                             | 示例值          |
    |------------|---------|----|--------------------------------|--------------|
    | status     | Integer | 否  | 状态：1-待支付，2-已支付，3-已取消  | 1            |
    | startTime  | String  | 否  | 开始日期（格式yyyy-MM-dd） | "2023-10-01" |
    | endTime    | String  | 否  | 结束日期（格式yyyy-MM-dd） | "2023-10-31" |

- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 10001,
      "userId": 10001,
      "amount": 500.00,
      "categoryId": 1,
      "categoryName": "生活",
      "subCategoryId": 1,
      "subCategoryName": "房租",
      "remark": "本月房租",
      "dueDate": "2023-10-25",
      "status": 1,
      "createTime": "2023-10-01 08:35:00",
      "updateTime": "2023-10-01 08:35:00"
    }
  ]
}
```

### 3. 获取预计支出详情
- **接口标识**：地址=`/expected-expense/{id}`，方法=GET
- **请求参数**：路径参数id（预计支出ID）
- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "success",
  "data": {
    "id": 10001,
    "userId": 10001,
    "amount": 500.00,
    "categoryId": 1,
    "categoryName": "生活",
    "subCategoryId": 1,
    "subCategoryName": "房租",
    "remark": "本月房租",
    "dueDate": "2023-10-25",
    "status": 1,
    "createTime": "2023-10-01 08:35:00",
    "updateTime": "2023-10-01 08:35:00"
  }
}
```

### 4. 更新预计支出
- **接口标识**：地址=`/expected-expense/{id}`，方法=PUT
- **请求参数**：
  - 路径参数：id（预计支出ID）
  - 请求体（JSON，参数规则同"创建预计支出"，所有参数非必传，仅传需修改的字段）

- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "预计支出更新成功",
  "data": {
    "id": 10001,
    "userId": 10001,
    "amount": 500.00,
    "categoryId": 1,
    "categoryName": "生活",
    "subCategoryId": 1,
    "subCategoryName": "房租",
    "remark": "本月房租",
    "dueDate": "2023-10-25",
    "status": 1,
    "createTime": "2023-10-01 08:35:00",
    "updateTime": "2023-10-01 08:35:00"
  }
}
```

### 5. 删除预计支出
- **接口标识**：地址=`/expected-expense/{id}`，方法=DELETE
- **请求参数**：路径参数id（预计支出ID）
- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "预计支出删除成功",
  "data": null
}
```

### 6. 确认预计支出（转为实际支出）
- **接口标识**：地址=`/expected-expense/confirm/{id}`，方法=POST
- **请求参数**：路径参数id（预计支出ID）
- **响应示例**：
```json
{
  "success": true,
  "code": 200,
  "message": "预计支出确认成功",
  "data": null
}
```

## 九、错误码与补充说明

### 1. 核心错误码
| 错误码 | 含义              | 示例场景             |
|-----|-----------------|------------------|
| 200 | 操作成功            | 账单创建成功           |
| 400 | 请求参数错误          | 密码格式不符合要求        |
| 401 | 未认证（Token过期/无效） | Token过期后调用用户信息接口 |
| 403 | 无权限             | 普通用户调用管理员接口      |
| 404 | 资源不存在           | 访问不存在的账单ID       |
| 500 | 服务端异常           | AI接口调用失败         |

### 2. 本地图片处理规则
- 前端通过`context.getFilesDir()` + 数据库存储的"相对路径"获取图片绝对路径（如`/data/user/0/com.example.greenfinance/files/greenfinance/avatars/10001.png`）
- 系统图标通过`R.drawable.资源名`加载（如`res:ic_category_food`对应`R.drawable.ic_category_food`）

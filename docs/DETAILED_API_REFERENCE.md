# GreenFinance 详细API接口参考文档

## 1. AI智能记账接口

### 1.1 自然语言文本记账
- **接口路径**: `POST /api/ai/text`
- **功能描述**: 解析自然语言文本，返回识别出的交易信息
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "text": "文本内容",
    "billtime": "可选的账单时间(yyyy-MM-dd HH:mm:ss)"
  }
  ```
- **请求体参数**:
  - `text` (String, 必填): 需要解析的自然语言文本，如"午饭20元"
  - `billtime` (String, 可选): 账单时间，格式为"yyyy-MM-dd HH:mm:ss"
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "amount": 20.00,
        "type": 1,
        "categoryId": 1,
        "remark": "午饭",
        "billTime": "2025-12-23 12:00:00"
      }
    ]
  }
  ```
- **错误响应**:
  - 400: 输入文本不能为空
  - 401: 请先登录

### 1.2 OCR批量记账
- **接口路径**: `POST /api/ai/ocr`
- **功能描述**: 接收OCR识别出的文本行列表，解析为交易信息
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "ocrTexts": ["文本1", "文本2", "..."]
  }
  ```
- **请求体参数**:
  - `ocrTexts` (Array<String>, 必填): OCR识别出的文本列表
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "amount": 25.50,
        "type": 1,
        "categoryId": 2,
        "remark": "超市购物",
        "billTime": "2025-12-23 10:30:00"
      }
    ]
  }
  ```
- **错误响应**:
  - 400: OCR文本列表不能为空
  - 401: 请先登录

### 1.3 获取财务建议
- **接口路径**: `GET /api/ai/advice`
- **功能描述**: 获取当前用户的月度支出统计及AI财务建议
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "monthlyExpenses": 1500.00,
      "savingsRate": 0.25,
      "advice": "您的支出在餐饮方面偏高，建议适当控制"
    }
  }
  ```
- **错误响应**:
  - 401: 请先登录

## 2. 账单管理接口

### 2.1 创建账单
- **接口路径**: `POST /api/bills/add`
- **功能描述**: 创建新的账单记录
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "amount": 50.00,
    "type": 1,
    "categoryId": 1,
    "subCategoryId": 2,
    "merchant": "7-Eleven",
    "remark": "购买饮料",
    "billTime": "2025-12-23 14:30:00"
  }
  ```
- **请求体参数**:
  - `amount` (BigDecimal, 必填): 金额
  - `type` (Integer, 必填): 账单类型 (1-支出, 2-收入)
  - `categoryId` (Long, 必填): 分类ID
  - `subCategoryId` (Long, 可选): 子分类ID
  - `merchant` (String, 可选): 商家名称
  - `remark` (String, 可选): 备注
  - `billTime` (String, 可选): 账单时间，默认为当前时间
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "账单创建成功",
    "data": {
      "id": 123,
      "amount": 50.00,
      "type": 1,
      "categoryId": 1,
      "subCategoryId": 2,
      "merchant": "7-Eleven",
      "remark": "购买饮料",
      "billTime": "2025-12-23 14:30:00",
      "createTime": "2025-12-23 14:30:00"
    }
  }
  ```

### 2.2 获取账单列表
- **接口路径**: `GET /api/bills/list`
- **功能描述**: 分页获取账单列表，支持按时间、分类筛选
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `page` (Integer, 可选, 默认: 1): 页码
  - `size` (Integer, 可选, 默认: 20): 每页数量
  - `type` (Integer, 可选): 账单类型 (1-支出, 2-收入)
  - `categoryId` (Long, 可选): 分类ID
  - `startDate` (String, 可选): 开始日期 (yyyy-MM-dd)
  - `endDate` (String, 可选): 结束日期 (yyyy-MM-dd)
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "records": [
        {
          "id": 123,
          "amount": 50.00,
          "type": 1,
          "categoryId": 1,
          "subCategoryId": 2,
          "merchant": "7-Eleven",
          "remark": "购买饮料",
          "billTime": "2025-12-23 14:30:00"
        }
      ],
      "total": 1,
      "size": 20,
      "current": 1,
      "pages": 1
    }
  }
  ```

### 2.3 获取账单详情
- **接口路径**: `GET /api/bills/{id}`
- **功能描述**: 获取指定ID的账单详情
- **路径参数**:
  - `id` (Long, 必填): 账单ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 123,
      "amount": 50.00,
      "type": 1,
      "categoryId": 1,
      "subCategoryId": 2,
      "merchant": "7-Eleven",
      "remark": "购买饮料",
      "billTime": "2025-12-23 14:30:00"
    }
  }
  ```
- **错误响应**:
  - 404: 账单不存在

### 2.4 更新账单
- **接口路径**: `PUT /api/bills/update`
- **功能描述**: 更新账单信息
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "id": 123,
    "amount": 55.00,
    "type": 1,
    "categoryId": 1,
    "subCategoryId": 2,
    "merchant": "7-Eleven",
    "remark": "购买饮料和零食",
    "billTime": "2025-12-23 14:30:00"
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "账单更新成功",
    "data": {
      "id": 123,
      "amount": 55.00,
      "type": 1,
      "categoryId": 1,
      "subCategoryId": 2,
      "merchant": "7-Eleven",
      "remark": "购买饮料和零食",
      "billTime": "2025-12-23 14:30:00"
    }
  }
  ```

### 2.5 删除账单
- **接口路径**: `DELETE /api/bills/{id}`
- **功能描述**: 删除指定ID的账单
- **路径参数**:
  - `id` (Long, 必填): 账单ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "账单删除成功"
  }
  ```
- **错误响应**:
  - 403: 无权限删除该账单
  - 404: 账单不存在

### 2.6 获取账单统计
- **接口路径**: `GET /api/bills/statistics`
- **功能描述**: 获取用户账单统计信息
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "totalIncome": 5000.00,
      "totalExpense": 3000.00,
      "currentMonthIncome": 2000.00,
      "currentMonthExpense": 1200.00
    }
  }
  ```

## 3. 预算管理接口

### 3.1 创建预算
- **接口路径**: `POST /api/budget`
- **功能描述**: 创建月度预算
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "categoryId": 1,
    "budgetAmount": 1000.00,
    "yearMonth": "2025-12"
  }
  ```
- **请求体参数**:
  - `categoryId` (Long, 必填): 分类ID
  - `budgetAmount` (BigDecimal, 必填): 预算金额
  - `yearMonth` (String, 可选): 年月格式(yyyy-MM)，默认为当前月
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预算创建成功",
    "data": {
      "id": 456,
      "categoryId": 1,
      "budgetAmount": 1000.00,
      "usedAmount": 0.00,
      "yearMonth": "2025-12",
      "userId": 1
    }
  }
  ```

### 3.2 获取预算列表
- **接口路径**: `GET /api/budget`
- **功能描述**: 分页获取预算列表
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `page` (Integer, 可选, 默认: 1): 页码
  - `size` (Integer, 可选, 默认: 20): 每页数量
  - `categoryId` (Long, 可选): 分类ID
  - `yearMonth` (String, 可选): 年月格式(yyyy-MM)
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "records": [
        {
          "id": 456,
          "categoryId": 1,
          "categoryName": "餐饮",
          "budgetAmount": 1000.00,
          "usedAmount": 350.00,
          "yearMonth": "2025-12"
        }
      ],
      "total": 1,
      "size": 20,
      "current": 1,
      "pages": 1
    }
  }
  ```

### 3.3 获取预算详情
- **接口路径**: `GET /api/budget/{id}`
- **功能描述**: 获取指定ID的预算详情
- **路径参数**:
  - `id` (Long, 必填): 预算ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 456,
      "categoryId": 1,
      "categoryName": "餐饮",
      "budgetAmount": 1000.00,
      "usedAmount": 350.00,
      "yearMonth": "2025-12",
      "userId": 1
    }
  }
  ```
- **错误响应**:
  - 404: 预算不存在

### 3.4 更新预算
- **接口路径**: `PUT /api/budget`
- **功能描述**: 更新预算信息
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "id": 456,
    "budgetAmount": 1200.00
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预算更新成功",
    "data": {
      "id": 456,
      "categoryId": 1,
      "budgetAmount": 1200.00,
      "usedAmount": 350.00,
      "yearMonth": "2025-12"
    }
  }
  ```

### 3.5 删除预算
- **接口路径**: `DELETE /api/budget/{id}`
- **功能描述**: 删除指定ID的预算
- **路径参数**:
  - `id` (Long, 必填): 预算ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预算删除成功"
  }
  ```
- **错误响应**:
  - 403: 无权限删除该预算
  - 404: 预算不存在

### 3.6 获取预算统计
- **接口路径**: `GET /api/budget/statistics`
- **功能描述**: 获取预算执行情况统计
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `yearMonth` (String, 可选): 年月格式(yyyy-MM)，默认为当前月
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "totalBudget": 5000.00,
      "totalSpent": 3200.00,
      "remainingBudget": 1800.00,
      "budgetItems": [
        {
          "categoryName": "餐饮",
          "budgetAmount": 1000.00,
          "spentAmount": 850.00,
          "percentage": 85.0
        }
      ]
    }
  }
  ```

## 4. 用户认证与管理接口

### 4.1 用户登录
- **接口路径**: `POST /api/auth/login`
- **功能描述**: 用户登录并获取Token
- **请求体**:
  ```json
  {
    "username": "user@example.com",
    "password": "password123"
  }
  ```
- **请求体参数**:
  - `username` (String, 必填): 用户名或邮箱
  - `password` (String, 必填): 密码
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 1,
        "username": "user@example.com",
        "nickname": "张三",
        "avatar": "/files/avatars/user1.jpg"
      }
    }
  }
  ```

### 4.2 用户注册
- **接口路径**: `POST /api/auth/register`
- **功能描述**: 用户注册
- **请求体**:
  ```json
  {
    "username": "user@example.com",
    "password": "password123",
    "nickname": "张三"
  }
  ```
- **请求体参数**:
  - `username` (String, 必填): 用户名或邮箱
  - `password` (String, 必填): 密码
  - `nickname` (String, 可选): 昵称
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "注册成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 2,
        "username": "user@example.com",
        "nickname": "张三"
      }
    }
  }
  ```

### 4.3 用户登出
- **接口路径**: `POST /api/auth/logout`
- **功能描述**: 用户登出
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "登出成功"
  }
  ```

### 4.4 获取用户详情
- **接口路径**: `GET /api/user/profile`
- **功能描述**: 获取当前用户详情
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "username": "user@example.com",
      "nickname": "张三",
      "avatar": "/files/avatars/user1.jpg",
      "createTime": "2025-10-25 10:00:00"
    }
  }
  ```

### 4.5 更新用户信息
- **接口路径**: `PUT /api/user/profile`
- **功能描述**: 更新用户信息
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "nickname": "李四",
    "phone": "13800138000"
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "用户更新成功",
    "data": {
      "id": 1,
      "username": "user@example.com",
      "nickname": "李四",
      "phone": "13800138000",
      "avatar": "/files/avatars/user1.jpg"
    }
  }
  ```

### 4.6 上传用户头像
- **接口路径**: `POST /api/user/avatar`
- **功能描述**: 上传用户头像
- **请求头**:
  - `Authorization: Bearer <token>`
- **请求格式**: `multipart/form-data`
- **参数**:
  - `file` (File, 必填): 头像文件，支持JPG/PNG格式，最大2MB
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "头像上传成功",
    "data": {
      "avatarPath": "/files/greenfinance/avatars/abc123.jpg"
    }
  }
  ```
- **错误响应**:
  - 400: 文件格式错误或文件过大

### 4.7 取消用户注销申请
- **接口路径**: `POST /api/user/cancel-deactivation`
- **功能描述**: 取消用户注销申请
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "注销申请已取消"
  }
  ```

## 5. 分类管理接口

### 5.1 获取分类列表
- **接口路径**: `GET /api/categories`
- **功能描述**: 获取所有分类，支持按类型筛选
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `type` (Integer, 可选): 分类类型 (1-支出, 2-收入)
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "name": "餐饮",
        "type": 1,
        "icon": "restaurant",
        "subCategories": [
          {
            "id": 1,
            "name": "早餐",
            "categoryId": 1
          }
        ]
      }
    ]
  }
  ```

### 5.2 创建分类
- **接口路径**: `POST /api/categories`
- **功能描述**: 创建主分类
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "name": "交通",
    "type": 1,
    "icon": "directions_car"
  }
  ```
- **请求体参数**:
  - `name` (String, 必填): 分类名称
  - `type` (Integer, 必填): 分类类型 (1-支出, 2-收入)
  - `icon` (String, 可选): 分类图标
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "主分类创建成功",
    "data": {
      "id": 2,
      "name": "交通",
      "type": 1,
      "icon": "directions_car",
      "userId": 1
    }
  }
  ```

### 5.3 更新分类
- **接口路径**: `PUT /api/categories/update`
- **功能描述**: 更新主分类
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "id": 2,
    "name": "出行交通",
    "icon": "commute"
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类更新成功",
    "data": {
      "id": 2,
      "name": "出行交通",
      "type": 1,
      "icon": "commute",
      "userId": 1
    }
  }
  ```

### 5.4 删除分类
- **接口路径**: `DELETE /api/categories/{id}`
- **功能描述**: 删除主分类
- **路径参数**:
  - `id` (Long, 必填): 分类ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类删除成功"
  }
  ```
- **错误响应**:
  - 403: 无权限删除该分类
  - 404: 分类不存在

## 6. 子分类管理接口

### 6.1 获取子分类列表
- **接口路径**: `GET /api/sub-categories`
- **功能描述**: 获取指定分类下的所有子分类
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `categoryId` (Long, 必填): 主分类ID
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "name": "早餐",
        "categoryId": 1,
        "userId": 1
      }
    ]
  }
  ```

### 6.2 创建子分类
- **接口路径**: `POST /api/sub-categories`
- **功能描述**: 创建子分类
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "name": "午餐",
    "categoryId": 1
  }
  ```
- **请求体参数**:
  - `name` (String, 必填): 子分类名称
  - `categoryId` (Long, 必填): 所属主分类ID
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "子分类创建成功",
    "data": {
      "id": 2,
      "name": "午餐",
      "categoryId": 1,
      "userId": 1
    }
  }
  ```

### 6.3 更新子分类
- **接口路径**: `PUT /api/sub-categories/update`
- **功能描述**: 更新子分类
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "id": 2,
    "name": "工作午餐",
    "categoryId": 1
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "子分类更新成功",
    "data": {
      "id": 2,
      "name": "工作午餐",
      "categoryId": 1,
      "userId": 1
    }
  }
  ```

### 6.4 删除子分类
- **接口路径**: `DELETE /api/sub-categories/{id}`
- **功能描述**: 删除子分类
- **路径参数**:
  - `id` (Long, 必填): 子分类ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "子分类删除成功"
  }
  ```
- **错误响应**:
  - 403: 无权限删除该子分类
  - 404: 子分类不存在

## 7. 分类关键词管理接口

### 7.1 创建分类关键词
- **接口路径**: `POST /api/category-keywords`
- **功能描述**: 创建分类关键词
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "categoryId": 1,
    "keyword": "麦当劳"
  }
  ```
- **请求体参数**:
  - `categoryId` (Long, 必填): 分类ID
  - `keyword` (String, 必填): 关键词
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类关键词创建成功",
    "data": {
      "id": 1,
      "categoryId": 1,
      "keyword": "麦当劳",
      "userId": 1
    }
  }
  ```

### 7.2 获取分类关键词列表
- **接口路径**: `GET /api/category-keywords`
- **功能描述**: 分页获取分类关键词列表
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `page` (Integer, 可选, 默认: 1): 页码
  - `size` (Integer, 可选, 默认: 20): 每页数量
  - `categoryId` (Long, 可选): 分类ID
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "records": [
        {
          "id": 1,
          "categoryId": 1,
          "keyword": "麦当劳",
          "userId": 1
        }
      ],
      "total": 1,
      "size": 20,
      "current": 1,
      "pages": 1
    }
  }
  ```

### 7.3 获取分类关键词详情
- **接口路径**: `GET /api/category-keywords/{id}`
- **功能描述**: 获取指定ID的分类关键词详情
- **路径参数**:
  - `id` (Long, 必填): 分类关键词ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "categoryId": 1,
      "categoryName": "餐饮",
      "keyword": "麦当劳",
      "userId": 1
    }
  }
  ```
- **错误响应**:
  - 404: 分类关键词不存在

### 7.4 更新分类关键词
- **接口路径**: `PUT /api/category-keywords`
- **功能描述**: 更新分类关键词
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "id": 1,
    "categoryId": 1,
    "keyword": "肯德基"
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类关键词更新成功",
    "data": {
      "id": 1,
      "categoryId": 1,
      "keyword": "肯德基",
      "userId": 1
    }
  }
  ```

### 7.5 删除分类关键词
- **接口路径**: `DELETE /api/category-keywords/{id}`
- **功能描述**: 删除分类关键词
- **路径参数**:
  - `id` (Long, 必填): 分类关键词ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "分类关键词删除成功"
  }
  ```
- **错误响应**:
  - 403: 无权限删除该分类关键词
  - 404: 分类关键词不存在

## 8. 预计支出管理接口

### 8.1 创建预计支出
- **接口路径**: `POST /api/expected-expense`
- **功能描述**: 创建预计支出
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "amount": 200.00,
    "categoryId": 1,
    "subCategoryId": 1,
    "description": "预计午餐费用",
    "expectedDate": "2025-12-24"
  }
  ```
- **请求体参数**:
  - `amount` (BigDecimal, 必填): 预计金额
  - `categoryId` (Long, 必填): 分类ID
  - `subCategoryId` (Long, 可选): 子分类ID
  - `description` (String, 可选): 描述
  - `expectedDate` (String, 可选): 预计日期 (yyyy-MM-dd)
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预计支出创建成功",
    "data": {
      "id": 1,
      "amount": 200.00,
      "categoryId": 1,
      "subCategoryId": 1,
      "description": "预计午餐费用",
      "expectedDate": "2025-12-24",
      "userId": 1
    }
  }
  ```

### 8.2 获取预计支出详情
- **接口路径**: `GET /api/expected-expense/{id}`
- **功能描述**: 获取指定ID的预计支出详情
- **路径参数**:
  - `id` (Long, 必填): 预计支出ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "amount": 200.00,
      "categoryId": 1,
      "subCategoryId": 1,
      "description": "预计午餐费用",
      "expectedDate": "2025-12-24",
      "userId": 1
    }
  }
  ```
- **错误响应**:
  - 404: 预计支出不存在

### 8.3 获取预计支出列表
- **接口路径**: `GET /api/expected-expense/list`
- **功能描述**: 获取预计支出列表
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `categoryId` (Long, 可选): 分类ID
  - `startDate` (String, 可选): 开始日期 (yyyy-MM-dd)
  - `endDate` (String, 可选): 结束日期 (yyyy-MM-dd)
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "amount": 200.00,
        "categoryId": 1,
        "subCategoryId": 1,
        "description": "预计午餐费用",
        "expectedDate": "2025-12-24",
        "userId": 1
      }
    ]
  }
  ```

### 8.4 更新预计支出
- **接口路径**: `PUT /api/expected-expense/{id}`
- **功能描述**: 更新预计支出
- **路径参数**:
  - `id` (Long, 必填): 预计支出ID
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "amount": 250.00,
    "description": "预计午餐及饮料费用"
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预计支出更新成功",
    "data": {
      "id": 1,
      "amount": 250.00,
      "categoryId": 1,
      "subCategoryId": 1,
      "description": "预计午餐及饮料费用",
      "expectedDate": "2025-12-24",
      "userId": 1
    }
  }
  ```
- **错误响应**:
  - 404: 预计支出不存在或无权限操作

### 8.5 删除预计支出
- **接口路径**: `DELETE /api/expected-expense/{id}`
- **功能描述**: 删除预计支出
- **路径参数**:
  - `id` (Long, 必填): 预计支出ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预计支出删除成功"
  }
  ```
- **错误响应**:
  - 404: 预计支出不存在或无权限操作

### 8.6 确认预计支出
- **接口路径**: `POST /api/expected-expense/confirm/{id}`
- **功能描述**: 确认预计支出，创建实际支出
- **路径参数**:
  - `id` (Long, 必填): 预计支出ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "预计支出确认成功"
  }
  ```
- **错误响应**:
  - 404: 预计支出不存在或无权限操作

## 9. 系统配置管理接口

### 9.1 创建系统配置
- **接口路径**: `POST /api/system-configs`
- **功能描述**: 创建系统配置
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "configKey": "theme",
    "configValue": "dark",
    "configDesc": "主题配置"
  }
  ```
- **请求体参数**:
  - `configKey` (String, 必填): 配置键
  - `configValue` (String, 必填): 配置值
  - `configDesc` (String, 可选): 配置描述
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "系统配置创建成功",
    "data": {
      "id": 1,
      "configKey": "theme",
      "configValue": "dark",
      "configDesc": "主题配置",
      "userId": 1
    }
  }
  ```

### 9.2 获取系统配置列表
- **接口路径**: `GET /api/system-configs`
- **功能描述**: 分页获取系统配置列表
- **请求头**:
  - `Authorization: Bearer <token>`
- **查询参数**:
  - `page` (Integer, 可选, 默认: 1): 页码
  - `size` (Integer, 可选, 默认: 20): 每页数量
  - `configKey` (String, 可选): 配置键
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "records": [
        {
          "id": 1,
          "configKey": "theme",
          "configValue": "dark",
          "configDesc": "主题配置",
          "userId": 1
        }
      ],
      "total": 1,
      "size": 20,
      "current": 1,
      "pages": 1
    }
  }
  ```

### 9.3 获取系统配置详情
- **接口路径**: `GET /api/system-configs/{id}`
- **功能描述**: 获取指定ID的系统配置详情
- **路径参数**:
  - `id` (Long, 必填): 系统配置ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "configKey": "theme",
      "configValue": "dark",
      "configDesc": "主题配置",
      "userId": 1
    }
  }
  ```
- **错误响应**:
  - 404: 系统配置不存在

### 9.4 更新系统配置
- **接口路径**: `PUT /api/system-configs`
- **功能描述**: 更新系统配置
- **请求头**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **请求体**:
  ```json
  {
    "id": 1,
    "configValue": "light"
  }
  ```
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "系统配置更新成功",
    "data": {
      "id": 1,
      "configKey": "theme",
      "configValue": "light",
      "configDesc": "主题配置",
      "userId": 1
    }
  }
  ```

### 9.5 删除系统配置
- **接口路径**: `DELETE /api/system-configs/{id}`
- **功能描述**: 删除系统配置
- **路径参数**:
  - `id` (Long, 必填): 系统配置ID
- **请求头**:
  - `Authorization: Bearer <token>`
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "系统配置删除成功"
  }
  ```
- **错误响应**:
  - 403: 无权限删除该系统配置
  - 404: 系统配置不存在

### 9.6 根据配置键获取配置值
- **接口路径**: `GET /api/system-configs/value`
- **功能描述**: 根据配置键获取配置值
- **查询参数**:
  - `configKey` (String, 必填): 配置键
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": "light"
  }
  ```
- **错误响应**:
  - 404: 配置项不存在

## 10. 测试接口

### 10.1 HanLP分词测试
- **接口路径**: `GET /api/test-hanlp/segment`
- **功能描述**: HanLP分词测试接口
- **查询参数**:
  - `text` (String, 必填): 需要分词的文本
- **响应示例**:
  ```json
  {
    "success": true,
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "word": "测试",
        "nature": "n"
      },
      {
        "word": "文本",
        "nature": "n"
      }
    ]
  }
  ```
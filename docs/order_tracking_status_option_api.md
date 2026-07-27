# 订单追踪状态配置接口说明

## 设计原则

- `quote.t_order_tracking_option` 是订单追踪类型和状态的唯一数据源。
- Java 不再维护类型值、状态码、状态名称或默认状态枚举。
- `TrackModuleEnum` 只表示固定业务模块，用于绑定实体字段和服务分支。
- 后端只读取 `is_deleted = 0 AND is_enabled = 1` 的基础数据。
- 通过状态修改接口保存成功后，后端会在事务提交后自动刷新翻译缓存。

## 1. 订单追踪表单配置

```http
GET /order-tracking/options
```

权限：

```text
order-tracking:page
```

此接口保持原结构，用于订单追踪列表、筛选项和编辑表单。

响应中的 `data` 类型：

```ts
type OrderTrackingOption = {
  moduleType: string
  optionType: 'TYPE' | 'STATUS'
  parentValue: number | null
  value: number
  label: string
  sortNo: number
  tagType: 'info' | 'warning' | 'success' | 'danger' | 'primary'
  remark: string | null
}

type OrderTrackingOptionMap = Record<string, OrderTrackingOption[]>
```

## 2. 查看全部启用状态配置

```http
GET /order-tracking/options/statuses
```

权限：

```text
order-tracking:page
```

响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "d149edba792c11f1905f005056c00001",
      "moduleType": "STICKER",
      "moduleLabel": "不干胶",
      "parentValue": 30,
      "parentLabel": "工厂生产",
      "optionValue": 3200,
      "optionLabel": "客户已提供设计资料",
      "sortNo": 20,
      "tagType": "warning",
      "remark": null,
      "updateTime": "2026-07-25T19:30:00",
      "updateBy": "用户ID"
    }
  ]
}
```

TypeScript 类型：

```ts
type OrderTrackingStatusOption = {
  id: string
  moduleType: string
  moduleLabel: string
  parentValue: number | null
  parentLabel: string
  optionValue: number
  optionLabel: string
  sortNo: number
  tagType: 'info' | 'warning' | 'success' | 'danger' | 'primary'
  remark: string | null
  updateTime: string | null
  updateBy: string | null
}
```

## 3. 修改状态配置

```http
PUT /order-tracking/options/statuses/{id}
Content-Type: application/json
```

权限：

```text
order-tracking:update
```

请求：

```json
{
  "optionValue": 3210,
  "optionLabel": "设计资料已接收",
  "sortNo": 25,
  "tagType": "primary",
  "remark": "前端维护的状态说明"
}
```

请求类型：

```ts
type UpdateOrderTrackingStatusOptionRequest = {
  optionValue: number
  optionLabel: string
  sortNo: number
  tagType: 'info' | 'warning' | 'success' | 'danger' | 'primary'
  remark?: string | null
}
```

校验规则：

- `optionValue` 必须是大于 `0` 的整数。
- 同一 `moduleType + parentValue` 下不能存在重复状态码。
- `optionLabel` 必填，最长 `200` 个字符。
- `sortNo` 必填。
- `tagType` 只能使用规定的五种值。
- `remark` 最长 `500` 个字符。
- 接口只允许修改 `option_type = STATUS` 的启用配置。

成功响应的 `data` 为更新后的完整 `OrderTrackingStatusOption`。

## 前端开发要求

1. 在订单追踪页面增加“状态配置”入口。
2. 调用 `GET /order-tracking/options/statuses` 展示模块、适用类型、状态码、名称、排序、标签和备注。
3. 有 `order-tracking:update` 权限时允许编辑；只有查询权限时只读展示。
4. 保存时调用 `PUT /order-tracking/options/statuses/{id}`。
5. 保存成功后重新调用：

```http
GET /order-tracking/options
```

刷新订单表单和筛选条件使用的配置。

6. 前端不得设置状态码、默认状态或状态文案兜底值；默认值取对应配置数组按 `sortNo` 排序后的第一项。
7. 不需要再调用 `/order-tracking/translate/cache/refresh`，修改接口会自动刷新后端缓存。

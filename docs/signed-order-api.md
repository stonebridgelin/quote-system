# 签单管理 API 对接文档

## 1. 通用约定

- 接口前缀：`/api/signed-order`
- 请求头：`Authorization: Bearer <accessToken>`
- 日期格式：`yyyy-MM-dd`
- 时间格式：`yyyy-MM-dd HH:mm:ss`
- 币种：`CNY`、`USD`
- 明细类型：`SINGLE`、`SET`
- 订单状态：`ACTIVE`、`CANCELLED`
- 重量单位：克（g）

统一响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

业务异常仍使用相同结构，常见错误码：

- `400`：请求字段或业务数据不合法。
- `404`：订单不存在。
- `409`：订单状态发生并发变化。
- `500`：系统异常。

## 2. 分页筛选和金额统计

```http
POST /api/signed-order/page
```

权限：`signed-order:list`

请求示例：

```json
{
  "current": 1,
  "size": 15,
  "customerName": "客户A",
  "orderNo": "SO-2026",
  "salesmanId": "77dbdacbc9cc456c13e056dddfea3163",
  "createDateStart": "2026-07-01",
  "createDateEnd": "2026-07-31",
  "amountMin": "1000.00",
  "amountMax": "50000.00"
}
```

所有筛选字段均可省略。`current`默认1，`size`默认15，最大200。

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "current": 1,
    "size": 15,
    "total": 2,
    "records": [
      {
        "id": "8e8e3ca8704a4a98a432151ac9f13a64",
        "orderNo": "SO-2026-001",
        "customerId": "379c503b26ea4381a59d9c06c09e4cf0",
        "customerName": "客户A",
        "salesmanId": "77dbdacbc9cc456c13e056dddfea3163",
        "salesmanName": "张三",
        "createDate": "2026-07-23",
        "currency": "CNY",
        "totalPcs": 108,
        "totalSets": 10,
        "totalAmount": 262.68,
        "status": "ACTIVE",
        "cancelTime": null,
        "cancelReason": null,
        "remark": "首批订单",
        "createTime": "2026-07-23 10:00:00",
        "updateTime": "2026-07-23 10:00:00",
        "creator": "lin"
      }
    ],
    "cnyTotal": 262.68,
    "usdTotal": 1250.00
  }
}
```

`cnyTotal`和`usdTotal`统计符合当前全部筛选条件的有效订单，不受分页影响。`CANCELLED`订单始终不计入这两个金额。

## 3. 新增订单

```http
POST /api/signed-order/create
```

权限：`signed-order:create`

请求示例：

```json
{
  "orderNo": "SO-2026-001",
  "customerId": null,
  "customerName": "客户A",
  "salesmanId": "77dbdacbc9cc456c13e056dddfea3163",
  "createDate": "2026-07-23",
  "currency": "CNY",
  "remark": "首批订单",
  "detailList": [
    {
      "lineType": "SINGLE",
      "productCode": "FLMLPP100",
      "weight": "410",
      "totalPcs": 48,
      "totalSets": 0,
      "unitPrice": "2.583",
      "setItems": []
    },
    {
      "lineType": "SET",
      "productCode": null,
      "totalSets": 10,
      "unitPrice": "13.87",
      "setItems": [
        {
          "productCode": "FLMLPP100",
          "weight": "410",
          "qtyPerSet": 2
        },
        {
          "productCode": "BOWL080",
          "weight": "300",
          "qtyPerSet": 4
        }
      ]
    }
  ]
}
```

客户处理：

- 从下拉框选择已有客户时，同时提交`customerId`和`customerName`。
- 自由输入新客户时，`customerId`传`null`，后端根据`customerName`自动去重并新增客户。
- 如果提交了`customerId`，它必须与`customerName`对应。

后端不会接收或信任主表的`totalPcs`、`totalSets`、`totalAmount`，也不会信任明细的`amount`。

计算规则：

```text
SINGLE:
totalPcs = 请求中的 totalPcs
totalSets = 0
amount = totalPcs × unitPrice

SET:
每套件数 = Σ setItem.qtyPerSet
每套重量 = Σ(setItem.weight × setItem.qtyPerSet)
totalPcs = totalSets × 每套件数
amount = totalSets × unitPrice

订单:
totalPcs = Σ 明细totalPcs
totalSets = Σ SET明细totalSets
totalAmount = Σ 明细amount
```

金额使用`BigDecimal`：

- `unitPrice`保存4位小数。
- 行金额和订单总金额保留2位小数。
- 舍入方式为`HALF_UP`。

套装至少需要两种组成商品，同一套装内不能重复提交相同`productCode`。SET父明细没有独立商品代号，价格只保存在父明细中，组成商品不保存价格。

成功响应的`data`为新订单ID：

```json
{
  "code": 200,
  "message": "订单保存成功",
  "data": "8e8e3ca8704a4a98a432151ac9f13a64"
}
```

## 4. 查询订单详情

```http
GET /api/signed-order/detail/{id}
```

权限：`signed-order:detail`

响应中的`detailList`按`sortNo`排序。SET明细的`setItems`包含套装组成，SINGLE明细的`setItems`为空数组。

## 5. 取消订单

```http
POST /api/signed-order/cancel/{id}
```

权限：`signed-order:cancel`

请求：

```json
{
  "cancelReason": "客户取消采购计划"
}
```

取消操作只修改：

- `status = CANCELLED`
- `cancelTime`
- `cancelReason`
- `updateTime`

订单原金额和商品数据不会被清空，但后续有效签单统计会自动排除该订单。重复取消按成功处理。

## 6. 客户下拉选项

```http
GET /api/signed-order/customer/options?keyword=客户&limit=20
```

权限：`signed-order:customer-options`

响应：

```json
[
  {
    "id": "379c503b26ea4381a59d9c06c09e4cf0",
    "customerName": "客户A"
  }
]
```

## 7. 业务员下拉选项

```http
GET /api/signed-order/salesman/options?keyword=张&limit=20
```

权限：`signed-order:salesman-options`

只返回启用且未删除的系统用户：

```json
[
  {
    "id": "77dbdacbc9cc456c13e056dddfea3163",
    "username": "sales01",
    "displayName": "张三"
  }
]
```

`displayName`依次取真实姓名、昵称、登录账号。

## 8. 部署顺序

1. 在`quote`数据库执行`src/main/resources/sql/signed_order_tables.sql`。
2. 在`quote_auth`数据库执行`src/main/resources/sql/signed_order_permissions.sql`。
3. 重新登录，让登录返回的权限列表包含新权限。
4. 部署后端。
5. 部署前端页面。

权限脚本会为角色ID`5510bc7c811c11f196fa005056c00001`的admin角色授予签单模块全部权限。

# 签单管理前端开发指导

## 1. 建议文件

建议新增：

```text
src/api/signedOrder.js
src/views/signedOrder/SignedOrderList.vue
src/views/signedOrder/SignedOrderCreateDialog.vue
src/views/signedOrder/SignedOrderDetailDialog.vue
```

产品选择继续复用：

```text
src/views/quote/AddProductDialog.vue
src/views/quote/AddSetProductDialog.vue
```

在`src/router/index.js`的`BUSINESS_ROUTES`中增加：

```js
{
    path: '/signed-order',
    name: 'SignedOrder',
    component: () =>
        import('../views/signedOrder/SignedOrderList.vue'),
    meta: {
        requiresAuth: true,
        mode: 'business',
        title: '签单管理',
        permission: 'signed-order:page'
    }
}
```

## 2. 列表页面

搜索区域字段：

- 客户名称：模糊查询。
- 订单编号：模糊查询。
- 业务员：远程下拉选择。
- 创建日期：起止日期范围。
- 订单金额：最小值和最大值。

不放置搜索按钮。监听筛选模型，条件变化后等待1000毫秒再请求：

```js
let queryTimer = null
let requestSequence = 0

watch(
    filters,
    () => {
        clearTimeout(queryTimer)
        queryTimer = setTimeout(() => {
            page.current = 1
            loadPage()
        }, 1000)
    },
    { deep: true }
)

async function loadPage() {
    const sequence = ++requestSequence
    const result = await pageSignedOrders(buildQuery())

    // 丢弃旧请求晚于新请求返回的结果
    if (sequence !== requestSequence) return

    records.value = result.records || []
    page.total = result.total || 0
    cnyTotal.value = result.cnyTotal ?? 0
    usdTotal.value = result.usdTotal ?? 0
}
```

分页切换应立即请求，不需要等待一秒；只有筛选条件变化才重置到第一页。

列表建议展示：

- 订单编号
- 客户名称
- 业务员
- 创建日期
- 币种
- 总件数
- 总套数
- 总金额
- 状态
- 创建人
- 创建时间
- 操作

页面底部直接展示接口返回的：

```text
人民币有效订单总额：cnyTotal
美元有效订单总额：usdTotal
```

不要用当前页`records`自行求和。

## 3. 新增订单弹窗

主信息字段：

- 订单编号：必填。
- 业务员：必选，调用业务员选项接口。
- 客户名称：可查询、可自由输入。
- 创建日期：默认当天。
- 币种：`CNY`、`USD`二选一。
- 备注：选填。

以下字段只能展示，不能让用户直接修改：

- `totalPcs`
- `totalSets`
- `totalAmount`
- 每条明细的`amount`
- SET明细的`weight`和`totalPcs`

### 客户输入

客户组件建议使用`el-autocomplete`：

1. 输入内容时调用`/customer/options`。
2. 选择已有客户后保存`customerId`和`customerName`。
3. 用户修改了已选择的文本时，立即清空`customerId`。
4. 保存时如果`customerId`为空，后端会按`customerName`新增客户。

### 业务员输入

业务员组件使用远程`el-select`，调用`/salesman/options`。提交时只需要保存选中项的`id`到`salesmanId`，业务员名称由后端从系统用户表读取并生成快照。

## 4. 复用 AddProductDialog.vue

现有组件：

```js
emit('confirm', selectedSpecs)
```

它一次可以返回多个产品。每个产品转换为一条SINGLE编辑行：

```js
function onProductsConfirm(products) {
    for (const product of products) {
        detailList.value.push({
            lineType: 'SINGLE',
            productCode: product.specCode,
            weight: product.weight,
            totalPcs: null,   // 用户填写订购件数
            totalSets: 0,
            unitPrice: null,  // 用户填写成交单价
            amount: '0.00',   // 仅前端预览，不提交
            setItems: []
        })
    }
}
```

`tonPrice`是产品基础吨价，不等于签单成交单价。本模块不能直接把`tonPrice`作为`unitPrice`提交，除非业务方另行确认新的价格换算规则。

## 5. 复用 AddSetProductDialog.vue

现有组件会返回至少两种产品，每个产品包含：

- `specCode`
- `weight`
- `qtyPerSet`

确认后生成一条SET父明细：

```js
function onSetProductsConfirm(products) {
    detailList.value.push({
        lineType: 'SET',
        productCode: null,
        totalSets: null,  // 用户填写本次订购套数
        unitPrice: null,  // 用户填写整套成交价格
        amount: '0.00',
        setItems: products.map(product => ({
            productCode: product.specCode,
            weight: product.weight,
            qtyPerSet: product.qtyPerSet
        }))
    })
}
```

前端可以实时预览：

```text
每套件数 = Σ qtyPerSet
每套重量 = Σ(weight × qtyPerSet)
SET总件数 = totalSets × 每套件数
SET金额 = totalSets × unitPrice
```

SET父行没有商品代号。各商品代号只显示在套装组成区域，整套价格只显示在父行。

## 6. 保存请求

提交前从表单对象中剔除所有仅供展示的计算字段：

- 主表`totalPcs`
- 主表`totalSets`
- 主表`totalAmount`
- 明细`amount`
- SET父行`weight`
- SET父行`totalPcs`

建议价格按字符串提交，避免JavaScript浮点误差：

```json
{
  "unitPrice": "13.8700"
}
```

前端预览金额可以使用`decimal.js`等十进制库，不能直接依赖JavaScript浮点数作为最终金额。后端保存时会重新计算全部结果。

保存成功后：

1. 关闭新增弹窗。
2. 清空表单。
3. 列表回到第一页并重新查询。
4. 使用成功响应中的订单ID按需打开详情。

## 7. 详情和取消

点击订单行时调用详情接口。详情中：

- SINGLE行直接显示商品代号、单件重量、总件数、单价和金额。
- SET行显示每套重量、总套数、拆分后的总件数、整套价格和金额。
- SET行展开后显示`setItems`。

取消订单需要二次确认，并要求填写取消原因。成功后刷新列表和底部人民币/美元统计。

已经取消的订单：

- 显示`CANCELLED`状态标签。
- 禁用或隐藏取消按钮。
- 不参与底部有效金额统计。

## 8. 权限控制

页面路由权限：

```text
signed-order:page
```

按钮和接口权限：

```text
signed-order:list
signed-order:create
signed-order:detail
signed-order:cancel
signed-order:customer-options
signed-order:salesman-options
```

前端只负责控制入口和按钮显示，后端`@PreAuthorize`仍会执行最终鉴权。

执行权限SQL后必须重新登录，因为登录响应和JWT授权缓存中的权限快照需要刷新。

<template>
  <div class="app-container" v-loading="loading">
    <el-page-header :content="pageTitle" @back="handleBack" class="page-header" />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-header">
            <span>基础信息</span>
            <el-tag v-if="form.id" :type="statusMeta.type" :effect="statusMeta.plain ? 'plain' : 'dark'">
              {{ statusMeta.label }}
            </el-tag>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商机名称" prop="name">
              <el-input v-model="form.name" maxlength="100" placeholder="请输入商机名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="ownerUser">
              <el-input v-model="form.ownerUser" maxlength="64" placeholder="请输入负责人账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户" prop="customerId">
              <el-select
                v-model="form.customerId"
                placeholder="请选择客户"
                filterable
                clearable
                :loading="customerLoading"
                style="width: 100%"
                @change="handleCustomerChange"
              >
                <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <div class="field-tip">渠道由下方意向明细逐行选择，可选范围限定为该客户已关联的渠道</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="意向合同金额" prop="contactAmount">
              <el-input-number
                v-model="form.contactAmount"
                :min="0"
                :max="999999999.99"
                :precision="2"
                :controls="false"
                placeholder="请输入意向合同金额"
                style="width: 100%"
              />
              <div class="field-tip">按意向报价人工填写，转合同时带入合同主表</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授信金额" prop="creditAmount">
              <el-input-number
                v-model="form.creditAmount"
                :min="0"
                :max="999999999.99"
                :precision="2"
                :controls="false"
                placeholder="请输入授信金额"
                style="width: 100%"
              />
              <div class="field-tip">意向授信额度，转合同时带入，用于后续额度管控</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-header">
            <span>意向模型产品</span>
            <div class="card-header__right">
              <span class="product-count">共 {{ products.length }} 项</span>
              <el-button
                type="primary"
                plain
                icon="Plus"
                size="small"
                :disabled="!canEditProduct"
                @click="openModelDialog"
              >添加渠道模型</el-button>
            </div>
          </div>
        </template>

        <el-alert
          v-if="!form.customerId"
          type="info"
          :closable="false"
          show-icon
          title="请先选择客户，可选模型仅来源于该客户已关联渠道下的模型产品"
          class="mb8"
        />
        <el-alert
          v-else-if="productsLocked"
          type="warning"
          :closable="false"
          show-icon
          title="商机已提交测试，意向产品清单已锁定，不可增删改"
          class="mb8"
        />

        <el-table :data="products" border size="small" empty-text="暂无意向模型产品，请点击「添加渠道模型」">
          <el-table-column label="渠道" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.supplierName || "-" }}</template>
          </el-table-column>
          <el-table-column label="渠道账户" min-width="180">
            <template #default="scope">
              <el-select
                :model-value="scope.row.supplierAccountId"
                placeholder="请选择渠道账户"
                filterable
                size="small"
                :disabled="!canEditProduct"
                style="width: 100%"
                @change="value => handleRowAccountChange(scope.row, value)"
              >
                <el-option
                  v-for="item in rowAccountOptions(scope.row)"
                  :key="item.id"
                  :label="item.label"
                  :value="item.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="模型型号" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.productNo || scope.row.productName || scope.row.productId }}</template>
          </el-table-column>
          <el-table-column label="模型厂商" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.productName || "-" }}</template>
          </el-table-column>
          <el-table-column label="成本折扣" width="140" align="center">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.costDiscount"
                :min="0.01"
                :max="1"
                :step="0.01"
                :precision="2"
                :controls="false"
                :disabled="!canEditProduct"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="售卖折扣" width="140" align="center">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.sellDiscount"
                :min="0.01"
                :max="1"
                :step="0.01"
                :precision="2"
                :controls="false"
                :disabled="!canEditProduct"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="价格明细" min-width="180" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.priceDetail">{{ scope.row.priceDetail }}</span>
              <span v-else class="text-placeholder">未填写</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="scope">
              <el-button link type="primary" :disabled="!canEditProduct" @click="openPriceDialog(scope.row)">价格明细</el-button>
              <el-button
                link
                type="danger"
                :disabled="!canEditProduct"
                @click="handleRemoveProduct(scope.$index)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-form>

    <div class="form-footer">
      <el-button type="primary" :loading="submitting" @click="submitForm">保 存</el-button>
      <el-button @click="handleBack">返 回</el-button>
    </div>

    <!-- 添加渠道模型 -->
    <el-dialog title="添加渠道模型" v-model="modelDialogOpen" width="1020px" append-to-body>
      <el-input v-model="modelKeyword" placeholder="输入型号或厂商筛选" clearable class="mb8" style="width: 260px" />
      <div class="field-tip mb8">渠道账户为必填项，默认带出该渠道的启用账户，可逐行改选；渠道下无可用账户时该模型不可勾选</div>
      <el-table
        ref="modelTableRef"
        :data="filteredModels"
        border
        size="small"
        height="360"
        row-key="key"
        empty-text="该客户暂无可添加的渠道模型，请先在客户资料中关联渠道"
        @selection-change="handleModelSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" reserve-selection :selectable="isModelSelectable" />
        <el-table-column label="渠道" min-width="140" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.supplierName || "-" }}</template>
        </el-table-column>
        <el-table-column label="渠道账户" min-width="200">
          <template #default="scope">
            <el-select
              :model-value="modelAccountMap[scope.row.key]"
              placeholder="请选择渠道账户"
              filterable
              size="small"
              style="width: 100%"
              @change="value => handleDialogAccountChange(scope.row, value)"
            >
              <el-option
                v-for="item in dialogAccountOptions(scope.row)"
                :key="item.id"
                :label="item.label"
                :value="item.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="模型型号" min-width="140" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.no || scope.row.name }}</template>
        </el-table-column>
        <el-table-column label="模型厂商" min-width="140" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.name || "-" }}</template>
        </el-table-column>
        <el-table-column label="成本折扣区间" width="140" align="center">
          <template #default="scope">{{ formatRange(scope.row.minCostDiscount, scope.row.maxCostDiscount) }}</template>
        </el-table-column>
        <el-table-column label="售卖折扣区间" width="140" align="center">
          <template #default="scope">{{ formatRange(scope.row.minSellDiscount, scope.row.maxSellDiscount) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="confirmAddModels">添 加（{{ selectedModels.length }}）</el-button>
          <el-button @click="modelDialogOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 价格明细 -->
    <el-dialog title="价格明细" v-model="priceDialogOpen" width="560px" append-to-body>
      <div class="field-tip mb8">支持 JSON 格式，用于承载计价单位、基准售价、配额等扩展字段，最大 2000 字符</div>
      <el-input
        v-model="priceForm.priceDetail"
        type="textarea"
        :rows="8"
        maxlength="2000"
        show-word-limit
        placeholder='例如：{"unit":"百万token","basePrice":12,"quota":1000}'
      />
      <div v-if="priceError" class="price-error">{{ priceError }}</div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formatPriceDetail">格式化</el-button>
          <el-button type="primary" @click="confirmPriceDetail">确 定</el-button>
          <el-button @click="priceDialogOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BusinessEdit">
import { getBusiness, saveBusiness } from "@/api/crm/business"
import { getCustomerOptions, getCustomerProductOptions, getCustomerSupplierAccounts } from "@/api/crm/customer"

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const STATUS = { INIT: 0, FOLLOWING: 1, SUBMIT_TEST: 2, TESTING: 3, DEAL: 4, PAUSED: 5, INTERRUPTED: 6 }
const STATUS_META = {
  [STATUS.INIT]: { label: "初始化", type: "info", plain: true },
  [STATUS.FOLLOWING]: { label: "跟进中", type: "primary", plain: false },
  [STATUS.SUBMIT_TEST]: { label: "提交测试", type: "primary", plain: true },
  [STATUS.TESTING]: { label: "测试中", type: "warning", plain: false },
  [STATUS.DEAL]: { label: "成交", type: "success", plain: false },
  [STATUS.PAUSED]: { label: "暂停", type: "warning", plain: true },
  [STATUS.INTERRUPTED]: { label: "中断", type: "danger", plain: false }
}

/** 列表页路由：由当前编辑页路由推导 */
const listPath = computed(() => {
  const base = route.path.replace(/\/+$/, "")
  return /\/edit$/.test(base) ? base.replace(/\/edit$/, "") : "/crm/business"
})

const loading = ref(false)
const submitting = ref(false)
const customerOptions = ref([])
const customerLoading = ref(false)

/** 客户已关联渠道下的可用模型（渠道-模型组合） */
const models = ref([])
/** 客户可用渠道下的账户（渠道-账户组合） */
const supplierAccounts = ref([])
/** 弹窗内每行选择的渠道账户：key 为「渠道:模型」 */
const modelAccountMap = reactive({})
const modelDialogOpen = ref(false)
const modelKeyword = ref("")
const modelTableRef = ref()
const selectedModels = ref([])

/** 意向产品清单 */
const products = ref([])

const formRef = ref()
const form = reactive({
  id: null,
  name: null,
  customerId: null,
  ownerUser: null,
  contactAmount: null,
  creditAmount: null,
  remark: null,
  busStatus: null
})

const rules = {
  name: [{ required: true, message: "商机名称不能为空", trigger: "blur" }],
  customerId: [{ required: true, message: "客户不能为空", trigger: ["blur", "change"] }],
  ownerUser: [{ required: true, message: "负责人不能为空", trigger: "blur" }]
}

const pageTitle = computed(() => (form.id ? "编辑商机" : "新建商机"))

const statusMeta = computed(() => STATUS_META[Number(form.busStatus)] || STATUS_META[STATUS.INIT])

/** 仅初始化、跟进中可维护产品（与后端一致） */
const canEditProduct = computed(() => {
  if (!form.id) {
    return true
  }
  const status = Number(form.busStatus)
  return status === STATUS.INIT || status === STATUS.FOLLOWING
})

const productsLocked = computed(() => !!form.id && !canEditProduct.value)

/** 明细唯一键：同一模型可挂在多个渠道下，需以「渠道 + 模型」定位 */
function rowKey(supplierId, productId) {
  return `${supplierId}:${productId}`
}

/** 可选模型：过滤已添加的渠道-模型组合与关键字 */
const filteredModels = computed(() => {
  const keyword = modelKeyword.value.trim().toLowerCase()
  const exists = new Set(products.value.map(item => rowKey(item.supplierId, item.productId)))
  return models.value
    .map(item => ({ ...item, key: rowKey(item.supplierId, item.id) }))
    .filter(item => {
      if (exists.has(item.key)) {
        return false
      }
      if (!keyword) {
        return true
      }
      return `${item.no || ""}${item.name || ""}`.toLowerCase().includes(keyword)
    })
})

function isEmptyVal(val) {
  return val === null || val === undefined || val === ""
}

function formatRange(min, max) {
  const minText = isEmptyVal(min) ? null : Number(min).toFixed(2)
  const maxText = isEmptyVal(max) ? null : Number(max).toFixed(2)
  if (minText === null && maxText === null) {
    return "-"
  }
  if (minText === null) {
    return `≤ ${maxText}`
  }
  if (maxText === null) {
    return `≥ ${minText}`
  }
  return `${minText} ~ ${maxText}`
}

function loadCustomerOptions() {
  customerLoading.value = true
  getCustomerOptions().then(res => {
    customerOptions.value = res.data || []
  }).finally(() => {
    customerLoading.value = false
  })
}

/** 加载客户已关联渠道下的可用模型与渠道账户（与合同明细同一数据源） */
function loadCustomerData(customerId) {
  if (isEmptyVal(customerId)) {
    models.value = []
    supplierAccounts.value = []
    return Promise.resolve()
  }
  return Promise.all([
    getCustomerProductOptions(customerId).then(res => {
      models.value = res.data || []
    }).catch(() => {
      models.value = []
    }),
    getCustomerSupplierAccounts(customerId).then(res => {
      supplierAccounts.value = res.data || []
    }).catch(() => {
      supplierAccounts.value = []
    })
  ])
}

/** 指定渠道下的账户，启用账户排在前面 */
function accountsOfSupplier(supplierId) {
  return supplierAccounts.value.filter(item => String(item.supplierId) === String(supplierId))
}

/** 渠道默认账户：优先启用账户，其次取 id 最小者 */
function defaultAccountOf(supplierId) {
  const list = accountsOfSupplier(supplierId)
  if (!list.length) {
    return null
  }
  return list.find(item => Number(item.status) === 1) || list[0]
}

/** 账户展示名：账户名（账户类型） */
function accountLabel(item) {
  if (!item) {
    return ""
  }
  const typeText = Number(item.type) === 1 ? "测试" : Number(item.type) === 2 ? "真实" : ""
  return typeText ? `${item.accountName}（${typeText}）` : item.accountName
}

/** 明细行的账户下拉：账户已失效（不在当前客户渠道下）时补一条兜底选项 */
function rowAccountOptions(row) {
  const list = accountsOfSupplier(row.supplierId).map(item => ({ id: item.id, label: accountLabel(item) }))
  if (row.supplierAccountId && !list.some(item => String(item.id) === String(row.supplierAccountId))) {
    list.unshift({
      id: row.supplierAccountId,
      label: `${row.supplierAccountName || `账户 ${row.supplierAccountId}`}（不可用）`
    })
  }
  return list
}

/** 弹窗内账户下拉（与明细行同源） */
function dialogAccountOptions(model) {
  return accountsOfSupplier(model.supplierId).map(item => ({ id: item.id, label: accountLabel(item) }))
}

/** 明细行切换渠道账户：同步账户名，便于未刷新时展示 */
function handleRowAccountChange(row, accountId) {
  const hit = supplierAccounts.value.find(item => String(item.id) === String(accountId))
  row.supplierAccountId = accountId || null
  row.supplierAccountName = hit ? hit.accountName : ""
}

/** 弹窗内切换渠道账户 */
function handleDialogAccountChange(model, accountId) {
  modelAccountMap[model.key] = accountId
}

/** 渠道下没有账户时不可勾选（没有账户无法提交，避免无效选择） */
function isModelSelectable(model) {
  return accountsOfSupplier(model.supplierId).length > 0
}

/** 客户变更：原明细的渠道已不属于新客户，需清空后重新选择 */
function handleCustomerChange(value) {
  if (products.value.length) {
    products.value = []
    proxy.$modal.msgWarning("已切换客户，原意向明细已清空，请重新添加渠道模型")
  }
  models.value = []
  supplierAccounts.value = []
  loadCustomerData(value)
}

function openModelDialog() {
  if (!form.customerId) {
    proxy.$modal.msgWarning("请先选择客户")
    return
  }
  modelKeyword.value = ""
  selectedModels.value = []
  modelDialogOpen.value = true
  loadCustomerData(form.customerId).then(() => {
    // 默认带出各渠道的可用账户，用户仍可逐行改选
    Object.keys(modelAccountMap).forEach(key => delete modelAccountMap[key])
    filteredModels.value.forEach(item => {
      const account = defaultAccountOf(item.supplierId)
      modelAccountMap[item.key] = account ? account.id : undefined
    })
  })
}

function handleModelSelectionChange(selection) {
  selectedModels.value = selection
}

/** 确认添加：渠道账户必填，折扣默认取渠道配置下限，未配置时取 1.00 */
function confirmAddModels() {
  if (!selectedModels.value.length) {
    proxy.$modal.msgWarning("请选择要添加的模型")
    return
  }
  const missing = selectedModels.value.filter(model => isEmptyVal(modelAccountMap[model.key]))
  if (missing.length) {
    proxy.$modal.msgError(`请为「${missing[0].supplierName || missing[0].name}」选择渠道账户`)
    return
  }
  selectedModels.value.forEach(model => {
    const account = supplierAccounts.value.find(item => String(item.id) === String(modelAccountMap[model.key]))
    products.value.push({
      supplierId: model.supplierId,
      supplierName: model.supplierName,
      supplierAccountId: modelAccountMap[model.key],
      supplierAccountName: account ? account.accountName : "",
      productId: model.id,
      productNo: model.no,
      productName: model.name,
      costDiscount: isEmptyVal(model.minCostDiscount) ? 1 : Number(model.minCostDiscount),
      sellDiscount: isEmptyVal(model.minSellDiscount) ? 1 : Number(model.minSellDiscount),
      priceDetail: null
    })
  })
  modelDialogOpen.value = false
  proxy.$modal.msgSuccess(`已添加 ${selectedModels.value.length} 个模型`)
}

function handleRemoveProduct(index) {
  products.value.splice(index, 1)
}

/** 价格明细编辑 */
const priceDialogOpen = ref(false)
const priceForm = reactive({ priceDetail: "" })
const priceError = ref("")
let priceEditingRow = null

function openPriceDialog(row) {
  priceEditingRow = row
  priceForm.priceDetail = row.priceDetail || ""
  priceError.value = ""
  priceDialogOpen.value = true
}

function formatPriceDetail() {
  const text = (priceForm.priceDetail || "").trim()
  if (!text) {
    priceError.value = ""
    return
  }
  try {
    priceForm.priceDetail = JSON.stringify(JSON.parse(text), null, 2)
    priceError.value = ""
  } catch (e) {
    priceError.value = "JSON 格式不正确，请检查后再格式化"
  }
}

function confirmPriceDetail() {
  const text = (priceForm.priceDetail || "").trim()
  if (text) {
    try {
      JSON.parse(text)
    } catch (e) {
      priceError.value = "价格明细必须是合法的 JSON"
      return
    }
  }
  if (priceEditingRow) {
    priceEditingRow.priceDetail = text || null
  }
  priceDialogOpen.value = false
}

/** 明细校验：折扣区间 0.01~1.00、同渠道同模型不可重复 */
function validateProducts() {
  const seen = new Set()
  for (let i = 0; i < products.value.length; i++) {
    const row = products.value[i]
    const rowNo = i + 1
    if (isEmptyVal(row.supplierId)) {
      proxy.$modal.msgError(`第 ${rowNo} 行未选择渠道`)
      return false
    }
    if (isEmptyVal(row.supplierAccountId)) {
      proxy.$modal.msgError(`第 ${rowNo} 行未选择渠道账户`)
      return false
    }
    if (isEmptyVal(row.productId)) {
      proxy.$modal.msgError(`第 ${rowNo} 行未选择模型`)
      return false
    }
    const key = rowKey(row.supplierId, row.productId)
    if (seen.has(key)) {
      proxy.$modal.msgError(`第 ${rowNo} 行模型重复，同一渠道下不可重复添加同一模型`)
      return false
    }
    seen.add(key)
    if (!isValidDiscount(row.costDiscount)) {
      proxy.$modal.msgError(`第 ${rowNo} 行成本折扣取值范围为 0.01 ~ 1.00`)
      return false
    }
    if (!isValidDiscount(row.sellDiscount)) {
      proxy.$modal.msgError(`第 ${rowNo} 行售卖折扣取值范围为 0.01 ~ 1.00`)
      return false
    }
  }
  return true
}

function isValidDiscount(value) {
  if (isEmptyVal(value)) {
    return false
  }
  const num = Number(value)
  return !isNaN(num) && num >= 0.01 && num <= 1
}

/** 组装提交数据：主表不再携带渠道，渠道随明细行提交 */
function buildPayload() {
  return {
    id: form.id,
    name: form.name,
    customerId: form.customerId,
    ownerUser: form.ownerUser,
    contactAmount: isEmptyVal(form.contactAmount) ? null : form.contactAmount,
    creditAmount: isEmptyVal(form.creditAmount) ? null : form.creditAmount,
    remark: form.remark,
    products: products.value.map(row => ({
      supplierId: row.supplierId,
      supplierAccountId: row.supplierAccountId,
      productId: row.productId,
      costDiscount: row.costDiscount,
      sellDiscount: row.sellDiscount,
      priceDetail: row.priceDetail || null
    }))
  }
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) {
      return
    }
    if (!validateProducts()) {
      return
    }
    submitting.value = true
    saveBusiness(buildPayload()).then(() => {
      proxy.$modal.msgSuccess(form.id ? "修改成功" : "新增成功")
      handleBack()
    }).finally(() => {
      submitting.value = false
    })
  })
}

function handleBack() {
  router.push({ path: listPath.value })
}

/** 加载详情：仅初始化/跟进中允许编辑，其余状态跳转详情页 */
function loadDetail(id) {
  loading.value = true
  getBusiness(id).then(res => {
    const detail = res.data || {}
    if (detail.mainEditable === false) {
      proxy.$modal.msgWarning("当前状态不允许编辑商机，已为您打开详情页")
      router.replace({ path: listPath.value + "/detail", query: { id } })
      return
    }
    form.id = detail.id
    form.name = detail.name
    form.customerId = detail.customerId
    form.ownerUser = detail.ownerUser
    form.contactAmount = detail.contactAmount
    form.creditAmount = detail.creditAmount
    form.remark = detail.remark
    form.busStatus = detail.busStatus
    products.value = (detail.businessProducts || []).map(item => ({
      supplierId: item.supplierId,
      supplierName: item.supplierName,
      supplierAccountId: item.supplierAccountId,
      supplierAccountName: item.supplierAccountName,
      productId: item.productId,
      productNo: item.productNo,
      productName: item.productName,
      costDiscount: item.costDiscount,
      sellDiscount: item.sellDiscount,
      priceDetail: item.priceDetail
    }))
    return loadCustomerData(detail.customerId)
  }).catch(() => {
    handleBack()
  }).finally(() => {
    loading.value = false
  })
}

loadCustomerOptions()

const routeId = route.query.id
if (routeId) {
  loadDetail(routeId)
}
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.section-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.card-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-count {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}
.field-tip {
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}
.text-placeholder {
  color: #c0c4cc;
}
.price-error {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
}
.form-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 24px;
}
</style>

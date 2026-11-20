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
              <div class="field-tip">渠道与模型由下方意向明细逐行选择，保存时校验渠道已启用、模型已在渠道上架、账户归属该渠道</div>
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
          title="请先选择客户，可选模型取自渠道已上架模型，明细保存时强制校验渠道与账户归属"
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

    <!-- 添加渠道模型：先选渠道 → 再选渠道账户 → 再选模型型号 -->
    <el-dialog title="添加渠道模型" v-model="modelDialogOpen" width="620px" append-to-body @closed="resetPick">
      <el-form label-width="90px">
        <el-form-item label="渠道" required>
          <el-select
            v-model="pick.supplierId"
            placeholder="请选择渠道"
            filterable
            style="width: 100%"
            @change="handlePickSupplierChange"
          >
            <el-option
              v-for="item in pickSupplierOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
              :disabled="item.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道账户" required>
          <el-select
            v-model="pick.accountId"
            :disabled="isEmptyVal(pick.supplierId)"
            :placeholder="isEmptyVal(pick.supplierId) ? '请先选择渠道' : '请选择渠道账户'"
            filterable
            style="width: 100%"
          >
            <el-option v-for="item in pickAccountOptions" :key="item.id" :label="accountLabel(item)" :value="item.id" />
          </el-select>
          <div v-if="!isEmptyVal(pick.supplierId) && !pickAccountOptions.length" class="field-tip">
            该渠道暂无可用账户，请先在「渠道账户管理」中维护
          </div>
        </el-form-item>
        <el-form-item label="模型型号" required>
          <el-select
            v-model="pick.productIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            :disabled="isEmptyVal(pick.supplierId)"
            :placeholder="isEmptyVal(pick.supplierId) ? '请先选择渠道' : '请选择模型型号，可多选'"
            style="width: 100%"
          >
            <el-option
              v-for="item in pickProductOptions"
              :key="item.id"
              :label="`${item.no || item.name}（${item.name}）`"
              :value="item.id"
            />
          </el-select>
          <div class="field-tip">
            已添加过的「渠道 + 模型」不再出现在下拉中；成本/售卖折扣默认取该渠道配置下限，添加后可在明细中调整
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer dialog-footer--spread">
          <span class="field-tip">
            {{ pickAddedCount ? `本次已添加 ${pickAddedCount} 项，可切换渠道继续添加` : "按「渠道 → 渠道账户 → 模型型号」顺序选择后添加" }}
          </span>
          <div>
            <el-button type="primary" @click="confirmAddModels">添 加</el-button>
            <el-button @click="modelDialogOpen = false">关 闭</el-button>
          </div>
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
import { getCustomerOptions } from "@/api/crm/customer"
import { getChannelProductOptions, getChannelAccountOptions } from "@/api/crm/supplier"

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

/** 渠道可售模型（渠道-模型组合），可选范围由渠道上架关系决定，与客户无关 */
const models = ref([])
/** 渠道账户（渠道-账户组合），明细行需指定账户 */
const supplierAccounts = ref([])
const modelDialogOpen = ref(false)
/** 弹窗级联选择状态：渠道 → 渠道账户 → 模型型号（可多选） */
const pick = reactive({ supplierId: null, accountId: null, productIds: [] })
/** 本次弹窗会话已添加条数（支持切换渠道连续添加，不关闭弹窗） */
const pickAddedCount = ref(0)

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

/** 弹窗渠道下拉：由可售模型反推渠道；无账户或模型已全部添加的渠道置灰 */
const pickSupplierOptions = computed(() => {
  const addedKeys = new Set(products.value.map(item => rowKey(item.supplierId, item.productId)))
  const group = new Map()
  models.value.forEach(item => {
    const key = String(item.supplierId)
    if (!group.has(key)) {
      group.set(key, {
        id: item.supplierId,
        name: item.supplierName || `渠道 ${item.supplierId}`,
        remaining: 0
      })
    }
    if (!addedKeys.has(rowKey(item.supplierId, item.id))) {
      group.get(key).remaining += 1
    }
  })
  return Array.from(group.values()).map(entry => {
    const noAccount = accountsOfSupplier(entry.id).length === 0
    const noModel = entry.remaining === 0
    let label = entry.name
    if (noAccount) {
      label += "（无可用账户）"
    } else if (noModel) {
      label += "（模型已全部添加）"
    }
    return { ...entry, label, disabled: noAccount || noModel }
  })
})

/** 弹窗账户下拉：限定当前所选渠道 */
const pickAccountOptions = computed(() => {
  if (isEmptyVal(pick.supplierId)) {
    return []
  }
  return accountsOfSupplier(pick.supplierId)
})

/** 弹窗模型下拉：限定当前所选渠道，并剔除已添加的「渠道 + 模型」组合 */
const pickProductOptions = computed(() => {
  if (isEmptyVal(pick.supplierId)) {
    return []
  }
  const addedKeys = new Set(products.value.map(item => rowKey(item.supplierId, item.productId)))
  return models.value.filter(item =>
    String(item.supplierId) === String(pick.supplierId) && !addedKeys.has(rowKey(item.supplierId, item.id))
  )
})

function isEmptyVal(val) {
  return val === null || val === undefined || val === ""
}

function loadCustomerOptions() {
  customerLoading.value = true
  getCustomerOptions().then(res => {
    customerOptions.value = res.data || []
  }).finally(() => {
    customerLoading.value = false
  })
}

/** 加载渠道可售模型与渠道账户（与合同明细同一数据源） */
function loadChannelData() {
  return Promise.all([
    getChannelProductOptions().then(res => {
      models.value = res.data || []
    }).catch(() => {
      models.value = []
    }),
    getChannelAccountOptions().then(res => {
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

/** 明细行切换渠道账户：同步账户名，便于未刷新时展示 */
function handleRowAccountChange(row, accountId) {
  const hit = supplierAccounts.value.find(item => String(item.id) === String(accountId))
  row.supplierAccountId = accountId || null
  row.supplierAccountName = hit ? hit.accountName : ""
}

/** 客户变更：业务主体变化，原意向明细需清空后重新确认 */
function handleCustomerChange() {
  if (products.value.length) {
    products.value = []
    proxy.$modal.msgWarning("已切换客户，原意向明细已清空，请重新添加渠道模型")
  }
}

/** 重置弹窗级联选择状态 */
function resetPick() {
  pick.supplierId = null
  pick.accountId = null
  pick.productIds = []
  pickAddedCount.value = 0
}

/** 切换渠道：账户回到该渠道默认账户，已选模型清空（模型范围随渠道变化） */
function handlePickSupplierChange() {
  pick.productIds = []
  const account = defaultAccountOf(pick.supplierId)
  pick.accountId = account ? account.id : null
}

function openModelDialog() {
  if (!form.customerId) {
    proxy.$modal.msgWarning("请先选择客户")
    return
  }
  resetPick()
  modelDialogOpen.value = true
  loadChannelData().then(() => {
    // 默认定位到第一个可选渠道（有账户且仍有未添加模型），减少一次点击
    const first = pickSupplierOptions.value.find(item => !item.disabled)
    if (first) {
      pick.supplierId = first.id
      handlePickSupplierChange()
    }
  })
}

/** 确认添加：渠道、账户、模型均必填；折扣默认取渠道配置下限，未配置时取 1.00 */
function confirmAddModels() {
  if (isEmptyVal(pick.supplierId)) {
    proxy.$modal.msgWarning("请选择渠道")
    return
  }
  if (isEmptyVal(pick.accountId)) {
    proxy.$modal.msgError("请选择渠道账户")
    return
  }
  if (!pick.productIds.length) {
    proxy.$modal.msgWarning("请至少选择一个模型型号")
    return
  }
  const account = supplierAccounts.value.find(item => String(item.id) === String(pick.accountId))
  const added = []
  pick.productIds.forEach(productId => {
    const model = models.value.find(item =>
      String(item.supplierId) === String(pick.supplierId) && String(item.id) === String(productId)
    )
    if (!model) {
      return
    }
    const key = rowKey(model.supplierId, model.id)
    if (products.value.some(row => rowKey(row.supplierId, row.productId) === key)) {
      return
    }
    products.value.push({
      supplierId: model.supplierId,
      supplierName: model.supplierName,
      supplierAccountId: account ? account.id : pick.accountId,
      supplierAccountName: account ? account.accountName : "",
      productId: model.id,
      productNo: model.no,
      productName: model.name,
      costDiscount: isEmptyVal(model.minCostDiscount) ? 1 : Number(model.minCostDiscount),
      sellDiscount: isEmptyVal(model.minSellDiscount) ? 1 : Number(model.minSellDiscount),
      priceDetail: null
    })
    added.push(model)
  })
  if (!added.length) {
    proxy.$modal.msgWarning("所选模型均已添加，请重新选择")
    return
  }
  pickAddedCount.value += added.length
  pick.productIds = []
  // 当前渠道的模型已全部添加时自动跳到下一个可选渠道，避免停留在空下拉
  if (!pickProductOptions.value.length) {
    const next = pickSupplierOptions.value.find(item => !item.disabled)
    if (next) {
      pick.supplierId = next.id
      handlePickSupplierChange()
    }
  }
  proxy.$modal.msgSuccess(`已添加 ${added.length} 个模型`)
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
    return loadChannelData()
  }).catch(() => {
    handleBack()
  }).finally(() => {
    loading.value = false
  })
}

loadCustomerOptions()
loadChannelData()

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
.dialog-footer--spread {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.dialog-footer--spread .field-tip {
  text-align: left;
}
</style>

<template>
  <div class="app-container" v-loading="loading">
    <el-page-header
      @back="handleBack"
      :content="isCreate ? '新增合同' : (productsOnly ? '编辑模型明细（已签约）' : '编辑合同（草稿）')"
      class="page-header"
    />

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-title">
          <span>基础信息</span>
          <el-tag v-if="!isCreate" size="small" type="info">合同编号：{{ form.contactNo || "保存后自动生成" }}</el-tag>
        </div>
      </template>
      <el-alert
        v-if="productsOnly"
        type="warning"
        :closable="false"
        show-icon
        class="mode-alert"
        title="已签约合同变更明细将立即生效"
        description="基础信息不可修改；新的渠道模型与折扣保存后即刻用于后续对账计费，历史台账按原折扣快照结算，不受影响。"
      />
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px" :disabled="productsOnly">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商机" prop="busId">
              <el-select
                v-model="form.busId"
                placeholder="请选择商机"
                filterable
                remote
                clearable
                :disabled="!isCreate || busLocked"
                :remote-method="searchBusiness"
                :loading="busLoading"
                style="width: 100%"
                @change="handleBusChange"
              >
                <el-option v-for="item in busOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户">
              <el-input v-model="form.consumerName" placeholder="由商机带出" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同编号">
              <el-input v-model="form.contactNo" placeholder="首次保存草稿后自动生成，不可修改" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同名称" prop="contractName">
              <el-input v-model="form.contractName" placeholder="请输入合同名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签约主体" prop="contractEntity">
              <el-input v-model="form.contractEntity" placeholder="请输入签约主体" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="到期时间" prop="expireTime">
              <el-date-picker
                v-model="form.expireTime"
                type="datetime"
                placeholder="请选择合同到期时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-title">
          <span>模型产品明细</span>
          <div class="section-actions">
            <span class="hint">
              {{ productsOnly
                ? "已签约合同可增删模型、调整折扣与价格明细，保存后立即生效并记入操作日志"
                : "仅可选择渠道已上架模型，保存时校验渠道、账户与模型归属一致；同一渠道下同一模型不可重复添加" }}
            </span>
            <el-button type="primary" plain icon="Plus" :disabled="!form.consumerId" @click="openAddProduct">
              添加渠道模型
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-if="rows.length" :data="rows" border size="small">
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
        <el-table-column label="模型名称" min-width="220">
          <template #default="scope">
            <span class="model-name">{{ modelLabel(scope.row.productId) }}</span>
            <div v-if="scope.row.productNo" class="model-no">{{ scope.row.productNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="成本折扣" width="160" align="center">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.costDiscount"
              :min="0.01"
              :max="1"
              :step="0.01"
              :precision="2"
              :controls="false"
              size="small"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="售卖折扣" width="160" align="center">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.sellDiscount"
              :min="0.01"
              :max="1"
              :step="0.01"
              :precision="2"
              :controls="false"
              size="small"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="价格明细（JSON）" min-width="240">
          <template #default="scope">
            <div class="price-cell">
              <span class="price-text">{{ scope.row.priceDetail || "未配置" }}</span>
              <el-button link type="primary" @click="openPriceDetail(scope.$index)">编辑</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="danger" icon="Delete" @click="removeRow(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无模型明细，点击「添加渠道模型」开始添加" :image-size="70" />

      <div v-if="duplicateNames.length" class="duplicate-warning">
        存在重复模型：{{ duplicateNames.join("、") }}，请调整后再保存
      </div>
    </el-card>

    <div class="footer-bar">
      <el-button @click="handleBack">返 回</el-button>
      <template v-if="productsOnly">
        <el-button type="primary" :loading="saving" @click="handleSave(false)">保存明细</el-button>
      </template>
      <template v-else>
        <el-button type="primary" :loading="saving" @click="handleSave(false)">保存草稿</el-button>
        <el-button type="success" :loading="saving" @click="handleSave(true)">保存并提交审批</el-button>
      </template>
    </div>

    <!-- 添加渠道模型 -->
    <el-dialog title="添加渠道模型" v-model="addOpen" width="560px" append-to-body>
      <el-form :model="addForm" :rules="addRules" ref="addRef" label-width="100px">
        <el-form-item label="渠道" prop="supplierId">
          <el-select
            v-model="addForm.supplierId"
            placeholder="请选择渠道"
            filterable
            style="width: 100%"
            @change="handleAddSupplierChange"
          >
            <el-option
              v-for="item in channelOptions"
              :key="item.supplierId"
              :label="item.supplierName || item.supplierId"
              :value="item.supplierId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道账户" prop="supplierAccountId">
          <el-select
            v-model="addForm.supplierAccountId"
            :placeholder="addForm.supplierId ? '请选择该渠道下的账户' : '请先选择渠道'"
            filterable
            :disabled="!addForm.supplierId"
            style="width: 100%"
          >
            <el-option
              v-for="item in addAccountOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模型" prop="productId">
          <el-select
            v-model="addForm.productId"
            :placeholder="addForm.supplierId ? '请选择该渠道下的模型' : '请先选择渠道'"
            filterable
            :disabled="!addForm.supplierId"
            style="width: 100%"
          >
            <el-option
              v-for="item in availableProducts"
              :key="item.key"
              :label="`${item.name || ''} ${item.no || ''}`.trim()"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成本折扣" prop="costDiscount">
          <el-input-number
            v-model="addForm.costDiscount"
            :min="0.01"
            :max="1"
            :step="0.01"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="售卖折扣" prop="sellDiscount">
          <el-input-number
            v-model="addForm.sellDiscount"
            :min="0.01"
            :max="1"
            :step="0.01"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="价格明细">
          <el-input
            v-model="addForm.priceDetail"
            type="textarea"
            :rows="3"
            placeholder='可选，JSON 格式，如 {"unit":"token","baseSellPrice":0.002,"limit":1000000}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="confirmAddProduct">确 定</el-button>
          <el-button @click="addOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 价格明细编辑 -->
    <el-dialog title="编辑价格明细" v-model="priceOpen" width="600px" append-to-body>
      <el-input
        v-model="priceForm.priceDetail"
        type="textarea"
        :rows="8"
        placeholder='JSON 格式，如 {"unit":"token","baseSellPrice":0.002,"limit":1000000}'
      />
      <div class="price-tips">价格明细用于渠道与客户结算、毛利核算，支持后续扩展包量、阶梯价、配额等字段。</div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formatPriceJson">格式化</el-button>
          <el-button type="primary" @click="confirmPriceDetail">确 定</el-button>
          <el-button @click="priceOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ContractEdit">
import { getContract, initContract, saveContract, submitAudit, batchSaveContractProduct } from "@/api/crm/contract"
import { listBusiness } from "@/api/crm/business"
import { getChannelProductOptions, getChannelAccountOptions } from "@/api/crm/supplier"

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

/** 合同列表路由前缀：/xxx/contract/edit → /xxx/contract */
const contractBasePath = computed(() => route.path.replace(/\/(edit|detail)\/?$/, "") || "/crm/contract")

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const addRef = ref()

const isCreate = ref(true)
/** 已签约合同的「仅明细编辑」模式：基础信息只读，保存仅提交明细 */
const productsOnly = ref(false)
const busLocked = ref(false)
const busOptions = ref([])
const busLoading = ref(false)
/** 渠道可售模型（渠道-模型组合），可选范围由渠道上架关系决定，与客户无关 */
const channelProducts = ref([])
/** 渠道账户（渠道-账户组合），明细行需指定账户 */
const supplierAccounts = ref([])

const form = ref({
  id: undefined,
  busId: undefined,
  consumerId: undefined,
  consumerName: "",
  contactNo: "",
  contractName: "",
  contractEntity: "",
  expireTime: undefined,
  remark: "",
  status: 0
})

/** 模型明细行 */
const rows = ref([])

const STATUS_LABELS = {
  0: "草稿",
  1: "审批中",
  2: "已签约",
  3: "已到期",
  4: "已作废",
  5: "已归档"
}

const STATUS_TAG_TYPES = {
  0: "info",
  1: "primary",
  2: "success",
  3: "warning",
  4: "danger",
  5: "info"
}

const statusLabel = computed(() => STATUS_LABELS[Number(form.value.status)] || "草稿")
const statusTagType = computed(() => STATUS_TAG_TYPES[Number(form.value.status)] || "info")

const rules = {
  busId: [{ required: true, message: "请选择商机", trigger: "change" }],
  contractName: [{ required: true, message: "合同名称不能为空", trigger: "blur" }],
  contractEntity: [{ required: true, message: "签约主体不能为空", trigger: "blur" }],
  expireTime: [
    { required: true, message: "请选择合同到期时间", trigger: "change" },
    {
      validator: (rule, value, callback) => {
        if (value && new Date(String(value).replace(/-/g, "/")) <= new Date()) {
          callback(new Error("合同到期时间必须大于当前时间"))
          return
        }
        callback()
      },
      trigger: "change"
    }
  ]
}

/** 商机远程搜索：仅返回已成交且未生成合同的商机（与后端校验一致） */
function searchBusiness(keyword) {
  busLoading.value = true
  listBusiness({ pageNum: 1, pageSize: 20, name: keyword || undefined, busStatus: 4 })
    .then(res => {
      busOptions.value = (res.rows || [])
        .filter(item => Number(item.hasContract) !== 1)
        .map(item => ({ id: item.id, name: item.name }))
    })
    .finally(() => {
      busLoading.value = false
    })
}

/** 选择商机后带出客户，渠道由下方明细逐行选择 */
function handleBusChange(busId) {
  if (!busId) {
    form.value.consumerId = undefined
    form.value.consumerName = ""
    channelProducts.value = []
    supplierAccounts.value = []
    busLocked.value = false
    return
  }
  initContract(busId).then(res => {
    const data = res.data || {}
    form.value.consumerId = data.consumerId
    form.value.consumerName = data.consumerName || ""
    if (!form.value.contractName && data.suggestedContractName) {
      form.value.contractName = data.suggestedContractName
    }
    busLocked.value = true
    loadChannelData()
  })
}

/** 明细唯一键：同一模型可挂在多个渠道下，需以「渠道 + 模型」定位 */
function rowKey(supplierId, productId) {
  return `${supplierId}:${productId}`
}

/** 加载渠道可售模型与渠道账户（与商机明细同一数据源） */
function loadChannelData() {
  Promise.all([
    getChannelProductOptions().then(res => {
      channelProducts.value = res.data || []
    }).catch(() => {
      channelProducts.value = []
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

/** 添加弹窗可选账户：随已选渠道联动 */
const addAccountOptions = computed(() =>
  accountsOfSupplier(addForm.value.supplierId).map(item => ({ id: item.id, label: accountLabel(item) }))
)

/** 客户可选的渠道（由渠道-模型组合去重得到） */
const channelOptions = computed(() => {
  const map = new Map()
  channelProducts.value.forEach(item => {
    if (item.supplierId != null && !map.has(item.supplierId)) {
      map.set(item.supplierId, { supplierId: item.supplierId, supplierName: item.supplierName })
    }
  })
  return Array.from(map.values())
})

/** 模型展示名：按「渠道 + 模型」定位 */
function modelLabel(productId, supplierId) {
  const hit = channelProducts.value.find(
    item => String(item.id) === String(productId) && String(item.supplierId) === String(supplierId)
  )
  if (hit) {
    return `${hit.name || ""} ${hit.no || ""}`.trim()
  }
  const row = rows.value.find(
    item => String(item.productId) === String(productId) && String(item.supplierId) === String(supplierId)
  )
  return row && row.productName ? `${row.productName} ${row.productNo || ""}`.trim() : `模型 ${productId}`
}

/** 已选「渠道-模型」组合集合 */
const selectedProductKeys = computed(() =>
  rows.value.map(row => rowKey(row.supplierId, row.productId))
)

/** 可选模型：按弹窗已选渠道过滤，并排除已添加 */
const availableProducts = computed(() =>
  channelProducts.value
    .filter(item => String(item.supplierId) === String(addForm.value.supplierId))
    .filter(item => !selectedProductKeys.value.includes(rowKey(item.supplierId, item.id)))
    .map(item => ({ ...item, key: rowKey(item.supplierId, item.id) }))
)

/** 重复「渠道-模型」名称（前端提前提示，后端同样强校验） */
const duplicateNames = computed(() => {
  const seen = new Set()
  const duplicated = []
  rows.value.forEach(row => {
    if (!row.productId || !row.supplierId) {
      return
    }
    const key = rowKey(row.supplierId, row.productId)
    if (seen.has(key)) {
      const name = `${row.supplierName || ""} / ${modelLabel(row.productId, row.supplierId)}`.trim()
      if (!duplicated.includes(name)) {
        duplicated.push(name)
      }
    } else {
      seen.add(key)
    }
  })
  return duplicated
})

/** 添加模型弹窗 */
const addOpen = ref(false)
const addForm = ref({
  supplierId: undefined,
  supplierAccountId: undefined,
  productId: undefined,
  costDiscount: 1,
  sellDiscount: 1,
  priceDetail: ""
})

const addRules = {
  supplierId: [{ required: true, message: "请选择渠道", trigger: "change" }],
  supplierAccountId: [{ required: true, message: "请选择渠道账户", trigger: "change" }],
  productId: [{ required: true, message: "请选择模型", trigger: "change" }],
  costDiscount: [{ required: true, message: "成本折扣不能为空", trigger: "blur" }],
  sellDiscount: [{ required: true, message: "售卖折扣不能为空", trigger: "blur" }]
}

/** 切换渠道后清空已选模型，并带出该渠道默认账户 */
function handleAddSupplierChange() {
  addForm.value.productId = undefined
  const account = defaultAccountOf(addForm.value.supplierId)
  addForm.value.supplierAccountId = account ? account.id : undefined
}

function openAddProduct() {
  if (!form.value.consumerId) {
    proxy.$modal.msgWarning("请先选择商机，确定客户后再添加模型")
    return
  }
  if (!channelOptions.value.length) {
    proxy.$modal.msgWarning("暂无可用的渠道模型，请先在「渠道管理」中维护渠道并上架模型")
    return
  }
  const defaultSupplierId = channelOptions.value[0].supplierId
  const account = defaultAccountOf(defaultSupplierId)
  if (!account) {
    proxy.$modal.msgWarning("所选渠道下暂无可用账户，请先在「渠道账户管理」中维护账户")
    return
  }
  addForm.value = {
    supplierId: defaultSupplierId,
    supplierAccountId: account.id,
    productId: undefined,
    costDiscount: 1,
    sellDiscount: 1,
    priceDetail: ""
  }
  addOpen.value = true
  nextTick(() => addRef.value && addRef.value.clearValidate())
}

function confirmAddProduct() {
  addRef.value.validate(valid => {
    if (!valid) {
      return
    }
    const priceDetail = String(addForm.value.priceDetail || "").trim()
    if (priceDetail && !isJson(priceDetail)) {
      proxy.$modal.msgError("价格明细不是合法的 JSON 格式")
      return
    }
    const hit = channelProducts.value.find(
      item => String(item.id) === String(addForm.value.productId)
        && String(item.supplierId) === String(addForm.value.supplierId)
    )
    if (!hit) {
      proxy.$modal.msgError("所选模型不属于该渠道，请重新选择")
      return
    }
    const accountHit = supplierAccounts.value.find(
      item => String(item.id) === String(addForm.value.supplierAccountId)
        && String(item.supplierId) === String(addForm.value.supplierId)
    )
    if (!accountHit) {
      proxy.$modal.msgError("所选渠道账户不属于该渠道，请重新选择")
      return
    }
    rows.value.push({
      supplierId: hit.supplierId,
      supplierName: hit.supplierName,
      supplierAccountId: accountHit.id,
      supplierAccountName: accountHit.accountName || "",
      productId: hit.id,
      productName: hit.name,
      productNo: hit.no,
      costDiscount: addForm.value.costDiscount,
      sellDiscount: addForm.value.sellDiscount,
      priceDetail
    })
    addOpen.value = false
  })
}

function removeRow(index) {
  rows.value.splice(index, 1)
}

/** 价格明细编辑 */
const priceOpen = ref(false)
const priceIndex = ref(-1)
const priceForm = ref({ priceDetail: "" })

function openPriceDetail(index) {
  priceIndex.value = index
  priceForm.value = { priceDetail: rows.value[index].priceDetail || "" }
  priceOpen.value = true
}

function isJson(text) {
  try {
    JSON.parse(text)
    return true
  } catch (e) {
    return false
  }
}

function formatPriceJson() {
  const text = String(priceForm.value.priceDetail || "").trim()
  if (!text) {
    return
  }
  if (!isJson(text)) {
    proxy.$modal.msgError("不是合法的 JSON，无法格式化")
    return
  }
  priceForm.value.priceDetail = JSON.stringify(JSON.parse(text), null, 2)
}

function confirmPriceDetail() {
  const text = String(priceForm.value.priceDetail || "").trim()
  if (text && !isJson(text)) {
    proxy.$modal.msgError("价格明细不是合法的 JSON 格式")
    return
  }
  if (rows.value[priceIndex.value]) {
    rows.value[priceIndex.value].priceDetail = text
  }
  priceOpen.value = false
}

/** 明细提交前校验 */
function validateRows(requireAtLeastOne) {
  if (requireAtLeastOne && !rows.value.length) {
    proxy.$modal.msgError("提交审批前请至少添加一条模型明细")
    return false
  }
  if (duplicateNames.value.length) {
    proxy.$modal.msgError(`存在重复模型：${duplicateNames.value.join("、")}`)
    return false
  }
  for (let i = 0; i < rows.value.length; i++) {
    const row = rows.value[i]
    if (!row.supplierId) {
      proxy.$modal.msgError(`第 ${i + 1} 行未选择渠道`)
      return false
    }
    if (!row.supplierAccountId) {
      proxy.$modal.msgError(`第 ${i + 1} 行未选择渠道账户`)
      return false
    }
    if (!row.productId) {
      proxy.$modal.msgError(`第 ${i + 1} 行未选择模型`)
      return false
    }
    for (const [key, label] of [["costDiscount", "成本折扣"], ["sellDiscount", "售卖折扣"]]) {
      const value = Number(row[key])
      if (!row[key] && row[key] !== 0) {
        proxy.$modal.msgError(`第 ${i + 1} 行「${modelLabel(row.productId, row.supplierId)}」${label}不能为空`)
        return false
      }
      if (value < 0.01 || value > 1) {
        proxy.$modal.msgError(`第 ${i + 1} 行「${modelLabel(row.productId, row.supplierId)}」${label}取值范围为 0.01 ~ 1.00`)
        return false
      }
    }
    if (row.priceDetail && !isJson(row.priceDetail)) {
      proxy.$modal.msgError(`第 ${i + 1} 行价格明细不是合法的 JSON`)
      return false
    }
  }
  return true
}

/** 组装明细入参：草稿保存与已签约明细维护共用 */
function buildProductsPayload() {
  return rows.value.map(row => ({
    supplierId: row.supplierId,
    supplierAccountId: row.supplierAccountId,
    productId: row.productId,
    costDiscount: row.costDiscount,
    sellDiscount: row.sellDiscount,
    priceDetail: row.priceDetail || null
  }))
}

/** 组装保存入参：主表不再携带渠道，渠道随明细行提交 */
function buildPayload() {
  return {
    id: form.value.id,
    busId: form.value.busId,
    consumerId: form.value.consumerId,
    contractName: String(form.value.contractName || "").trim(),
    contractEntity: String(form.value.contractEntity || "").trim(),
    expireTime: form.value.expireTime,
    remark: form.value.remark,
    products: buildProductsPayload()
  }
}

/** 已签约合同仅维护明细：不调用主信息保存接口，避免非草稿状态被后端拦截 */
function saveProductsOnly() {
  if (!validateRows(true)) {
    return
  }
  saving.value = true
  batchSaveContractProduct({ contractId: form.value.id, list: buildProductsPayload() })
    .then(() => {
      proxy.$modal.msgSuccess("模型明细已更新")
      router.push({ path: contractBasePath.value + "/detail", query: { id: form.value.id } })
    })
    .catch(() => {})
    .finally(() => {
      saving.value = false
    })
}

/** 保存（可串联提交审批） */
function handleSave(andSubmit) {
  if (productsOnly.value) {
    saveProductsOnly()
    return
  }
  formRef.value.validate(valid => {
    if (!valid) {
      return
    }
    if (!validateRows(andSubmit)) {
      return
    }
    saving.value = true
    saveContract(buildPayload())
      .then(res => {
        const id = res.data || form.value.id
        form.value.id = id
        isCreate.value = false
        proxy.$modal.msgSuccess("保存成功")
        if (andSubmit) {
          return submitAudit(id)
        }
        return null
      })
      .then(result => {
        if (result !== null) {
          proxy.$modal.msgSuccess("提交审批成功")
          router.push({ path: contractBasePath.value + "/detail", query: { id: form.value.id } })
          return
        }
        refreshContract()
      })
      .catch(() => {})
      .finally(() => {
        saving.value = false
      })
  })
}

/** 重新拉取详情，回显合同编号与明细 */
function refreshContract() {
  if (!form.value.id) {
    return
  }
  getContract(form.value.id).then(res => {
    applyDetail(res.data || {})
  })
}

function applyDetail(data) {
  form.value.id = data.id
  form.value.busId = data.busId
  form.value.consumerId = data.consumerId
  form.value.consumerName = data.consumerName || ""
  form.value.contactNo = data.contactNo || ""
  form.value.contractName = data.contractName || ""
  form.value.contractEntity = data.contractEntity || ""
  form.value.expireTime = data.expireTime
  form.value.remark = data.remark || ""
  form.value.status = data.status
  if (data.busName) {
    busOptions.value = [{ id: data.busId, name: data.busName }]
  }
  rows.value = (data.products || []).map(item => ({
    supplierId: item.supplierId,
    supplierName: item.supplierName,
    supplierAccountId: item.supplierAccountId,
    supplierAccountName: item.supplierAccountName,
    productId: item.productId,
    productName: item.productName,
    productNo: item.productNo,
    costDiscount: item.costDiscount,
    sellDiscount: item.sellDiscount,
    priceDetail: item.priceDetail || ""
  }))
  loadChannelData()
}

function handleBack() {
  if (productsOnly.value && form.value.id) {
    router.push({ path: contractBasePath.value + "/detail", query: { id: form.value.id } })
    return
  }
  router.push({ path: contractBasePath.value })
}

/** 初始化：编辑模式加载详情，新增模式预载商机 */
function init() {
  const id = route.query.id
  const mode = route.query.mode
  if (id) {
    isCreate.value = false
    loading.value = true
    getContract(id)
      .then(res => {
        const data = res.data || {}
        const status = Number(data.status)
        if (mode === "products") {
          // 已签约合同仅开放明细维护；草稿仍走常规编辑（可同时修改主信息）
          if (status !== 0 && status !== 2) {
            proxy.$modal.msgWarning("仅草稿或已签约状态的合同可以维护模型明细，已为您打开详情页")
            router.replace({ path: contractBasePath.value + "/detail", query: { id } })
            return
          }
          productsOnly.value = status === 2
          applyDetail(data)
          return
        }
        if (status !== 0) {
          proxy.$modal.msgWarning("仅草稿状态的合同可以编辑，已为您打开详情页")
          router.replace({ path: contractBasePath.value + "/detail", query: { id } })
          return
        }
        applyDetail(data)
      })
      .finally(() => {
        loading.value = false
      })
    return
  }
  isCreate.value = true
  searchBusiness()
  const busId = route.query.busId
  if (busId) {
    form.value.busId = Number(busId)
    handleBusChange(form.value.busId)
  }
}

init()
</script>

<style scoped>
.page-header {
  margin-bottom: 12px;
}

.section-card {
  margin-bottom: 14px;
}

.mode-alert {
  margin-bottom: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.model-name {
  color: var(--el-text-color-primary);
}

.model-no {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.price-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.price-text {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.duplicate-warning {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-color-danger);
}

.price-tips {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.footer-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 0 24px;
}
</style>

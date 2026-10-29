<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="商机名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入商机名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="请选择客户"
          filterable
          clearable
          :loading="customerLoading"
          style="width: 200px"
          @change="handleQuery"
        >
          <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="请选择供应商"
          filterable
          clearable
          :loading="supplierLoading"
          style="width: 200px"
          @change="handleQuery"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUser">
        <el-input
          v-model="queryParams.ownerUser"
          placeholder="请输入负责人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['crm:business:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['crm:business:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['crm:business:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['crm:business:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="businessList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" fixed="left"/>
      <el-table-column label="商机名称" align="center" prop="name" fixed="left"/>
      <el-table-column label="客户" align="center" prop="customer" fixed="left"/>
      <el-table-column label="供应商" align="center" prop="supplier" fixed="left"/>
      <el-table-column label="商机状态" align="center" prop="busStatus" fixed="left">
        <template #default="scope">
          <dict-tag :options="crm_bus_status" :value="scope.row.busStatus" />
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="ownerUser" />
      <el-table-column label="合同状态" align="center" prop="contactStatus">
        <template #default="scope">
          <dict-tag :options="crm_bus_contact" :value="scope.row.contactStatus" />
        </template>
      </el-table-column>
      <el-table-column label="结算状态" align="center" prop="settleStatus">
        <template #default="scope">
          <dict-tag :options="crm_bus_settle" :value="scope.row.settleStatus" />
        </template>
      </el-table-column>
      <el-table-column label="合同金额" align="center" prop="contactAmount" />
      <el-table-column label="授信金额" align="center" prop="creditAmount" />
      <el-table-column label="成交日期" align="center" prop="dealTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.dealTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="creator" />
      <el-table-column label="更新人" align="center" prop="updater" />
      <el-table-column label="是否删除" align="center" prop="deleted">
        <template #default="scope">
          <dict-tag :options="crm_deleted" :value="scope.row.deleted" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleViewData(scope.row)" v-hasPermi="['crm:business:query']">详情</el-button>
          <el-button
              v-for="action in rowActions(scope.row)"
              :key="action.code"
              link
              :type="action.type"
              :icon="action.icon || undefined"
              @click="handleStatusAction(scope.row, action)"
              v-hasPermi="['crm:business:edit']"
          >{{ action.label }}</el-button>
          <el-button
              v-if="isEditableStatus(scope.row.busStatus)"
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['crm:business:edit']"
          >修改</el-button>
          <el-button
              v-if="canDeleteStatus(scope.row.busStatus)"
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['crm:business:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- CRM 商机详情抽屉 -->
    <business-view-drawer ref="businessViewRef" />
    <!-- 添加或修改CRM 商机对话框 -->
    <el-dialog :title="title" v-model="open" width="760px" append-to-body>
      <el-form ref="businessRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="商机名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商机名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="客户编号" prop="customerId">
              <el-select
                  v-model="form.customerId"
                  placeholder="请选择客户"
                  filterable
                  clearable
                  :loading="customerLoading"
                  style="width: 100%"
              >
                <el-option
                    v-for="item in customerSelectOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                  v-model="form.supplierId"
                  placeholder="请选择供应商"
                  filterable
                  clearable
                  :loading="supplierLoading"
                  style="width: 100%"
                  @change="handleSupplierChange"
              >
                <el-option
                    v-for="item in supplierSelectOptions"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="负责人" prop="ownerUser">
              <el-input v-model="form.ownerUser" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模型明细" prop="items">
              <el-table
                  ref="itemTableRef"
                  :data="form.items || []"
                  border
                  size="small"
                  class="item-table"
                  style="width: 100%"
              >
                <!-- 选中模型后自动展开：在该行下方展示供应商配置的折扣区间 -->
                <el-table-column type="expand" width="36">
                  <template #default="scope">
                    <div class="item-discount">
                      <template v-if="rowDiscountInfo(scope.row).length">
                        <span class="item-discount__title">折扣区间</span>
                        <span
                            v-for="col in rowDiscountInfo(scope.row)"
                            :key="col.label"
                            class="item-discount__cell"
                        >
                          <span class="item-discount__label">{{ col.label }}</span>
                          <span class="item-discount__value">{{ col.value }}</span>
                        </span>
                      </template>
                      <span v-else class="item-discount__empty">该模型暂无折扣配置</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="模型" min-width="200">
                  <template #default="scope">
                    <el-select
                        v-model="scope.row.modelId"
                        placeholder="请选择模型"
                        filterable
                        clearable
                        :loading="supplierProjectsLoading"
                        :no-data-text="modelNoDataText"
                        style="width: 100%"
                        @change="val => handleModelChange(scope.row, val)"
                    >
                      <el-option
                          v-for="item in modelSelectOptions"
                          :key="item.modelId"
                          :label="item.modelName"
                          :value="item.modelId"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="成本折扣" width="150" align="center">
                  <template #default="scope">
                    <el-input-number
                        v-model="scope.row.costDiscount"
                        :min="0"
                        :max="100"
                        :precision="2"
                        :controls="false"
                        placeholder="请输入折扣"
                        style="width: 100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="售卖折扣" width="150" align="center">
                  <template #default="scope">
                    <el-input-number
                        v-model="scope.row.discount"
                        :min="0"
                        :max="100"
                        :precision="2"
                        :controls="false"
                        placeholder="请输入折扣"
                        style="width: 100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template #default="scope">
                    <el-button
                        link
                        type="primary"
                        icon="Plus"
                        title="新增一行"
                        @click="handleAddItem(scope.$index)"
                    ></el-button>
                    <el-button
                        v-if="(form.items || []).length > 1"
                        link
                        type="danger"
                        icon="Delete"
                        title="删除该行"
                        @click="handleRemoveItem(scope.$index)"
                    ></el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 提交测试：录入授信金额，未填写不允许进入测试流程 -->
    <el-dialog title="提交测试" v-model="submitTestOpen" width="420px" append-to-body>
      <el-form ref="submitTestFormRef" :model="submitTestForm" :rules="submitTestRules" label-width="90px">
        <el-form-item label="商机名称">
          <span class="submit-test__name">{{ submitTestRow.name || "-" }}</span>
        </el-form-item>
        <el-form-item label="授信金额" prop="creditAmount">
          <el-input-number
              v-model="submitTestForm.creditAmount"
              :min="0.01"
              :max="99999999.99"
              :precision="2"
              :controls="false"
              placeholder="请输入授信金额"
              style="width: 100%"
          />
        </el-form-item>
        <div class="submit-test__tip">授信金额为进入测试流程的前置条件，必须大于 0</div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitSubmitTest">确 定</el-button>
          <el-button @click="submitTestOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Business">
import { listBusiness, getBusiness, delBusiness, addBusiness, updateBusiness, getBusinessStatusOption, changeBusinessStatus } from "@/api/crm/business"
import { getCustomerOptions } from "@/api/crm/customer"
import { getSupplierOptions, getSupplierProjects } from "@/api/crm/supplier"
import BusinessViewDrawer from "./view"

const { crm_deleted,crm_bus_status,crm_bus_contact,crm_bus_settle } = useDict('crm_deleted','crm_bus_status','crm_bus_contact','crm_bus_settle')

const { proxy } = getCurrentInstance()

/** 商机状态（与后端 BusStatusEnum 一致）：0初始化 1跟进中 2提交测试 3测试中 4成交 5暂停 6中断 */
const BUS_STATUS = {
  INIT: 0,
  FOLLOWING: 1,
  SUBMIT_TEST: 2,
  TESTING: 3,
  DEAL: 4,
  PAUSED: 5,
  INTERRUPTED: 6
}

/** 终态：成交后不可再进行任何操作 */
const TERMINAL_STATUS = [BUS_STATUS.DEAL]

/** 目标状态对应的按钮文案与样式；未列出的状态直接使用后端返回的状态名称 */
const ACTION_META = {
  [BUS_STATUS.FOLLOWING]: { label: "跟进", type: "primary", icon: "Phone" },
  [BUS_STATUS.SUBMIT_TEST]: { label: "提交测试", type: "warning", icon: "Promotion" },
  [BUS_STATUS.TESTING]: { label: "进入测试", type: "warning", icon: "Promotion" },
  [BUS_STATUS.DEAL]: { label: "成交", type: "success", icon: "CircleCheck" },
  [BUS_STATUS.PAUSED]: { label: "暂停", type: "info", icon: "VideoPause" },
  [BUS_STATUS.INTERRUPTED]: { label: "中断", type: "danger", icon: "CircleClose" }
}

/**
 * 各状态业务上要求展示的操作（与后端 statusOption 返回的合法目标状态取交集）
 * 未列出的状态（提交测试/暂停/中断）不做收窄，按后端白名单展示，避免出现无路可走的状态
 */
const VISIBLE_TARGETS = {
  [BUS_STATUS.INIT]: [BUS_STATUS.FOLLOWING],
  [BUS_STATUS.FOLLOWING]: [BUS_STATUS.SUBMIT_TEST],
  [BUS_STATUS.TESTING]: [BUS_STATUS.DEAL, BUS_STATUS.INTERRUPTED],
  [BUS_STATUS.DEAL]: []
}

/** 各行允许流转的目标状态，key 为商机编号 */
const statusOptionsMap = reactive({})

/** 可删除的状态：仅初始化、跟进中；提交测试之后（含提交测试）不允许删除 */
const DELETABLE_STATUS = [BUS_STATUS.INIT, BUS_STATUS.FOLLOWING]

/** 是否处于可操作状态（成交为终态，不再允许修改/删除） */
function isEditableStatus(status) {
  return !TERMINAL_STATUS.includes(Number(status))
}

/** 是否允许删除（提交测试后进入测试流程，禁止删除） */
function canDeleteStatus(status) {
  return DELETABLE_STATUS.includes(Number(status))
}

/** 「提交测试」对话框：进入测试前必须录入授信金额 */
const submitTestOpen = ref(false)
const submitTestRow = ref({})
const submitTestFormRef = ref()
const submitTestForm = reactive({ creditAmount: null })
const submitTestRules = {
  creditAmount: [
    { required: true, message: "授信金额不能为空", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value == null || value === "") {
          callback(new Error("授信金额不能为空"))
          return
        }
        if (Number(value) <= 0) {
          callback(new Error("授信金额必须大于 0"))
          return
        }
        callback()
      },
      trigger: "blur"
    }
  ]
}

const businessList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

/** 客户下拉选项 */
const customerOptions = ref([])
const customerLoading = ref(false)
const customerLoaded = ref(false)

/** 供应商下拉选项 */
const supplierOptions = ref([])
const supplierLoading = ref(false)
const supplierLoaded = ref(false)

/** 当前供应商关联的产品：来源 /crm/supplier/{id}/projects，含模型型号 projectNo */
const supplierProjects = ref([])
const supplierProjectsLoading = ref(false)
/** supplierProjects 所属的供应商ID，用于避免切换供应商后误用上一次的结果 */
const supplierProjectsSupplierId = ref(null)

/** 模型明细表格实例：用于按行的价格配置展开/收起折扣提示行 */
const itemTableRef = ref()

/** 是否为空值 */
function isEmptyVal(val) {
  return val === null || val === undefined || val === ""
}

const data = reactive({
  form: { items: [createItemRow()] },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    customerId: undefined,
    supplierId: undefined,
    ownerUser: undefined,
    busStatus: undefined,
    dealTime: undefined,
    contactStatus: undefined,
    settleStatus: undefined,
    contactAmount: undefined,
    creditAmount: undefined,
    failRemark: undefined,
    creator: undefined,
    updater: undefined,
    deleted: undefined
  },
  rules: {
    name: [
      { required: true, message: "商机名称不能为空", trigger: "blur" }
    ],
    customerId: [
      { required: true, message: "客户不能为空", trigger: ["blur", "change"] }
    ],
    supplierId: [
      { required: true, message: "供应商不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)


/** 客户下拉数据：若当前记录引用的客户不在可选列表中，补一条兜底项避免只显示编号 */
const customerSelectOptions = computed(() => {
  const list = customerOptions.value
  const current = form.value.customerId
  if (current != null && current !== "" && !list.some(item => String(item.id) === String(current))) {
    return [{ id: current, name: `编号 ${current}（不在可选客户中）` }, ...list]
  }
  return list
})

/** 加载客户下拉选项（只请求一次，后续复用缓存） */
function loadCustomerOptions() {
  if (customerLoaded.value || customerLoading.value) {
    return
  }
  customerLoading.value = true
  getCustomerOptions().then(response => {
    customerOptions.value = response.data || []
    customerLoaded.value = true
  }).catch(() => {}).finally(() => {
    customerLoading.value = false
  })
}

/** 供应商下拉数据：若当前记录引用的供应商不在可选列表中，补一条兜底项避免只显示编号 */
const supplierSelectOptions = computed(() => {
  const list = supplierOptions.value
  const current = form.value.supplierId
  if (current != null && current !== "" && !list.some(item => String(item.id) === String(current))) {
    return [{ id: current, name: `编号 ${current}（不在可选供应商中）` }, ...list]
  }
  return list
})

/** 加载供应商下拉选项（只请求一次，后续复用缓存） */
function loadSupplierOptions() {
  if (supplierLoaded.value || supplierLoading.value) {
    return
  }
  supplierLoading.value = true
  getSupplierOptions().then(response => {
    supplierOptions.value = response.data || []
    supplierLoaded.value = true
  }).catch(() => {}).finally(() => {
    supplierLoading.value = false
  })
}

/** 是否已选定供应商 */
const hasSupplier = computed(() => !isEmptyVal(form.value.supplierId))

/** 当前供应商的产品是否已加载完成（按供应商ID比对，避免用到上一个供应商的缓存） */
const supplierProjectsReady = computed(
  () => hasSupplier.value && !isEmptyVal(supplierProjectsSupplierId.value)
    && String(supplierProjectsSupplierId.value) === String(form.value.supplierId)
)

/** 可选模型范围：仅限当前供应商关联的产品，未选供应商或产品未就绪时为空（不再回退全量模型） */
const availableModels = computed(() => {
  if (!supplierProjectsReady.value) {
    return []
  }
  return supplierProjects.value.map(item => ({
    modelId: item.projectId,
    modelName: item.projectNo || item.projectName || `编号 ${item.projectId}`
  }))
})

/** 模型下拉数据：补齐历史记录中引用但不在可选范围内的模型，避免只显示编号 */
const modelSelectOptions = computed(() => {
  const list = availableModels.value
  const extra = []
  ;(form.value.items || []).forEach(row => {
    const current = row.modelId
    if (current == null || current === "") {
      return
    }
    const exist = list.some(item => String(item.modelId) === String(current))
    const added = extra.some(item => String(item.modelId) === String(current))
    if (!exist && !added) {
      const fallback = supplierProjectsReady.value
        ? `（不在该供应商产品中）`
        : `（不在可选模型中）`
      extra.push({ modelId: current, modelName: row.modelName || `编号 ${current}${fallback}` })
    }
  })
  return extra.length ? [...extra, ...list] : list
})

/** 模型下拉空数据提示：模型选项完全由供应商产品决定，需明确区分未选、加载中、无产品与加载失败 */
const modelNoDataText = computed(() => {
  if (!hasSupplier.value) {
    return "请先选择供应商"
  }
  if (supplierProjectsLoading.value) {
    return "加载中..."
  }
  if (supplierProjectsReady.value) {
    return "该供应商暂未配置产品"
  }
  return "供应商产品获取失败，请重新选择供应商"
})

/** 折扣数值格式化：非数字原样返回，数字保留两位小数 */
function formatDiscountValue(val) {
  if (isEmptyVal(val)) {
    return null
  }
  const num = Number(val)
  return isNaN(num) ? String(val) : num.toFixed(2)
}

/** 折扣区间文案：两端都为空返回 -，只有一端时用 ≥ / ≤ 表示 */
function formatDiscountRange(min, max) {
  const minText = formatDiscountValue(min)
  const maxText = formatDiscountValue(max)
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

/**
 * 查找当前行模型在该供应商下的价格配置
 * <p>供应商产品未就绪（未选供应商 / 加载中 / 加载失败）时返回 null，不做任何默认值猜测</p>
 */
function findSupplierProject(modelId) {
  if (!supplierProjectsReady.value || isEmptyVal(modelId)) {
    return null
  }
  return supplierProjects.value.find(item => String(item.projectId) === String(modelId)) || null
}

/** 当前行展示用的折扣项：成本 / 代理 / 售卖；未配置（区间为 -）的项不展示，全部未配置时返回空数组 */
function rowDiscountInfo(row) {
  const config = findSupplierProject(row && row.modelId)
  if (!config) {
    return []
  }
  return [
    { label: "成本折扣", value: formatDiscountRange(config.minCostDiscount, config.maxCostDiscount) },
    { label: "代理折扣", value: formatDiscountRange(config.minAgentDiscount, config.maxAgentDiscount) },
    { label: "售卖折扣", value: formatDiscountRange(config.minSellDiscount, config.maxSellDiscount) }
  ].filter(item => item.value !== "-")
}

/** 按当前行的价格配置同步折扣提示行的展开状态 */
function syncRowExpansion(row) {
  const table = itemTableRef.value
  if (!table || !row) {
    return
  }
  table.toggleRowExpansion(row, rowDiscountInfo(row).length > 0)
}

/** 批量展开已有行的折扣提示：用于回显已保存明细、以及供应商产品异步加载完成后补展开 */
function expandRowsWithDiscount() {
  nextTick(() => {
    const table = itemTableRef.value
    if (!table) {
      return
    }
    ;(form.value.items || []).forEach(row => {
      if (rowDiscountInfo(row).length) {
        table.toggleRowExpansion(row, true)
      }
    })
  })
}

/** 加载指定供应商的关联产品（清空供应商时同步重置） */
function loadSupplierProjects(supplierId) {
  if (isEmptyVal(supplierId)) {
    supplierProjects.value = []
    supplierProjectsSupplierId.value = null
    return
  }
  supplierProjectsLoading.value = true
  getSupplierProjects(supplierId).then(response => {
    supplierProjects.value = Array.isArray(response.data) ? response.data : []
    supplierProjectsSupplierId.value = supplierId
    // 价格配置到手后，为已选模型的行补展开折扣提示（回显、或加载期间先选了模型的情况）
    expandRowsWithDiscount()
  }).catch(error => {
    // 加载失败时回退为全量模型，避免明细完全无法录入
    console.error("获取供应商产品明细失败:", error)
    supplierProjects.value = []
    supplierProjectsSupplierId.value = null
  }).finally(() => {
    supplierProjectsLoading.value = false
  })
}

/** 供应商变更：清空已有明细，并按新供应商收敛模型下拉 */
function handleSupplierChange(value) {
  form.value.items = [createItemRow()]
  loadSupplierProjects(value)
}

/** 创建一行空白明细 */
function createItemRow(costDiscount = null, discount = null) {
  return { modelId: null, modelName: null, costDiscount, discount }
}

/** 后端明细结构（businessProducts）→ 表单明细行：productId→modelId、sellDiscount→discount */
function toItemRow(record) {
  if (!record || typeof record !== "object") {
    return createItemRow()
  }
  return {
    ...record,
    modelId: isEmptyVal(record.productId) ? null : record.productId,
    modelName: isEmptyVal(record.modelName) ? null : record.modelName,
    costDiscount: isEmptyVal(record.costDiscount) ? null : record.costDiscount,
    discount: isEmptyVal(record.sellDiscount) ? null : record.sellDiscount
  }
}

/** 表单明细行 → 后端明细结构：modelId→productId、discount→sellDiscount */
function toBusinessProduct(row) {
  const { modelId, modelName, discount, ...rest } = row
  return {
    ...rest,
    busId: isEmptyVal(form.value.id) ? (rest.busId ?? null) : form.value.id,
    productId: isEmptyVal(modelId) ? null : modelId,
    costDiscount: isEmptyVal(row.costDiscount) ? null : row.costDiscount,
    sellDiscount: isEmptyVal(discount) ? null : discount,
    supplierId: isEmptyVal(form.value.supplierId) ? (rest.supplierId ?? null) : form.value.supplierId
  }
}

/** 规范化明细数据：读取后端 businessProducts，为空或非数组时兜底为一行空白行 */
function normalizeItems(record) {
  const list = record && Array.isArray(record.businessProducts) ? record.businessProducts : []
  return list.length ? list.map(toItemRow) : [createItemRow()]
}

/** 操作列 + 号：在当前行下方插入一行（无下标时追加到末尾） */
function handleAddItem(index) {
  if (!Array.isArray(form.value.items)) {
    form.value.items = [createItemRow()]
  }
  const row = createItemRow()
  if (typeof index === "number") {
    form.value.items.splice(index + 1, 0, row)
  } else {
    form.value.items.push(row)
  }
}

/** 操作列删除：移除指定行，至少保留一行 */
function handleRemoveItem(index) {
  if (!Array.isArray(form.value.items)) {
    return
  }
  form.value.items.splice(index, 1)
  if (!form.value.items.length) {
    form.value.items.push(createItemRow())
  }
}

/** 模型选中后同步名称，并按该模型的价格配置展开/收起折扣提示行 */
function handleModelChange(row, value) {
  const matched = modelSelectOptions.value.find(item => String(item.modelId) === String(value))
  row.modelName = matched ? matched.modelName : null
  syncRowExpansion(row)
}

/** 查询CRM 商机列表 */
function getList() {
  loading.value = true
  listBusiness(queryParams.value).then(response => {
    businessList.value = response.rows
    total.value = response.total
    loading.value = false
    refreshStatusOptions()
  })
}

/** 刷新当前页各商机可流转的目标状态 */
function refreshStatusOptions() {
  Object.keys(statusOptionsMap).forEach(key => delete statusOptionsMap[key])
  ;(businessList.value || []).forEach(row => {
    getBusinessStatusOption(row.id).then(res => {
      statusOptionsMap[row.id] = Array.isArray(res.data) ? res.data : []
    }).catch(() => {
      statusOptionsMap[row.id] = []
    })
  })
}

/** 计算某一行可展示的状态操作按钮 */
function rowActions(row) {
  const options = statusOptionsMap[row.id] || []
  const visible = VISIBLE_TARGETS[Number(row.busStatus)]
  return options
    .filter(item => !Array.isArray(visible) || visible.includes(Number(item.code)))
    .map(item => {
      const meta = ACTION_META[Number(item.code)] || {}
      return {
        code: Number(item.code),
        label: meta.label || item.info,
        type: meta.type || "primary",
        icon: meta.icon || ""
      }
    })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: null,
    customerId: null,
    supplierId: null,
    ownerUser: null,
    busStatus: null,
    dealTime: null,
    contactStatus: null,
    settleStatus: null,
    contactAmount: null,
    creditAmount: null,
    remark: null,
    failRemark: null,
    creator: null,
    updater: null,
    createTime: null,
    updateTime: null,
    deleted: null,
    items: [createItemRow()]
  }
  supplierProjects.value = []
  supplierProjectsSupplierId.value = null
  proxy.resetForm("businessRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  loadCustomerOptions()
  loadSupplierOptions()
  open.value = true
  title.value = "添加CRM 商机"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  loadCustomerOptions()
  loadSupplierOptions()
  const _id = row.id || ids.value
  getBusiness(_id).then(response => {
    const detail = response.data || {}
    form.value = { ...detail, items: normalizeItems(detail) }
    // 按已保存的供应商收敛模型下拉，但不清空明细
    loadSupplierProjects(form.value.supplierId)
    open.value = true
    title.value = "修改CRM 商机"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["businessRef"].validate(valid => {
    if (valid) {
      const payload = { ...form.value }
      // 过滤掉未填写任何内容的空白明细行，并按后端结构组装 businessProducts
      payload.businessProducts = (form.value.items || []).filter(row =>
          (row.modelId != null && row.modelId !== "") ||
          (row.costDiscount != null && row.costDiscount !== "") ||
          (row.discount != null && row.discount !== "")
      ).map(toBusinessProduct)
      delete payload.items
      if (form.value.id != null) {
        updateBusiness(payload).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addBusiness(payload).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  // 单行删除取当前行，批量删除按勾选编号在已加载列表中定位（拿不到时不做拦截）
  const targets = row && row.id
      ? [row]
      : businessList.value.filter(item => ids.value.includes(item.id))
  const blocked = targets.filter(item => !canDeleteStatus(item.busStatus))
  if (blocked.length) {
    const names = blocked.map(item => `「${item.name || item.id}」`).join("、")
    proxy.$modal.msgError(`${names}已提交测试，不允许删除`)
    return
  }
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除CRM 商机编号为"' + _ids + '"的数据项？').then(function() {
    return delBusiness(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 详情按钮操作 */
function handleViewData(row) {
  proxy.$refs["businessViewRef"].open(row.id)
}

/** 提交状态流转（服务端会再次校验流转合法性） */
function submitStatusChange(row, busStatus, extra = {}) {
  return changeBusinessStatus({ id: row.id, busStatus, ...extra }).then(() => {
    proxy.$modal.msgSuccess("状态变更成功")
    getList()
  })
}

/** 打开「提交测试」对话框：授信金额默认回显商机已有值 */
function openSubmitTest(row) {
  submitTestRow.value = row
  const current = Number(row.creditAmount)
  submitTestForm.creditAmount = isNaN(current) || current <= 0 ? null : current
  submitTestOpen.value = true
  nextTick(() => {
    if (submitTestFormRef.value) {
      submitTestFormRef.value.clearValidate()
    }
  })
}

/** 提交测试：校验授信金额后流转到「提交测试」状态 */
function submitSubmitTest() {
  submitTestFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    submitStatusChange(submitTestRow.value, BUS_STATUS.SUBMIT_TEST, {
      creditAmount: submitTestForm.creditAmount
    }).then(() => {
      submitTestOpen.value = false
    })
  })
}

/** 状态操作按钮：提交测试需录入授信金额，中断需填写备注，其余二次确认后提交 */
function handleStatusAction(row, action) {
  if (action.code === BUS_STATUS.SUBMIT_TEST) {
    openSubmitTest(row)
    return
  }
  if (action.code === BUS_STATUS.INTERRUPTED) {
    proxy.$modal.prompt(`请输入商机「${row.name}」的中断备注`).then(({ value }) => {
      const failRemark = (value || "").trim()
      if (!failRemark) {
        proxy.$modal.msgWarning("中断备注不能为空")
        return
      }
      submitStatusChange(row, action.code, { failRemark })
    }).catch(() => {})
    return
  }
  const tip = action.code === BUS_STATUS.DEAL
      ? `是否确认将商机「${row.name}」标记为成交？成交后不可再进行操作。`
      : `是否确认将商机「${row.name}」变更为「${action.label}」？`
  proxy.$modal.confirm(tip).then(() => {
    submitStatusChange(row, action.code)
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('crm/business/export', {
    ...queryParams.value
  }, `business_${new Date().getTime()}.xlsx`)
}

getList()
loadCustomerOptions()
loadSupplierOptions()
</script>

<style scoped>
.item-table :deep(.el-table__cell) {
  padding: 4px 0;
}
/* 展开行：整行展示当前模型的价格区间，压缩内边距避免明细表格行高被撑开 */
.item-table :deep(.el-table__expanded-cell) {
  padding: 6px 12px;
  background: #fafafa;
}
.item-discount {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 24px;
  font-size: 12px;
  line-height: 20px;
}
.item-discount__title {
  color: #909399;
}
.item-discount__cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.item-discount__label {
  color: #909399;
}
.item-discount__value {
  color: #303133;
  font-weight: 500;
}
.item-discount__empty {
  color: #c0c4cc;
}
.submit-test__name {
  color: #303133;
}
.submit-test__tip {
  padding-left: 90px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}
</style>

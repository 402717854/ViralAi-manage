<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="商机名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入商机名称"
          clearable
          style="width: 200px"
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
        >
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="渠道" prop="supplierId">
        <!-- 渠道为明细行属性，此处按「明细中存在该渠道」筛选 -->
        <el-select
          v-model="queryParams.supplierId"
          placeholder="按明细渠道筛选"
          filterable
          clearable
          :loading="supplierLoading"
          style="width: 200px"
        >
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUser">
        <el-input
          v-model="queryParams.ownerUser"
          placeholder="请输入负责人"
          clearable
          style="width: 160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商机状态" prop="statusList">
        <el-select
          v-model="queryParams.statusList"
          placeholder="请选择状态"
          multiple
          collapse-tags
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="dict in crm_bus_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="合同状态" prop="contactStatus">
        <el-select v-model="queryParams.contactStatus" placeholder="请选择" clearable style="width: 120px">
          <el-option label="未签" :value="0" />
          <el-option label="已签" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="成交时间">
        <el-date-picker
          v-model="dealTimeRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:business:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['crm:business:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="businessList">
      <el-table-column label="商机名称" prop="name" min-width="180" fixed="left" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="handleView(scope.row)">{{ scope.row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="客户" prop="customer" min-width="140" show-overflow-tooltip />
      <el-table-column label="渠道（明细）" prop="supplier" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.supplier">{{ scope.row.supplier }}</span>
          <span v-else class="text-placeholder">-</span>
        </template>
      </el-table-column>
      <el-table-column label="负责人" prop="ownerUser" width="110" align="center" />
      <el-table-column label="商机状态" prop="busStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="statusMeta(scope.row.busStatus).type" :effect="statusMeta(scope.row.busStatus).plain ? 'plain' : 'dark'">
            {{ scope.row.statusName || statusMeta(scope.row.busStatus).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="意向金额" prop="contactAmount" width="130" align="right">
        <template #default="scope">{{ formatAmount(scope.row.contactAmount) }}</template>
      </el-table-column>
      <el-table-column label="授信金额" prop="creditAmount" width="130" align="right">
        <template #default="scope">{{ formatAmount(scope.row.creditAmount) }}</template>
      </el-table-column>
      <el-table-column label="成交时间" prop="dealTime" width="160" align="center">
        <template #default="scope">
          <span>{{ scope.row.dealTime ? parseTime(scope.row.dealTime) : "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否签合同" width="110" align="center">
        <template #default="scope">
          <el-tag v-if="Number(scope.row.contactStatus) === 1" type="success" size="small">已签</el-tag>
          <el-tag v-else type="info" size="small" effect="plain">未签</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['crm:business:query']">详情</el-button>
          <el-button
            v-if="canEdit(scope.row)"
            link
            type="primary"
            icon="Edit"
            @click="handleEdit(scope.row)"
            v-hasPermi="['crm:business:edit']"
          >编辑</el-button>
          <el-button
            v-for="action in rowActions(scope.row)"
            :key="action.key"
            link
            :type="action.type"
            :icon="action.icon"
            @click="handleAction(scope.row, action)"
            v-hasPermi="['crm:business:edit']"
          >{{ action.label }}</el-button>
          <el-button
            v-if="canConvert(scope.row)"
            link
            type="success"
            icon="DocumentAdd"
            @click="handleConvert(scope.row)"
            v-hasPermi="['crm:business:convert']"
          >转合同</el-button>
          <el-button
            v-if="canViewContract(scope.row)"
            link
            type="success"
            icon="Document"
            @click="handleViewContract(scope.row)"
            v-hasPermi="['crm:business:query']"
          >查看合同</el-button>
          <el-button
            v-if="canDelete(scope.row)"
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
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 中断弹窗：失败备注必填 -->
    <el-dialog title="中断商机" v-model="interruptOpen" width="460px" append-to-body>
      <el-form ref="interruptFormRef" :model="interruptForm" :rules="interruptRules" label-width="90px">
        <el-form-item label="商机名称">
          <span>{{ interruptRow.name || "-" }}</span>
        </el-form-item>
        <el-form-item label="失败备注" prop="failRemark">
          <el-input
            v-model="interruptForm.failRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请填写中断原因，中断后不可恢复"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="submitInterrupt">确 认 中 断</el-button>
          <el-button @click="interruptOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Business">
import { listBusiness, delBusiness, changeBusinessStatus, convertContract } from "@/api/crm/business"
import { getCustomerOptions } from "@/api/crm/customer"
import { getSupplierOptions } from "@/api/crm/supplier"

const { proxy } = getCurrentInstance()
const route = useRoute()
const { crm_bus_status } = proxy.useDict("crm_bus_status")

/** 商机状态（与后端 BusStatusEnum 一致） */
const STATUS = {
  INIT: 0,
  FOLLOWING: 1,
  SUBMIT_TEST: 2,
  TESTING: 3,
  DEAL: 4,
  PAUSED: 5,
  INTERRUPTED: 6
}

/** 状态标签样式：初始化灰、跟进中蓝、提交测试浅蓝、测试中黄、成交绿、暂停浅橙、中断红 */
const STATUS_META = {
  [STATUS.INIT]: { label: "初始化", type: "info", plain: true },
  [STATUS.FOLLOWING]: { label: "跟进中", type: "primary", plain: false },
  [STATUS.SUBMIT_TEST]: { label: "提交测试", type: "primary", plain: true },
  [STATUS.TESTING]: { label: "测试中", type: "warning", plain: false },
  [STATUS.DEAL]: { label: "成交", type: "success", plain: false },
  [STATUS.PAUSED]: { label: "暂停", type: "warning", plain: true },
  [STATUS.INTERRUPTED]: { label: "中断", type: "danger", plain: false }
}

/**
 * 各状态的可用流转动作（与需求 5.2 列表按钮一致）
 * 后端仍会按状态机白名单二次校验，前端仅作展示收敛
 */
const ROW_ACTIONS = {
  [STATUS.INIT]: [{ key: "follow", target: STATUS.FOLLOWING, label: "开始跟进", type: "primary", icon: "Phone" }],
  [STATUS.FOLLOWING]: [
    { key: "submitTest", target: STATUS.SUBMIT_TEST, label: "提交测试", type: "warning", icon: "Promotion" },
    { key: "pause", target: STATUS.PAUSED, label: "暂停", type: "info", icon: "VideoPause" },
    { key: "interrupt", target: STATUS.INTERRUPTED, label: "中断", type: "danger", icon: "CircleClose" }
  ],
  [STATUS.SUBMIT_TEST]: [
    { key: "testing", target: STATUS.TESTING, label: "进入测试", type: "warning", icon: "Promotion" },
    { key: "backFollowing", target: STATUS.FOLLOWING, label: "退回跟进", type: "primary", icon: "Back" },
    { key: "interrupt", target: STATUS.INTERRUPTED, label: "中断", type: "danger", icon: "CircleClose" }
  ],
  [STATUS.TESTING]: [
    { key: "deal", target: STATUS.DEAL, label: "成交", type: "success", icon: "CircleCheck" },
    { key: "backFollowing", target: STATUS.FOLLOWING, label: "退回跟进", type: "primary", icon: "Back" },
    { key: "interrupt", target: STATUS.INTERRUPTED, label: "中断", type: "danger", icon: "CircleClose" }
  ],
  [STATUS.DEAL]: [],
  [STATUS.PAUSED]: [{ key: "resume", target: STATUS.FOLLOWING, label: "恢复跟进", type: "primary", icon: "Phone" }],
  [STATUS.INTERRUPTED]: []
}

const businessList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dealTimeRange = ref([])
const customerOptions = ref([])
const customerLoading = ref(false)
const supplierOptions = ref([])
const supplierLoading = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  customerId: undefined,
  supplierId: undefined,
  ownerUser: undefined,
  statusList: [],
  contactStatus: undefined
})

/** 当前列表路由，编辑/详情页据此推导，避免菜单父级路径差异导致跳转失效 */
const businessBasePath = computed(() => route.path.replace(/\/+$/, ""))

/** 合同模块路由前缀：由商机路由推导，推导失败时回退到 /crm/contract */
const contractBasePath = computed(() => {
  const base = businessBasePath.value
  return /\/business$/.test(base) ? base.replace(/\/business$/, "/contract") : "/crm/contract"
})

function statusMeta(status) {
  return STATUS_META[Number(status)] || { label: "未知", type: "info", plain: true }
}

function rowActions(row) {
  return ROW_ACTIONS[Number(row.busStatus)] || []
}

function canEdit(row) {
  const status = Number(row.busStatus)
  return status === STATUS.INIT || status === STATUS.FOLLOWING
}

function canDelete(row) {
  const status = Number(row.busStatus)
  return status === STATUS.INIT || status === STATUS.FOLLOWING
}

/** 仅成交且尚未生成合同的商机允许转合同 */
function canConvert(row) {
  return Number(row.busStatus) === STATUS.DEAL && Number(row.hasContract) !== 1
}

function canViewContract(row) {
  return row.contractId != null && row.contractId !== ""
}

/** 金额展示：空值显示 -，数字千分位两位小数 */
function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  const num = Number(value)
  if (isNaN(num)) {
    return String(value)
  }
  return num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 组装查询条件：状态多选转逗号串，成交时间写入 params */
function buildQuery() {
  const { statusList, ...rest } = queryParams.value
  const query = { ...rest, params: {} }
  if (Array.isArray(statusList) && statusList.length) {
    query.params.statusList = statusList.join(",")
  }
  if (Array.isArray(dealTimeRange.value) && dealTimeRange.value.length === 2) {
    query.params.beginDealTime = dealTimeRange.value[0]
    query.params.endDealTime = dealTimeRange.value[1]
  }
  return query
}

function getList() {
  loading.value = true
  listBusiness(buildQuery()).then(res => {
    businessList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadSupplierOptions() {
  supplierLoading.value = true
  getSupplierOptions().then(res => {
    supplierOptions.value = res.data || []
  }).finally(() => {
    supplierLoading.value = false
  })
}

function loadCustomerOptions() {
  customerLoading.value = true
  getCustomerOptions().then(res => {
    customerOptions.value = res.data || []
  }).finally(() => {
    customerLoading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dealTimeRange.value = []
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    customerId: undefined,
    supplierId: undefined,
    ownerUser: undefined,
    statusList: [],
    contactStatus: undefined
  }
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleAdd() {
  proxy.$router.push({ path: businessBasePath.value + "/edit" })
}

function handleEdit(row) {
  proxy.$router.push({ path: businessBasePath.value + "/edit", query: { id: row.id } })
}

function handleView(row) {
  proxy.$router.push({ path: businessBasePath.value + "/detail", query: { id: row.id } })
}

/** 状态流转：中断需填写备注，成交需二次确认，其余直接确认 */
function handleAction(row, action) {
  if (action.target === STATUS.INTERRUPTED) {
    openInterrupt(row)
    return
  }
  const tip = action.target === STATUS.DEAL
    ? `是否确认将商机「${row.name}」标记为成交？成交后商机基础信息与意向产品清单将被锁定。`
    : `是否确认将商机「${row.name}」变更为「${action.label}」？`
  proxy.$modal.confirm(tip).then(() => {
    changeBusinessStatus({ id: row.id, busStatus: action.target }).then(() => {
      proxy.$modal.msgSuccess("状态变更成功")
      getList()
    })
  }).catch(() => {})
}

/** 中断弹窗：失败备注必填 */
const interruptOpen = ref(false)
const interruptRow = ref({})
const interruptFormRef = ref()
const interruptForm = reactive({ failRemark: "" })
const interruptRules = {
  failRemark: [
    { required: true, message: "中断备注不能为空", trigger: "blur" },
    { max: 255, message: "中断备注不能超过 255 个字符", trigger: "blur" }
  ]
}

function openInterrupt(row) {
  interruptRow.value = row
  interruptForm.failRemark = ""
  interruptOpen.value = true
  nextTick(() => interruptFormRef.value && interruptFormRef.value.clearValidate())
}

function submitInterrupt() {
  interruptFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    changeBusinessStatus({
      id: interruptRow.value.id,
      busStatus: STATUS.INTERRUPTED,
      failRemark: interruptForm.failRemark.trim()
    }).then(() => {
      proxy.$modal.msgSuccess("商机已中断")
      interruptOpen.value = false
      getList()
    })
  })
}

/** 转合同：确认后生成草稿合同并跳转合同编辑页 */
function handleConvert(row) {
  proxy.$modal.confirm("将根据当前商机意向清单生成草稿合同，确认继续？").then(() => {
    return convertContract(row.id)
  }).then(res => {
    proxy.$modal.msgSuccess("已生成草稿合同")
    getList()
    if (res.data) {
      proxy.$router.push({ path: contractBasePath.value + "/edit", query: { id: res.data } })
    }
  }).catch(() => {})
}

function handleViewContract(row) {
  proxy.$router.push({ path: contractBasePath.value + "/detail", query: { id: row.contractId } })
}

function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除商机「${row.name}」？`).then(() => {
    return delBusiness(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    getList()
  }).catch(() => {})
}

function handleExport() {
  proxy.download("crm/business/export", buildQuery(), `business_${new Date().getTime()}.xlsx`)
}

getList()
loadCustomerOptions()
loadSupplierOptions()
</script>

<style scoped>
.app-container :deep(.el-table .cell) {
  line-height: 22px;
}
.text-placeholder {
  color: #c0c4cc;
}
</style>

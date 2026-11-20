<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Back" @click="goBack">返回账单列表</el-button>
      </el-col>
      <el-col :span="14">
        <span class="page-tip">
          子账单按用量的调用日期拆分，金额口径与父账单一致；没有匹配成功用量的日期不生成子账单
        </span>
      </el-col>
    </el-row>

    <el-card v-if="bill" shadow="never" class="bill-card">
      <template #header>
        <div class="card-header">
          <span>父账单 {{ bill.billNo }}</span>
          <dict-tag :options="crm_bill_status" :value="String(bill.billStatus)" />
        </div>
      </template>
      <el-descriptions :column="4" size="small" border>
        <el-descriptions-item label="账期">{{ bill.billCycle }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ bill.contactNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ bill.customerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ bill.supplierName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="应收总额">{{ formatAmount(bill.totalSellAmount) }}</el-descriptions-item>
        <el-descriptions-item label="总成本">{{ formatAmount(bill.totalCostAmount) }}</el-descriptions-item>
        <el-descriptions-item label="总毛利">{{ formatAmount(bill.totalGrossProfit) }}</el-descriptions-item>
        <el-descriptions-item label="用量条数">{{ detailUsageCount ?? "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="90px">
      <el-form-item label="子账单编号" prop="subBillNo">
        <el-input
          v-model="queryParams.subBillNo"
          placeholder="请输入子账单编号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同编号" prop="contactNo">
        <el-input
          v-model="queryParams.contactNo"
          placeholder="请输入合同编号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item v-if="!billId" label="渠道" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="请选择渠道"
          filterable
          clearable
          :loading="supplierLoading"
          style="width: 180px"
        >
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!billId" label="账期" prop="billCycle">
        <el-date-picker
          v-model="queryParams.billCycle"
          type="month"
          value-format="YYYY-MM"
          placeholder="请选择账期"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="账单日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
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

    <el-table v-loading="loading" :data="subList" show-summary :summary-method="summaryMethod">
      <el-table-column label="账单日期" align="center" prop="billDate" width="115">
        <template #default="scope">{{ parseTime(scope.row.billDate, '{y}-{m}-{d}') || "-" }}</template>
      </el-table-column>
      <el-table-column label="子账单编号" align="center" prop="subBillNo" min-width="235" show-overflow-tooltip />
      <el-table-column
        v-if="!billId"
        label="父账单编号"
        align="center"
        prop="billNo"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column label="合同编号" align="center" prop="contactNo" min-width="190" show-overflow-tooltip />
      <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
      <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
      <el-table-column label="用量条数" align="center" prop="usageCount" width="100" />
      <el-table-column label="token" align="center" prop="totalTokens" width="120">
        <template #default="scope">{{ formatNumber(scope.row.totalTokens) }}</template>
      </el-table-column>
      <el-table-column label="应收" align="center" prop="totalSellAmount" width="155" show-overflow-tooltip>
        <template #default="scope">{{ formatAmount(scope.row.totalSellAmount) }}</template>
      </el-table-column>
      <el-table-column label="成本" align="center" prop="totalCostAmount" width="155" show-overflow-tooltip>
        <template #default="scope">{{ formatAmount(scope.row.totalCostAmount) }}</template>
      </el-table-column>
      <el-table-column label="毛利" align="center" prop="totalGrossProfit" width="155" show-overflow-tooltip>
        <template #default="scope">{{ formatAmount(scope.row.totalGrossProfit) }}</template>
      </el-table-column>
      <el-table-column label="账单状态" align="center" width="110">
        <template #default="scope">
          <dict-tag :options="crm_bill_status" :value="String(scope.row.billStatus)" />
        </template>
      </el-table-column>
      <el-table-column label="生成时间" align="center" prop="createTime" width="155">
        <template #default="scope">
          {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') || "-" }}
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
  </div>
</template>

<script setup name="BillSub">
import { listBillSub, getBill } from "@/api/crm/bill"
import { getSupplierOptions } from "@/api/crm/supplier"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { crm_bill_status } = proxy.useDict("crm_bill_status")
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const subList = ref([])
const total = ref(0)
const supplierOptions = ref([])
const supplierLoading = ref(false)

/** 父账单ID：从账单列表跳转时带过来，独立菜单进入时为空 */
const billId = ref(normalizeId(route.query.billId))
const bill = ref(null)
const detailUsageCount = ref(null)

/** 账单日期范围：[开始日期, 结束日期]，格式 YYYY-MM-DD */
const dateRange = ref([])

const queryParams = ref({
  pageNum: 1,
  pageSize: 20,
  subBillNo: undefined,
  contactNo: undefined,
  supplierId: undefined,
  billCycle: undefined
})

function normalizeId(value) {
  const text = String(value ?? "").trim()
  if (!/^\d+$/.test(text)) {
    return undefined
  }
  return Number(text)
}

function formatNumber(value) {
  if (value === null || value === undefined || value === "") {
    return "0"
  }
  return Number(value).toLocaleString()
}

function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(8)
}

function buildQuery() {
  const query = {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    billId: billId.value,
    subBillNo: queryParams.value.subBillNo || undefined,
    contactNo: queryParams.value.contactNo || undefined,
    supplierId: queryParams.value.supplierId || undefined,
    billCycle: queryParams.value.billCycle || undefined,
    params: {}
  }
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    query.params.beginDate = dateRange.value[0]
    query.params.endDate = dateRange.value[1]
  }
  return query
}

/** 合计行只统计当前页：后端分页下无法拿到全量合计，标签必须写清楚口径 */
const SUMMARY_PROPS = ["usageCount", "totalTokens", "totalSellAmount", "totalCostAmount", "totalGrossProfit"]

function summaryMethod({ columns, data }) {
  return columns.map((column, index) => {
    if (index === 0) {
      return "本页合计"
    }
    if (!SUMMARY_PROPS.includes(column.property)) {
      return ""
    }
    const sum = data.reduce((acc, item) => {
      const value = Number(item[column.property])
      return acc + (Number.isNaN(value) ? 0 : value)
    }, 0)
    if (column.property === "usageCount" || column.property === "totalTokens") {
      return formatNumber(sum)
    }
    return formatAmount(sum)
  })
}

function getList() {
  loading.value = true
  listBillSub(buildQuery()).then(res => {
    subList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = {
    pageNum: 1,
    pageSize: queryParams.value.pageSize,
    subBillNo: undefined,
    contactNo: undefined,
    supplierId: undefined,
    billCycle: undefined
  }
  dateRange.value = []
  getList()
}

/** 父账单概要：仅从账单列表跳转时展示，独立菜单进入无单一父账单上下文 */
function loadBill() {
  if (!billId.value) {
    return
  }
  getBill(billId.value).then(res => {
    const data = res.data || {}
    bill.value = data.bill || null
    detailUsageCount.value = data.usageCount ?? null
  })
}

function goBack() {
  const base = route.path.replace(/\/sub\/?$/, "")
  if (base && base !== route.path) {
    router.push({ path: base })
    return
  }
  router.push({ path: "/finance/bill" })
}

function loadSupplierOptions() {
  supplierLoading.value = true
  getSupplierOptions().then(res => {
    supplierOptions.value = res.data || []
  }).finally(() => {
    supplierLoading.value = false
  })
}

loadBill()
getList()
if (!billId.value) {
  loadSupplierOptions()
}
</script>

<style scoped>
.page-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 32px;
}

.bill-card {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

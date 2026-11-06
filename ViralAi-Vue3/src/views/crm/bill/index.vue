<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="账单编号" prop="billNo">
        <el-input
          v-model="queryParams.billNo"
          placeholder="请输入账单编号"
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
      <el-form-item label="渠道" prop="supplierId">
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
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="请选择客户"
          filterable
          clearable
          :loading="customerLoading"
          style="width: 180px"
        >
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="账期" prop="billCycle">
        <el-date-picker
          v-model="queryParams.billCycle"
          type="month"
          value-format="YYYY-MM"
          placeholder="请选择账期"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="账单状态" prop="statusList">
        <el-select
          v-model="queryParams.statusList"
          placeholder="请选择"
          multiple
          collapse-tags
          clearable
          style="width: 200px"
        >
          <el-option v-for="dict in crm_bill_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
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
          @click="openGenerate"
          v-hasPermi="['crm:bill:generate']"
        >生成账单</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['crm:bill:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="billList">
      <el-table-column label="账单编号" align="center" prop="billNo" min-width="200" fixed="left" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="openDetail(scope.row)">{{ scope.row.billNo }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="合同编号" align="center" prop="contactNo" min-width="190" show-overflow-tooltip />
      <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
      <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
      <el-table-column label="账期" align="center" prop="billCycle" width="100" />
      <el-table-column label="总token" align="center" prop="totalTokens" width="120" />
      <el-table-column label="应收总额" align="center" width="150" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatAmount(scope.row.totalSellAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总成本" align="center" width="150" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatAmount(scope.row.totalCostAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总毛利" align="center" width="150" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ formatAmount(scope.row.totalGrossProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账单状态" align="center" width="110">
        <template #default="scope">
          <dict-tag :options="crm_bill_status" :value="String(scope.row.billStatus)" />
        </template>
      </el-table-column>
      <el-table-column label="待处理差异" align="center" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.pendingDiffCount > 0" type="danger"> {{ scope.row.pendingDiffCount }} 条 </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="复核人" align="center" prop="auditorName" width="110" />
      <el-table-column label="复核时间" align="center" prop="auditTime" width="165">
        <template #default="scope">
          <span>{{ parseTime(scope.row.auditTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="openDetail(scope.row)">详情</el-button>
          <el-button
            v-if="canAudit(scope.row)"
            link
            type="warning"
            icon="Stamp"
            @click="openAudit(scope.row)"
            v-hasPermi="['crm:bill:audit']"
          >复核</el-button>
          <el-button
            v-if="canSettle(scope.row)"
            link
            type="success"
            icon="Money"
            @click="handleSettle(scope.row)"
            v-hasPermi="['crm:bill:settle']"
          >结算</el-button>
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

    <!-- 生成账单 -->
    <el-dialog title="生成对账账单" v-model="generateOpen" width="560px" append-to-body>
      <el-form :model="generateForm" :rules="generateRules" ref="generateRef" label-width="90px">
        <el-form-item label="渠道" prop="supplierId">
          <el-select v-model="generateForm.supplierId" placeholder="请选择渠道" filterable style="width: 100%">
            <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期" prop="billCycle">
          <el-date-picker
            v-model="generateForm.billCycle"
            type="month"
            value-format="YYYY-MM"
            placeholder="请选择账期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="按合同分组汇总该渠道该账期的待入账用量；已入账的用量不会重复归集，同账期若仍有未入账用量会生成补充账单。"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitGenerate" :loading="generateSubmitting">生 成</el-button>
          <el-button @click="generateOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 账单复核 -->
    <el-dialog title="账单复核" v-model="auditOpen" width="540px" append-to-body>
      <el-form :model="auditForm" :rules="auditRules" ref="auditRef" label-width="90px">
        <el-form-item label="账单编号">
          <span class="plain-text">{{ auditForm.billNo }}</span>
        </el-form-item>
        <el-form-item label="复核结果" prop="billStatus">
          <el-radio-group v-model="auditForm.billStatus">
            <el-radio :value="2">确认</el-radio>
            <el-radio :value="4">差异挂起</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="复核意见" prop="remark">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            maxlength="480"
            show-word-limit
            placeholder="差异挂起时请说明原因"
          />
        </el-form-item>
        <el-alert
          v-if="auditForm.pendingDiffCount > 0"
          type="warning"
          :closable="false"
          show-icon
          :title="`该账期存在 ${auditForm.pendingDiffCount} 条待处理差异，确认前请先到「差异处理」中处理完毕`"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAudit" :loading="auditSubmitting">确 定</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 账单详情 -->
    <el-drawer v-model="detailOpen" title="账单详情" size="60%" append-to-body>
      <div v-if="detail.bill" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="账单编号">{{ detail.bill.billNo }}</el-descriptions-item>
          <el-descriptions-item label="账期">{{ detail.bill.billCycle }}</el-descriptions-item>
          <el-descriptions-item label="账单状态">
            <dict-tag :options="crm_bill_status" :value="String(detail.bill.billStatus)" />
          </el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ detail.bill.contactNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.bill.customerName }}</el-descriptions-item>
          <el-descriptions-item label="渠道">{{ detail.bill.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="总token">{{ detail.bill.totalTokens }}</el-descriptions-item>
          <el-descriptions-item label="应收总额">{{ formatAmount(detail.bill.totalSellAmount) }}</el-descriptions-item>
          <el-descriptions-item label="总成本">{{ formatAmount(detail.bill.totalCostAmount) }}</el-descriptions-item>
          <el-descriptions-item label="总毛利">{{ formatAmount(detail.bill.totalGrossProfit) }}</el-descriptions-item>
          <el-descriptions-item label="复核人">{{ detail.bill.auditorName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="复核时间">
            {{ parseTime(detail.bill.auditTime, '{y}-{m}-{d} {h}:{i}') || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detail.bill.remark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="detailTab" class="detail-tabs">
          <el-tab-pane :label="`用量明细（${(detail.usages || []).length}）`" name="usages">
            <el-table :data="detail.usages" size="small" max-height="420">
              <el-table-column label="流水号" prop="callNo" min-width="220" show-overflow-tooltip />
              <el-table-column label="模型编码" prop="productCode" min-width="140" show-overflow-tooltip />
              <el-table-column label="token" prop="totalTokens" width="100" />
              <el-table-column label="应收" width="150" show-overflow-tooltip>
                <template #default="scope">{{ formatAmount(scope.row.sellAmount) }}</template>
              </el-table-column>
              <el-table-column label="成本" width="150" show-overflow-tooltip>
                <template #default="scope">{{ formatAmount(scope.row.costAmount) }}</template>
              </el-table-column>
              <el-table-column label="毛利" width="150" show-overflow-tooltip>
                <template #default="scope">{{ formatAmount(scope.row.grossProfit) }}</template>
              </el-table-column>
              <el-table-column label="调用时间" width="160">
                <template #default="scope">{{ parseTime(scope.row.callTime, '{y}-{m}-{d} {h}:{i}') }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`对账差异（${(detail.diffs || []).length}）`" name="diffs">
            <el-table :data="detail.diffs" size="small" max-height="420">
              <el-table-column label="差异类型" width="110">
                <template #default="scope">
                  <dict-tag :options="crm_bill_diff_type" :value="String(scope.row.diffType)" />
                </template>
              </el-table-column>
              <el-table-column label="描述" prop="diffDesc" min-width="240" show-overflow-tooltip />
              <el-table-column label="流水号" prop="callNo" min-width="200" show-overflow-tooltip />
              <el-table-column label="处理状态" width="100">
                <template #default="scope">
                  <dict-tag :options="crm_bill_diff_handle" :value="String(scope.row.handleStatus)" />
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Bill">
import { listBill, getBill, generateBill, auditBill, settleBill } from "@/api/crm/bill"
import { getSupplierOptions } from "@/api/crm/supplier"
import { getCustomerOptions } from "@/api/crm/customer"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { crm_bill_status, crm_bill_diff_type, crm_bill_diff_handle } = proxy.useDict(
  "crm_bill_status",
  "crm_bill_diff_type",
  "crm_bill_diff_handle"
)

const billList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const supplierOptions = ref([])
const supplierLoading = ref(false)
const customerOptions = ref([])
const customerLoading = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  billNo: undefined,
  contactNo: undefined,
  supplierId: undefined,
  customerId: undefined,
  billCycle: undefined,
  statusList: []
})

/** 账单状态常量，与后端 ContractBillStatusEnum 一致 */
const BILL_STATUS = {
  WAIT_AUDIT: 1,
  CONFIRMED: 2,
  SETTLED: 3,
  DIFF_HANG: 4
}

function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(8)
}

function canAudit(row) {
  const status = Number(row.billStatus)
  return status === BILL_STATUS.WAIT_AUDIT || status === BILL_STATUS.DIFF_HANG
}

function canSettle(row) {
  return Number(row.billStatus) === BILL_STATUS.CONFIRMED
}

function buildQuery() {
  const { statusList, ...rest } = queryParams.value
  const query = { ...rest, params: {} }
  if (Array.isArray(statusList) && statusList.length) {
    query.params.statusList = statusList.join(",")
  }
  return query
}

function getList() {
  loading.value = true
  listBill(buildQuery()).then(res => {
    billList.value = res.rows || []
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
    pageSize: 10,
    billNo: undefined,
    contactNo: undefined,
    supplierId: undefined,
    customerId: undefined,
    billCycle: undefined,
    statusList: []
  }
  getList()
}

/** 生成账单 */
const generateOpen = ref(false)
const generateSubmitting = ref(false)
const generateRef = ref()
const generateForm = ref({
  supplierId: undefined,
  billCycle: undefined,
  remark: ""
})

const generateRules = {
  supplierId: [{ required: true, message: "请选择渠道", trigger: "change" }],
  billCycle: [{ required: true, message: "请选择账期", trigger: "change" }]
}

function openGenerate() {
  const now = new Date()
  now.setMonth(now.getMonth() - 1)
  generateForm.value = {
    supplierId: undefined,
    billCycle: parseTime(now, "{y}-{m}"),
    remark: ""
  }
  generateOpen.value = true
  nextTick(() => generateRef.value && generateRef.value.clearValidate())
}

function submitGenerate() {
  generateRef.value.validate(valid => {
    if (!valid) {
      return
    }
    generateSubmitting.value = true
    generateBill({ ...generateForm.value }).then(res => {
      const r = res.data || {}
      if (Number(r.billCount) === 0) {
        proxy.$modal.msgWarning(
          `账期 ${r.billCycle || ""} 无可生成账单：该渠道没有待入账的成功用量，请先完成对账`
        )
      } else {
        proxy.$modal.msgSuccess(
          `生成 ${r.billCount} 张账单，归集用量 ${r.usageCount} 条，应收合计 ${formatAmount(r.totalSellAmount)}`
        )
      }
      generateOpen.value = false
      getList()
    }).finally(() => {
      generateSubmitting.value = false
    })
  })
}

/** 复核 */
const auditOpen = ref(false)
const auditSubmitting = ref(false)
const auditRef = ref()
const auditForm = ref({
  billId: undefined,
  billNo: "",
  billStatus: 2,
  remark: "",
  pendingDiffCount: 0
})

const auditRules = {
  billStatus: [{ required: true, message: "请选择复核结果", trigger: "change" }],
  remark: [
    {
      validator: (rule, value, callback) => {
        if (Number(auditForm.value.billStatus) === 4 && !String(value || "").trim()) {
          callback(new Error("差异挂起时必须填写说明"))
          return
        }
        callback()
      },
      trigger: "blur"
    }
  ]
}

function openAudit(row) {
  auditForm.value = {
    billId: row.id,
    billNo: row.billNo,
    billStatus: 2,
    remark: "",
    pendingDiffCount: row.pendingDiffCount || 0
  }
  auditOpen.value = true
  nextTick(() => auditRef.value && auditRef.value.clearValidate())
}

function submitAudit() {
  auditRef.value.validate(valid => {
    if (!valid) {
      return
    }
    auditSubmitting.value = true
    auditBill({
      billId: auditForm.value.billId,
      billStatus: auditForm.value.billStatus,
      remark: String(auditForm.value.remark || "").trim()
    }).then(() => {
      proxy.$modal.msgSuccess(Number(auditForm.value.billStatus) === 2 ? "账单已确认" : "账单已标记差异挂起")
      auditOpen.value = false
      getList()
    }).finally(() => {
      auditSubmitting.value = false
    })
  })
}

/** 结算 */
function handleSettle(row) {
  proxy.$modal.confirm(
    `确认结算账单【${row.billNo}】？结算后账单状态置为已结算，并回写关联商机的结算状态。`
  ).then(() => {
    return settleBill(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("结算完成")
    getList()
  }).catch(() => {})
}

/** 详情 */
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailTab = ref("usages")
const detail = ref({ bill: null, usages: [], diffs: [] })

function openDetail(row) {
  detail.value = { bill: null, usages: [], diffs: [] }
  detailLoad(row.id)
  detailOpen.value = true
}

function detailLoad(id) {
  detailLoading.value = true
  getBill(id).then(res => {
    const data = res.data || {}
    detail.value = {
      bill: data.bill,
      usages: data.usages || [],
      diffs: data.diffs || []
    }
    detailTab.value = "usages"
  }).finally(() => {
    detailLoading.value = false
  })
}

function handleExport() {
  proxy.download("crm/bill/export", buildQuery(), `对账账单_${new Date().getTime()}.xlsx`)
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

loadSupplierOptions()
loadCustomerOptions()
getList()
</script>

<style scoped>
.plain-text {
  color: var(--el-text-color-regular);
}

.detail-tabs {
  margin-top: 16px;
}
</style>

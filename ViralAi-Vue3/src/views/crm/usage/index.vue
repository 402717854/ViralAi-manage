<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 对账台账 -->
      <el-tab-pane label="对账台账" name="usage">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="流水号" prop="callNo">
            <el-input
              v-model="queryParams.callNo"
              placeholder="请输入流水号"
              clearable
              style="width: 220px"
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
              style="width: 180px"
            >
              <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
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
          <el-form-item label="模型编码" prop="productCode">
            <el-input
              v-model="queryParams.productCode"
              placeholder="请输入模型编码"
              clearable
              style="width: 180px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="匹配状态" prop="matchStatusList">
            <el-select
              v-model="queryParams.matchStatusList"
              placeholder="请选择"
              multiple
              collapse-tags
              clearable
              style="width: 220px"
            >
              <el-option
                v-for="dict in crm_usage_match_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="调用时间">
            <el-date-picker
              v-model="dateRange"
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
            <el-button
              type="primary"
              plain
              icon="Refresh"
              @click="openReconcile"
              v-hasPermi="['crm:usage:reconcile']"
            >手动对账</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="Download"
              @click="handleExport"
              v-hasPermi="['crm:usage:export']"
            >导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="usageList">
          <el-table-column label="流水号" align="center" prop="callNo" min-width="230" fixed="left" show-overflow-tooltip>
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="handleViewUsage(scope.row)">
                {{ scope.row.callNo }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
          <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
          <el-table-column label="渠道账户" align="center" min-width="130" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.supplierAccountName || (scope.row.supplierAccountId ? scope.row.supplierAccountId : "未归属") }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模型编码" align="center" prop="productCode" min-width="150" show-overflow-tooltip />
          <el-table-column label="token" align="center" prop="totalTokens" width="110" />
          <el-table-column label="消耗费用" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalFees) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="应收金额" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.sellAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本金额" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.costAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="毛利" align="center" width="140" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.grossProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="匹配状态" align="center" width="130">
            <template #default="scope">
              <dict-tag :options="crm_usage_match_status" :value="String(scope.row.matchStatus)" />
            </template>
          </el-table-column>
          <el-table-column label="合同编号" align="center" prop="contactNo" min-width="180" show-overflow-tooltip />
          <el-table-column label="账单编号" align="center" prop="billNo" min-width="160" show-overflow-tooltip />
          <el-table-column label="用量类型" align="center" width="100">
            <template #default="scope">
              <dict-tag :options="crm_usage_type" :value="String(scope.row.usageType ?? 0)" />
            </template>
          </el-table-column>
          <el-table-column label="调用时间" align="center" prop="callTime" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.callTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="150" fixed="right">
            <template #default="scope">
              <el-button
                v-if="canRematch(scope.row)"
                link
                type="primary"
                icon="Refresh"
                @click="handleRematch(scope.row)"
                v-hasPermi="['crm:usage:reconcile']"
              >重新匹配</el-button>
              <el-button link type="info" icon="View" @click="handleViewUsage(scope.row)">详情</el-button>
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
      </el-tab-pane>

      <!-- 网关原始流水 -->
      <el-tab-pane label="网关原始流水" name="callLog">
        <el-form :model="logQuery" ref="logQueryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="流水号" prop="callNo">
            <el-input
              v-model="logQuery.callNo"
              placeholder="请输入流水号"
              clearable
              style="width: 220px"
              @keyup.enter="handleLogQuery"
            />
          </el-form-item>
          <el-form-item label="客户" prop="customerId">
            <el-select
              v-model="logQuery.customerId"
              placeholder="请选择客户"
              filterable
              clearable
              style="width: 180px"
            >
              <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="渠道" prop="supplierId">
            <el-select
              v-model="logQuery.supplierId"
              placeholder="请选择渠道"
              filterable
              clearable
              style="width: 180px"
            >
              <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型编码" prop="productCode">
            <el-input
              v-model="logQuery.productCode"
              placeholder="请输入模型编码"
              clearable
              style="width: 180px"
              @keyup.enter="handleLogQuery"
            />
          </el-form-item>
          <el-form-item label="调用状态" prop="callStatus">
            <el-select v-model="logQuery.callStatus" placeholder="全部" clearable style="width: 140px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="调用时间">
            <el-date-picker
              v-model="logDateRange"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleLogQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetLogQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="Download"
              @click="openPull"
              v-hasPermi="['crm:usage:reconcile']"
            >拉取流水</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="Download"
              @click="handleLogExport"
              v-hasPermi="['crm:usage:export']"
            >导出流水</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getCallLogList"></right-toolbar>
        </el-row>

        <el-table v-loading="logLoading" :data="callLogList">
          <el-table-column label="流水号" align="center" prop="callNo" min-width="230" fixed="left" show-overflow-tooltip />
          <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
          <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
          <el-table-column label="渠道账户" align="center" min-width="130" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.supplierAccountName || (scope.row.supplierAccountId ? scope.row.supplierAccountId : "未归属") }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模型编码" align="center" prop="productCode" min-width="150" show-overflow-tooltip />
          <el-table-column label="模型类型" align="center" prop="modelType" width="110" />
          <el-table-column label="输入/输出" align="center" width="130">
            <template #default="scope">
              <span>{{ scope.row.promptTokens || 0 }} / {{ scope.row.completionTokens || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="总token" align="center" prop="totalTokens" width="110" />
          <el-table-column label="消耗费用" align="center" width="150" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ formatAmount(scope.row.totalFees) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="调用状态" align="center" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.callStatus === 1 ? 'success' : 'danger'">
                {{ scope.row.callStatus === 1 ? "成功" : "失败" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="是否已对账" align="center" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.reconciled === 1 ? 'success' : 'info'">
                {{ scope.row.reconciled === 1 ? "已对账" : "未对账" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="调用时间" align="center" prop="callTime" width="165">
            <template #default="scope">
              <span>{{ parseTime(scope.row.callTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" align="center" prop="errMsg" min-width="160" show-overflow-tooltip />
        </el-table>

        <pagination
          v-show="logTotal > 0"
          :total="logTotal"
          v-model:page="logQuery.pageNum"
          v-model:limit="logQuery.pageSize"
          @pagination="getCallLogList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 手动对账 -->
    <el-dialog title="手动执行对账" v-model="reconcileOpen" width="560px" append-to-body>
      <el-form :model="reconcileForm" :rules="reconcileRules" ref="reconcileRef" label-width="110px">
        <el-form-item label="调用时间" prop="timeRange">
          <el-date-picker
            v-model="reconcileForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="渠道" prop="supplierId">
          <el-select v-model="reconcileForm.supplierId" placeholder="不选表示当前账号可见的全部渠道" filterable clearable style="width: 100%">
            <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="重试异常记录">
          <el-switch v-model="reconcileForm.retryAbnormal" />
          <span class="form-tip">开启后对「无合同 / 已过期 / 其他异常」的台账重新匹配，补签合同后使用</span>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="对账会扫描区间内未生成台账的网关流水，按客户+渠道+模型匹配生效合同并计费；已生成账单的用量不会被重算。"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitReconcile" :loading="reconcileSubmitting">开始对账</el-button>
          <el-button @click="reconcileOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 拉取流水 -->
    <el-dialog title="从渠道账户拉取流水" v-model="pullOpen" width="600px" append-to-body>
      <el-form :model="pullForm" :rules="pullRules" ref="pullRef" label-width="110px">
        <el-form-item label="调用时间" prop="timeRange">
          <el-date-picker
            v-model="pullForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="渠道" prop="supplierId">
          <el-select
            v-model="pullForm.supplierId"
            placeholder="不选表示全部渠道"
            filterable
            clearable
            style="width: 100%"
            @change="handlePullSupplierChange"
          >
            <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道账户" prop="supplierAccountId">
          <el-select
            v-model="pullForm.supplierAccountId"
            placeholder="不选时拉取所选渠道下全部启用账户"
            filterable
            clearable
            :loading="pullAccountLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in pullAccountOptions"
              :key="item.id"
              :label="`${item.accountName}${item.pullChannel ? '（' + item.pullChannel + '）' : ''}`"
              :value="item.id"
            />
          </el-select>
          <div class="form-tip">
            一个账户对应一套密钥与一种上游协议。不选账户时会逐个拉取该渠道下所有启用账户，
            未配置拉取协议的账户会按通用地址尝试。
          </div>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="按流水号幂等：已存在的流水不会重复入库。客户/模型映射缺失的流水不会入库，结果里会给出未映射数量与样例。"
        />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitPull" :loading="pullSubmitting">开始拉取</el-button>
          <el-button @click="pullOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 台账详情 -->
    <el-dialog title="用量明细" v-model="usageOpen" width="820px" append-to-body>
      <el-descriptions :column="2" border v-if="usageDetail">
        <el-descriptions-item label="流水号" :span="2">{{ usageDetail.callNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ usageDetail.customerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ usageDetail.supplierName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="渠道账户">{{ usageDetail.supplierAccountName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="模型编码">{{ usageDetail.productCode }}</el-descriptions-item>
        <el-descriptions-item label="模型型号">{{ usageDetail.modelNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="匹配状态">
          <dict-tag :options="crm_usage_match_status" :value="String(usageDetail.matchStatus)" />
        </el-descriptions-item>
        <el-descriptions-item label="用量类型">
          <dict-tag :options="crm_usage_type" :value="String(usageDetail.usageType ?? 0)" />
        </el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ usageDetail.contactNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="账单编号">{{ usageDetail.billNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="token">{{ usageDetail.totalTokens }}</el-descriptions-item>
        <el-descriptions-item label="消耗费用">{{ formatAmount(usageDetail.totalFees) }}</el-descriptions-item>
        <el-descriptions-item label="调用时间">{{ parseTime(usageDetail.callTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
        <el-descriptions-item label="基准售价(千token)">{{ formatAmount(usageDetail.baseSellPrice) }}</el-descriptions-item>
        <el-descriptions-item label="售价折扣">{{ usageDetail.sellDiscount ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="基准成本价(千token)">{{ formatAmount(usageDetail.baseCostPrice) }}</el-descriptions-item>
        <el-descriptions-item label="成本折扣">{{ usageDetail.costDiscount ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="应收金额">{{ formatAmount(usageDetail.sellAmount) }}</el-descriptions-item>
        <el-descriptions-item label="成本金额">{{ formatAmount(usageDetail.costAmount) }}</el-descriptions-item>
        <el-descriptions-item label="毛利">{{ formatAmount(usageDetail.grossProfit) }}</el-descriptions-item>
        <el-descriptions-item label="匹配说明" :span="2">{{ usageDetail.matchMsg || "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="Usage">
import {
  listUsage,
  getUsage,
  reconcileUsage,
  rematchUsage,
  listCallLog,
  pullCallLog
} from "@/api/crm/usage"
import { getSupplierOptions } from "@/api/crm/supplier"
import { listSupplierAccount } from "@/api/crm/supplierAccount"
import { getCustomerOptions } from "@/api/crm/customer"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { crm_usage_match_status, crm_usage_type } = proxy.useDict("crm_usage_match_status", "crm_usage_type")

const activeTab = ref("usage")
const showSearch = ref(true)
const loading = ref(false)
const logLoading = ref(false)
const usageList = ref([])
const callLogList = ref([])
const total = ref(0)
const logTotal = ref(0)
const dateRange = ref([])
const logDateRange = ref([])
const supplierOptions = ref([])
const supplierLoading = ref(false)
const customerOptions = ref([])
const customerLoading = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  callNo: undefined,
  customerId: undefined,
  supplierId: undefined,
  productCode: undefined,
  matchStatusList: []
})

const logQuery = ref({
  pageNum: 1,
  pageSize: 10,
  callNo: undefined,
  customerId: undefined,
  supplierId: undefined,
  productCode: undefined,
  callStatus: undefined
})

/** 匹配状态常量，与后端 UsageMatchStatusEnum 一致 */
const MATCH_SUCCESS = 1

/** 财务对账金额统一展示 8 位小数，与 decimal(20,8) 存储精度一致 */
function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(8)
}

function canRematch(row) {
  if (row.billId) {
    return false
  }
  if (Number(row.usageType || 0) === 1) {
    return false
  }
  return Number(row.matchStatus) !== MATCH_SUCCESS
}

function buildQuery() {
  const { matchStatusList, ...rest } = queryParams.value
  const query = { ...rest, params: {} }
  if (Array.isArray(matchStatusList) && matchStatusList.length) {
    query.params.matchStatusList = matchStatusList.join(",")
  }
  return proxy.addDateRange(query, dateRange.value)
}

function getList() {
  loading.value = true
  listUsage(buildQuery()).then(res => {
    usageList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function getCallLogList() {
  logLoading.value = true
  const query = proxy.addDateRange({ ...logQuery.value, params: {} }, logDateRange.value)
  listCallLog(query).then(res => {
    callLogList.value = res.rows || []
    logTotal.value = res.total || 0
  }).finally(() => {
    logLoading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    callNo: undefined,
    customerId: undefined,
    supplierId: undefined,
    productCode: undefined,
    matchStatusList: []
  }
  getList()
}

function handleLogQuery() {
  logQuery.value.pageNum = 1
  getCallLogList()
}

function resetLogQuery() {
  logDateRange.value = []
  logQuery.value = {
    pageNum: 1,
    pageSize: 10,
    callNo: undefined,
    customerId: undefined,
    supplierId: undefined,
    productCode: undefined,
    callStatus: undefined
  }
  getCallLogList()
}

/** 切换页签时懒加载对应数据 */
function handleTabChange(name) {
  if (name === "callLog" && callLogList.value.length === 0) {
    getCallLogList()
  }
}

/** 手动对账 */
const reconcileOpen = ref(false)
const reconcileSubmitting = ref(false)
const reconcileRef = ref()
const reconcileForm = ref({
  timeRange: [],
  supplierId: undefined,
  retryAbnormal: true
})

const reconcileRules = {
  timeRange: [{ required: true, message: "请选择调用时间区间", trigger: "change" }]
}

function openReconcile() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 1)
  reconcileForm.value = {
    timeRange: [parseTime(start, "{y}-{m}-{d} 00:00:00"), parseTime(end, "{y}-{m}-{d} 23:59:59")],
    supplierId: undefined,
    retryAbnormal: true
  }
  reconcileOpen.value = true
  nextTick(() => reconcileRef.value && reconcileRef.value.clearValidate())
}

function submitReconcile() {
  reconcileRef.value.validate(valid => {
    if (!valid) {
      return
    }
    reconcileSubmitting.value = true
    reconcileUsage({
      startTime: reconcileForm.value.timeRange[0],
      endTime: reconcileForm.value.timeRange[1],
      supplierId: reconcileForm.value.supplierId,
      retryAbnormal: reconcileForm.value.retryAbnormal
    }).then(res => {
      const r = res.data || {}
      proxy.$modal.msgSuccess(
        `对账完成：扫描 ${r.scanned || 0} 条，新增台账 ${r.created || 0} 条，成功 ${r.matched || 0} 条，异常 ${r.abnormal || 0} 条，新增差异 ${r.diffCreated || 0} 条`
      )
      reconcileOpen.value = false
      getList()
    }).finally(() => {
      reconcileSubmitting.value = false
    })
  })
}

/** 拉取流水：按渠道 + 渠道账户从上游拉取 */
const pullOpen = ref(false)
const pullSubmitting = ref(false)
const pullRef = ref()
const pullAccountLoading = ref(false)
const pullAccountOptions = ref([])
const pullForm = ref({
  timeRange: [],
  supplierId: undefined,
  supplierAccountId: undefined
})

const pullRules = {
  timeRange: [{ required: true, message: "请选择调用时间区间", trigger: "change" }]
}

function openPull() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 1)
  pullForm.value = {
    timeRange: [parseTime(start, "{y}-{m}-{d} 00:00:00"), parseTime(end, "{y}-{m}-{d} 23:59:59")],
    supplierId: undefined,
    supplierAccountId: undefined
  }
  pullAccountOptions.value = []
  loadPullAccounts()
  pullOpen.value = true
  nextTick(() => pullRef.value && pullRef.value.clearValidate())
}

/** 账户选项：只取启用账户，选渠道后收窄范围 */
function loadPullAccounts() {
  pullAccountLoading.value = true
  listSupplierAccount({ pageNum: 1, pageSize: 500, status: 1, supplierId: pullForm.value.supplierId })
    .then(res => {
      pullAccountOptions.value = res.rows || []
    })
    .finally(() => {
      pullAccountLoading.value = false
    })
}

function handlePullSupplierChange() {
  // 渠道变了，已选账户可能不属于新渠道，必须清掉
  pullForm.value.supplierAccountId = undefined
  loadPullAccounts()
}

function submitPull() {
  pullRef.value.validate(valid => {
    if (!valid) {
      return
    }
    pullSubmitting.value = true
    pullCallLog({
      startTime: pullForm.value.timeRange[0],
      endTime: pullForm.value.timeRange[1],
      supplierId: pullForm.value.supplierId,
      supplierAccountId: pullForm.value.supplierAccountId
    }).then(res => {
      const r = res.data || {}
      let msg = `拉取完成：共 ${r.pulled || 0} 条，入库 ${r.inserted || 0} 条，重复 ${r.duplicated || 0} 条`
      if (r.unmapped) {
        msg += `；未映射客户 ${r.unmapped} 条`
      }
      if (r.missingModel) {
        msg += `；缺模型编码 ${r.missingModel} 条`
      }
      proxy.$modal.msgSuccess(msg)
      // 有未映射时给出样例，便于直接去「流水映射配置」补规则
      if (r.unmapped > 0 && r.unmappedSample && r.unmappedSample.length) {
        proxy.$alert(
          `以下上游标识未配置客户映射，对应流水未入库：${r.unmappedSample.join("、")}`,
          "存在未映射的上游客户",
          { type: "warning" }
        )
      }
      pullOpen.value = false
      getCallLogList()
    }).catch(e => {
      // 单账户失败时后端会把原因放进 accounts[].error，这里直接展示首个错误
      const accounts = (e && e.data && e.data.accounts) || []
      const first = accounts.find(item => item.error)
      proxy.$modal.msgError((e && e.message) || (first && first.error) || "拉取失败")
    }).finally(() => {
      pullSubmitting.value = false
    })
  })
}

/** 单条重新匹配 */
function handleRematch(row) {  proxy.$modal.confirm(`确认重新匹配流水【${row.callNo}】？`).then(() => {
    return rematchUsage(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("匹配成功，已按当前生效合同计费")
    getList()
  }).catch(() => {})
}

/** 台账详情 */
const usageOpen = ref(false)
const usageDetail = ref(null)

function handleViewUsage(row) {
  getUsage(row.id).then(res => {
    usageDetail.value = res.data
    usageOpen.value = true
  })
}

function handleExport() {
  proxy.download("crm/usage/export", buildQuery(), `对账台账_${new Date().getTime()}.xlsx`)
}

function handleLogExport() {
  const query = proxy.addDateRange({ ...logQuery.value, params: {} }, logDateRange.value)
  proxy.download("crm/gateway-call-log/export", query, `网关调用流水_${new Date().getTime()}.xlsx`)
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
.form-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>

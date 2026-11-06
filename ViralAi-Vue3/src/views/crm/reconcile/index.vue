<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="4">
        <el-date-picker
          v-model="billCycle"
          type="month"
          value-format="YYYY-MM"
          placeholder="账期"
          :clearable="false"
          style="width: 100%"
          @change="loadBoard"
        />
      </el-col>
      <el-col :span="2">
        <el-button type="primary" icon="Search" @click="loadBoard">查询</el-button>
      </el-col>
      <el-col :span="12">
        <span class="board-tip">统计口径：按网关调用时间归属账期，金额只统计匹配成功的用量</span>
      </el-col>
    </el-row>

    <el-row :gutter="12" v-loading="loading">
      <el-col :xs="12" :sm="8" :md="4" v-for="card in cards" :key="card.label">
        <div class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :class="card.cls">{{ card.value }}</div>
          <div class="stat-sub" v-if="card.sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="content-row">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>渠道对账汇总</span>
              <el-tag type="info" v-if="board.billCycle">账期 {{ board.billCycle }}</el-tag>
            </div>
          </template>
          <el-table :data="board.supplierStats || []" size="small" max-height="420">
            <el-table-column label="渠道" prop="supplierName" min-width="140" show-overflow-tooltip />
            <el-table-column label="调用数" prop="totalCalls" width="90" />
            <el-table-column label="token" prop="totalTokens" width="110" />
            <el-table-column label="应收" width="145" show-overflow-tooltip>
              <template #default="scope">{{ formatAmount(scope.row.totalSellAmount) }}</template>
            </el-table-column>
            <el-table-column label="成本" width="145" show-overflow-tooltip>
              <template #default="scope">{{ formatAmount(scope.row.totalCostAmount) }}</template>
            </el-table-column>
            <el-table-column label="毛利" width="145" show-overflow-tooltip>
              <template #default="scope">{{ formatAmount(scope.row.totalGrossProfit) }}</template>
            </el-table-column>
            <el-table-column label="毛利率" width="90">
              <template #default="scope">{{ formatRate(scope.row.profitRate) }}</template>
            </el-table-column>
            <el-table-column label="异常占比" width="100">
              <template #default="scope">
                <span :class="{ 'rate-warning': Number(scope.row.abnormalRate) > 0 }">
                  {{ formatRate(scope.row.abnormalRate) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="合同数" prop="contractCount" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>合同消耗排行 TOP10</span>
            </div>
          </template>
          <el-table :data="board.contractRank || []" size="small" max-height="420">
            <el-table-column label="合同编号" prop="contactNo" min-width="170" show-overflow-tooltip />
            <el-table-column label="客户" prop="customerName" min-width="120" show-overflow-tooltip />
            <el-table-column label="token" prop="totalTokens" width="110" />
            <el-table-column label="毛利" width="110">
              <template #default="scope">{{ formatAmount(scope.row.totalGrossProfit) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="ReconcileBoard">
import { getUsageBoard } from "@/api/crm/usage"
import { parseTime } from "@/utils/ruoyi"

const loading = ref(false)
const board = ref({ supplierStats: [], contractRank: [] })
const billCycle = ref(parseTime(new Date(), "{y}-{m}"))

const cards = computed(() => {
  const b = board.value || {}
  return [
    { label: "调用次数", value: formatNumber(b.totalCalls) },
    { label: "总 token", value: formatNumber(b.totalTokens) },
    { label: "应收总额", value: formatAmount(b.totalSellAmount), cls: "text-primary" },
    { label: "毛利总额", value: formatAmount(b.totalGrossProfit), sub: `毛利率 ${formatRate(b.profitRate)}`, cls: "text-success" },
    { label: "异常占比", value: formatRate(b.abnormalRate), sub: `异常 ${formatNumber(b.abnormalCount)} 条`, cls: "text-danger" },
    { label: "待处理差异", value: formatNumber(b.pendingDiffCount), sub: `账单 ${formatNumber(b.billCount)} 张` }
  ]
})

function formatNumber(value) {
  if (value === null || value === undefined) {
    return "0"
  }
  return Number(value).toLocaleString()
}

function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "0.00000000"
  }
  return Number(value).toFixed(8)
}

function formatRate(value) {
  if (value === null || value === undefined || value === "") {
    return "0.00%"
  }
  return Number(value).toFixed(2) + "%"
}

function loadBoard() {
  loading.value = true
  getUsageBoard(billCycle.value).then(res => {
    board.value = res.data || { supplierStats: [], contractRank: [] }
  }).finally(() => {
    loading.value = false
  })
}

loadBoard()
</script>

<style scoped>
.board-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 32px;
}

.stat-card {
  padding: 14px 16px;
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.stat-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.text-primary {
  color: var(--el-color-primary);
}

.text-success {
  color: var(--el-color-success);
}

.text-danger {
  color: var(--el-color-danger);
}

.content-row {
  margin-top: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rate-warning {
  color: var(--el-color-danger);
}
</style>

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="差异类型" prop="diffType">
        <el-select v-model="queryParams.diffType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in crm_bill_diff_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="handleStatus">
        <el-select v-model="queryParams.handleStatus" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="dict in crm_bill_diff_handle" :key="dict.value" :label="dict.label" :value="dict.value" />
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
      <el-form-item label="流水号" prop="callNo">
        <el-input
          v-model="queryParams.callNo"
          placeholder="请输入流水号"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
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
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['crm:bill:diff']"
        >导出</el-button>
      </el-col>
      <el-col :span="6">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="调账会生成一条冲销用量并计入下一账期账单；豁免表示不计费；驳回退回复核。"
        />
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="diffList">
      <el-table-column label="差异类型" align="center" width="110">
        <template #default="scope">
          <dict-tag :options="crm_bill_diff_type" :value="String(scope.row.diffType)" />
        </template>
      </el-table-column>
      <el-table-column label="差异描述" align="center" prop="diffDesc" min-width="280" show-overflow-tooltip />
      <el-table-column label="流水号" align="center" prop="callNo" min-width="220" show-overflow-tooltip />
      <el-table-column label="客户" align="center" prop="customerName" min-width="130" show-overflow-tooltip />
      <el-table-column label="渠道" align="center" prop="supplierName" min-width="120" show-overflow-tooltip />
      <el-table-column label="涉及金额" align="center" width="150" show-overflow-tooltip>
        <template #default="scope">{{ formatAmount(scope.row.amount) }}</template>
      </el-table-column>
      <el-table-column label="调用时间" align="center" width="165">
        <template #default="scope">{{ parseTime(scope.row.callTime, '{y}-{m}-{d} {h}:{i}') }}</template>
      </el-table-column>
      <el-table-column label="处理状态" align="center" width="110">
        <template #default="scope">
          <dict-tag :options="crm_bill_diff_handle" :value="String(scope.row.handleStatus)" />
        </template>
      </el-table-column>
      <el-table-column label="处理人" align="center" prop="handlerName" width="110" />
      <el-table-column label="处理时间" align="center" width="165">
        <template #default="scope">{{ parseTime(scope.row.handleTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</template>
      </el-table-column>
      <el-table-column label="处理意见" align="center" prop="handleRemark" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="110" fixed="right">
        <template #default="scope">
          <el-button
            v-if="Number(scope.row.handleStatus) === 0"
            link
            type="primary"
            icon="Edit"
            @click="openHandle(scope.row)"
            v-hasPermi="['crm:bill:diff-handle']"
          >处理</el-button>
          <span v-else>-</span>
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

    <!-- 差异处理 -->
    <el-dialog title="差异处理" v-model="handleOpen" width="560px" append-to-body>
      <el-form :model="handleForm" :rules="handleRules" ref="handleRef" label-width="110px">
        <el-form-item label="差异类型">
          <dict-tag :options="crm_bill_diff_type" :value="String(handleForm.diffType)" />
        </el-form-item>
        <el-form-item label="差异描述" :span="2">
          <span class="diff-desc">{{ handleForm.diffDesc }}</span>
        </el-form-item>
        <el-form-item label="处理方式" prop="handleStatus">
          <el-radio-group v-model="handleForm.handleStatus">
            <el-radio :value="1">调账</el-radio>
            <el-radio :value="2">豁免</el-radio>
            <el-radio :value="3">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="Number(handleForm.handleStatus) === 1">
          <el-form-item label="应收调整额">
            <el-input-number v-model="handleForm.adjustSellAmount" :precision="4" :step="1" style="width: 200px" />
            <span class="form-tip">负数冲减，正数补收；留空按原用量全额冲销</span>
          </el-form-item>
          <el-form-item label="成本调整额">
            <el-input-number v-model="handleForm.adjustCostAmount" :precision="4" :step="1" style="width: 200px" />
            <span class="form-tip">负数冲减，正数补收；留空按原用量全额冲销</span>
          </el-form-item>
        </template>
        <el-form-item label="处理意见" prop="handleRemark">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="3"
            maxlength="480"
            show-word-limit
            placeholder="请填写处理说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitHandle" :loading="handleSubmitting">确 定</el-button>
          <el-button @click="handleOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BillDiff">
import { listBillDiff, handleBillDiff } from "@/api/crm/bill"
import { getSupplierOptions } from "@/api/crm/supplier"
import { getCustomerOptions } from "@/api/crm/customer"
import { parseTime } from "@/utils/ruoyi"

const { proxy } = getCurrentInstance()
const { crm_bill_diff_type, crm_bill_diff_handle } = proxy.useDict("crm_bill_diff_type", "crm_bill_diff_handle")

const diffList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const supplierOptions = ref([])
const supplierLoading = ref(false)
const customerOptions = ref([])
const customerLoading = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  diffType: undefined,
  handleStatus: undefined,
  supplierId: undefined,
  customerId: undefined,
  callNo: undefined
})

function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(8)
}

function buildQuery() {
  return proxy.addDateRange({ ...queryParams.value, params: {} }, dateRange.value)
}

function getList() {
  loading.value = true
  listBillDiff(buildQuery()).then(res => {
    diffList.value = res.rows || []
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
  dateRange.value = []
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    diffType: undefined,
    handleStatus: undefined,
    supplierId: undefined,
    customerId: undefined,
    callNo: undefined
  }
  getList()
}

/** 处理差异 */
const handleOpen = ref(false)
const handleSubmitting = ref(false)
const handleRef = ref()
const handleForm = ref({
  diffId: undefined,
  diffType: undefined,
  diffDesc: "",
  handleStatus: 1,
  adjustSellAmount: undefined,
  adjustCostAmount: undefined,
  handleRemark: ""
})

const handleRules = {
  handleStatus: [{ required: true, message: "请选择处理方式", trigger: "change" }],
  handleRemark: [
    {
      validator: (rule, value, callback) => {
        const status = Number(handleForm.value.handleStatus)
        if (status !== 1 && !String(value || "").trim()) {
          callback(new Error("豁免或驳回必须填写处理意见"))
          return
        }
        callback()
      },
      trigger: "blur"
    }
  ]
}

function openHandle(row) {
  handleForm.value = {
    diffId: row.id,
    diffType: row.diffType,
    diffDesc: row.diffDesc,
    handleStatus: 1,
    adjustSellAmount: undefined,
    adjustCostAmount: undefined,
    handleRemark: ""
  }
  handleOpen.value = true
  nextTick(() => handleRef.value && handleRef.value.clearValidate())
}

function submitHandle() {
  handleRef.value.validate(valid => {
    if (!valid) {
      return
    }
    handleSubmitting.value = true
    const payload = {
      diffId: handleForm.value.diffId,
      handleStatus: handleForm.value.handleStatus,
      handleRemark: String(handleForm.value.handleRemark || "").trim()
    }
    if (Number(handleForm.value.handleStatus) === 1) {
      payload.adjustSellAmount = handleForm.value.adjustSellAmount
      payload.adjustCostAmount = handleForm.value.adjustCostAmount
    }
    handleBillDiff(payload).then(() => {
      proxy.$modal.msgSuccess(Number(payload.handleStatus) === 1 ? "调账完成，冲销用量已计入下一账期" : "处理完成")
      handleOpen.value = false
      getList()
    }).finally(() => {
      handleSubmitting.value = false
    })
  })
}

function handleExport() {
  proxy.download("crm/bill/diff/export", buildQuery(), `对账差异_${new Date().getTime()}.xlsx`)
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

.diff-desc {
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>

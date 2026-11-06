<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="76px">
      <el-form-item label="合同编号" prop="contactNo">
        <el-input
          v-model="queryParams.contactNo"
          placeholder="请输入合同编号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同名称" prop="contractName">
        <el-input
          v-model="queryParams.contractName"
          placeholder="请输入合同名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
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
      <el-form-item label="客户" prop="consumerId">
        <el-select
          v-model="queryParams.consumerId"
          placeholder="请选择客户"
          filterable
          clearable
          :loading="customerLoading"
          style="width: 200px"
        >
          <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="statusList">
        <el-select
          v-model="queryParams.statusList"
          placeholder="请选择状态"
          multiple
          collapse-tags
          clearable
          style="width: 220px"
        >
          <el-option
            v-for="dict in crm_contract_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
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
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['crm:contract:add']"
        >新增合同</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="contractList">
      <el-table-column label="合同编号" align="center" prop="contactNo" min-width="200" fixed="left">
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="handleView(scope.row)">{{ scope.row.contactNo }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="合同名称" align="center" prop="contractName" min-width="180" show-overflow-tooltip />
      <el-table-column label="渠道（明细）" align="center" prop="supplierName" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.supplierName">{{ scope.row.supplierName }}</span>
          <span v-else class="text-placeholder">-</span>
        </template>
      </el-table-column>
      <el-table-column label="客户" align="center" prop="consumerName" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="crm_contract_status" :value="String(scope.row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="签约时间" align="center" prop="signTime" width="165">
        <template #default="scope">
          <span>{{ parseTime(scope.row.signTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="到期时间" align="center" prop="expireTime" width="165">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expireTime, '{y}-{m}-{d} {h}:{i}') || "-" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="creatorName" width="110" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="165">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button
            v-if="canEdit(scope.row)"
            link
            type="primary"
            icon="Edit"
            @click="handleEdit(scope.row)"
            v-hasPermi="['crm:contract:edit']"
          >编辑</el-button>
          <el-button
            v-if="canSubmit(scope.row)"
            link
            type="success"
            icon="Promotion"
            @click="handleSubmit(scope.row)"
            v-hasPermi="['crm:contract:submit']"
          >提交审批</el-button>
          <el-button
            v-if="canAudit(scope.row)"
            link
            type="warning"
            icon="Stamp"
            @click="handleAudit(scope.row)"
            v-hasPermi="['crm:contract:audit']"
          >审批</el-button>
          <el-button
            v-if="canAudit(scope.row)"
            link
            type="info"
            icon="RefreshLeft"
            @click="handleWithdraw(scope.row)"
            v-hasPermi="['crm:contract:submit']"
          >撤回</el-button>
          <el-button
            v-if="canCancel(scope.row)"
            link
            type="danger"
            icon="CircleClose"
            @click="handleCancel(scope.row)"
            v-hasPermi="['crm:contract:cancel']"
          >作废</el-button>
          <el-button
            v-if="canArchive(scope.row)"
            link
            type="info"
            icon="FolderChecked"
            @click="handleArchive(scope.row)"
            v-hasPermi="['crm:contract:archive']"
          >归档</el-button>
          <el-button
            v-if="canDelete(scope.row)"
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['crm:contract:remove']"
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

    <!-- 审批弹窗 -->
    <el-dialog title="合同审批" v-model="auditOpen" width="520px" append-to-body>
      <el-form :model="auditForm" :rules="auditRules" ref="auditRef" label-width="90px">
        <el-form-item label="审批结果" prop="auditResult">
          <el-radio-group v-model="auditForm.auditResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="auditOpinion">
          <el-input
            v-model="auditForm.auditOpinion"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="通过时可不填；驳回时必须填写驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAuditForm" :loading="auditSubmitting">确 定</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 作废弹窗 -->
    <el-dialog title="合同作废" v-model="cancelOpen" width="520px" append-to-body>
      <el-form :model="cancelForm" :rules="cancelRules" ref="cancelRef" label-width="90px">
        <el-form-item label="合同编号">
          <span class="plain-text">{{ cancelForm.contactNo }}</span>
        </el-form-item>
        <el-form-item label="作废原因" prop="cancelReason">
          <el-input
            v-model="cancelForm.cancelReason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入作废原因（必填），作废后不可恢复签约"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="submitCancelForm" :loading="cancelSubmitting">确认作废</el-button>
          <el-button @click="cancelOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Contract">
import {
  listContract,
  delContract,
  submitAudit,
  withdrawAudit,
  auditContract,
  cancelContract,
  archiveContract
} from "@/api/crm/contract"
import { getSupplierOptions } from "@/api/crm/supplier"
import { getCustomerOptions } from "@/api/crm/customer"

const { proxy } = getCurrentInstance()
const route = useRoute()
const { crm_contract_status } = proxy.useDict("crm_contract_status")

/** 当前列表路由前缀，编辑/详情页按此推导，避免菜单父级路径差异导致跳转失效 */
const contractBasePath = computed(() => route.path.replace(/\/+$/, ""))

const contractList = ref([])
const loading = ref(true)
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
  contactNo: undefined,
  contractName: undefined,
  supplierId: undefined,
  consumerId: undefined,
  statusList: []
})

/** 状态常量，与后端 ContractStatusEnum 保持一致 */
const STATUS = {
  DRAFT: 0,
  AUDITING: 1,
  SIGNED: 2,
  EXPIRED: 3,
  CANCELLED: 4,
  ARCHIVED: 5
}

function canEdit(row) {
  return Number(row.status) === STATUS.DRAFT
}

function canSubmit(row) {
  return Number(row.status) === STATUS.DRAFT
}

function canAudit(row) {
  return Number(row.status) === STATUS.AUDITING
}

function canCancel(row) {
  const status = Number(row.status)
  return status === STATUS.SIGNED || status === STATUS.EXPIRED
}

function canArchive(row) {
  const status = Number(row.status)
  return status === STATUS.SIGNED || status === STATUS.EXPIRED || status === STATUS.CANCELLED
}

function canDelete(row) {
  return Number(row.status) === STATUS.DRAFT
}

/** 组装查询条件：状态多选转为逗号分隔字符串，创建时间范围写入 params */
function buildQuery() {
  const { statusList, ...rest } = queryParams.value
  const query = { ...rest, params: {} }
  if (Array.isArray(statusList) && statusList.length) {
    query.params.statusList = statusList.join(",")
  }
  return proxy.addDateRange(query, dateRange.value)
}

/** 查询合同列表 */
function getList() {
  loading.value = true
  listContract(buildQuery()).then(res => {
    contractList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

/** 渠道下拉 */
function loadSupplierOptions() {
  supplierLoading.value = true
  getSupplierOptions().then(res => {
    supplierOptions.value = res.data || []
  }).finally(() => {
    supplierLoading.value = false
  })
}

/** 客户下拉 */
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
  dateRange.value = []
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    contactNo: undefined,
    contractName: undefined,
    supplierId: undefined,
    consumerId: undefined,
    statusList: []
  }
  getList()
}

function handleAdd() {
  proxy.$router.push({ path: contractBasePath.value + "/edit" })
}

function handleEdit(row) {
  proxy.$router.push({ path: contractBasePath.value + "/edit", query: { id: row.id } })
}

function handleView(row) {
  proxy.$router.push({ path: contractBasePath.value + "/detail", query: { id: row.id } })
}

/** 提交审批 */
function handleSubmit(row) {
  proxy.$modal.confirm(`确认提交合同【${row.contactNo}】进入审批流程？提交后不可编辑。`).then(() => {
    return submitAudit(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("提交审批成功")
    getList()
  }).catch(() => {})
}

/** 撤回审批 */
function handleWithdraw(row) {
  proxy.$modal.confirm(`确认撤回合同【${row.contactNo}】的审批申请？撤回后合同退回草稿。`).then(() => {
    return withdrawAudit(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("已撤回，合同退回草稿")
    getList()
  }).catch(() => {})
}

/** 审批弹窗 */
const auditOpen = ref(false)
const auditSubmitting = ref(false)
const auditRef = ref()
const auditForm = ref({
  contractId: undefined,
  auditResult: 1,
  auditOpinion: ""
})

const auditRules = {
  auditResult: [{ required: true, message: "请选择审批结果", trigger: "change" }],
  auditOpinion: [
    {
      validator: (rule, value, callback) => {
        if (Number(auditForm.value.auditResult) === 2 && !String(value || "").trim()) {
          callback(new Error("驳回时必须填写审批意见"))
          return
        }
        callback()
      },
      trigger: "blur"
    }
  ]
}

function handleAudit(row) {
  auditForm.value = { contractId: row.id, auditResult: 1, auditOpinion: "" }
  auditOpen.value = true
  nextTick(() => auditRef.value && auditRef.value.clearValidate())
}

function submitAuditForm() {
  auditRef.value.validate(valid => {
    if (!valid) {
      return
    }
    auditSubmitting.value = true
    auditContract({ ...auditForm.value, auditOpinion: String(auditForm.value.auditOpinion || "").trim() })
      .then(() => {
        proxy.$modal.msgSuccess(Number(auditForm.value.auditResult) === 1 ? "审批通过，合同已签约" : "已驳回，合同退回草稿")
        auditOpen.value = false
        getList()
      })
      .finally(() => {
        auditSubmitting.value = false
      })
  })
}

/** 作废弹窗 */
const cancelOpen = ref(false)
const cancelSubmitting = ref(false)
const cancelRef = ref()
const cancelForm = ref({
  contractId: undefined,
  contactNo: "",
  cancelReason: ""
})

const cancelRules = {
  cancelReason: [{ required: true, message: "作废原因不能为空", trigger: "blur" }]
}

function handleCancel(row) {
  cancelForm.value = { contractId: row.id, contactNo: row.contactNo, cancelReason: "" }
  cancelOpen.value = true
  nextTick(() => cancelRef.value && cancelRef.value.clearValidate())
}

function submitCancelForm() {
  cancelRef.value.validate(valid => {
    if (!valid) {
      return
    }
    cancelSubmitting.value = true
    cancelContract({
      contractId: cancelForm.value.contractId,
      cancelReason: String(cancelForm.value.cancelReason || "").trim()
    }).then(() => {
      proxy.$modal.msgSuccess("合同已作废")
      cancelOpen.value = false
      getList()
    }).finally(() => {
      cancelSubmitting.value = false
    })
  })
}

/** 归档：二次强确认 */
function handleArchive(row) {
  proxy.$modal.confirm(`归档后合同【${row.contactNo}】不可再编辑、作废或恢复，确认归档？`).then(() => {
    return archiveContract(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("归档成功")
    getList()
  }).catch(() => {})
}

/** 删除草稿 */
function handleDelete(row) {
  proxy.$modal.confirm(`确认删除草稿合同【${row.contactNo}】？删除后编号不再复用。`).then(() => {
    return delContract(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    getList()
  }).catch(() => {})
}

loadSupplierOptions()
loadCustomerOptions()
getList()
</script>

<style scoped>
.plain-text {
  color: var(--el-text-color-regular);
}
.text-placeholder {
  color: #c0c4cc;
}
</style>
